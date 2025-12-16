#!/bin/bash

# Helm Deployment Script for Personal Finance
# Usage: ./deploy.sh [install|upgrade|uninstall|status]

set -e

CHART_NAME="finance"
CHART_PATH="./finance-chart"
NAMESPACE="finance"
RELEASE_NAME="finance"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🎯 Finance Helm Deployment Script"
echo "   Chart: $CHART_NAME"
echo "   Namespace: $NAMESPACE"
echo "   Release: $RELEASE_NAME"
echo ""

ACTION=${1:-install}

case $ACTION in
  install)
    echo "📦 Installing Finance chart..."

    # Check if Bitnami repo is added
    if ! helm repo list | grep -q "bitnami"; then
      echo "Adding Bitnami repository..."
      helm repo add bitnami https://charts.bitnami.com/bitnami
    fi

    echo "Updating Helm repositories..."
    helm repo update

    # Update chart dependencies
    echo "Updating chart dependencies..."
    helm dependency update $CHART_PATH

    # Ask for custom values
    read -p "Use custom values file? (y/N): " use_custom
    if [[ $use_custom =~ ^[Yy]$ ]]; then
      read -p "Enter path to values file: " values_file
      VALUES_FLAG="-f $values_file"
    else
      VALUES_FLAG=""
    fi

    # Install chart
    helm install $RELEASE_NAME $CHART_PATH \
      --namespace $NAMESPACE \
      --create-namespace \
      $VALUES_FLAG

    echo ""
    echo -e "${GREEN}✅ Installation started!${NC}"
    echo ""
    echo "Waiting for pods to be ready..."
    kubectl wait --for=condition=ready pod -l app=finance -n $NAMESPACE --timeout=600s || true

    echo ""
    echo "📊 Current status:"
    kubectl get all -n $NAMESPACE
    echo ""
    echo -e "${GREEN}Installation complete!${NC}"
    echo ""
    echo "Access the application:"
    echo "  kubectl port-forward -n $NAMESPACE svc/finance-frontend 3000:80"
    echo "  Then open: http://localhost:3000"
    ;;

  upgrade)
    echo "🔄 Upgrading Finance chart..."

    # Check if release exists
    if ! helm list -n $NAMESPACE | grep -q $RELEASE_NAME; then
      echo -e "${RED}❌ Release '$RELEASE_NAME' not found. Use 'install' instead.${NC}"
      exit 1
    fi

    # Update chart dependencies
    echo "Updating chart dependencies..."
    helm dependency update $CHART_PATH

    # Ask for custom values
    read -p "Use custom values file? (y/N): " use_custom
    if [[ $use_custom =~ ^[Yy]$ ]]; then
      read -p "Enter path to values file: " values_file
      VALUES_FLAG="-f $values_file"
    else
      VALUES_FLAG="--reuse-values"
    fi

    # Upgrade chart
    helm upgrade $RELEASE_NAME $CHART_PATH \
      --namespace $NAMESPACE \
      $VALUES_FLAG

    echo ""
    echo -e "${GREEN}✅ Upgrade started!${NC}"
    echo ""
    echo "Waiting for rollout to complete..."
    kubectl rollout status deployment/finance-backend -n $NAMESPACE || true
    kubectl rollout status deployment/finance-frontend -n $NAMESPACE || true

    echo ""
    echo -e "${GREEN}Upgrade complete!${NC}"
    ;;

  uninstall)
    echo "🗑️  Uninstalling Finance chart..."

    # Check if release exists
    if ! helm list -n $NAMESPACE | grep -q $RELEASE_NAME; then
      echo -e "${YELLOW}⚠️  Release '$RELEASE_NAME' not found${NC}"
    else
      helm uninstall $RELEASE_NAME -n $NAMESPACE
      echo -e "${GREEN}✅ Release uninstalled${NC}"
    fi

    echo ""
    read -p "Delete PVCs (this will DELETE ALL DATA)? (y/N): " delete_pvc
    if [[ $delete_pvc =~ ^[Yy]$ ]]; then
      kubectl delete pvc -n $NAMESPACE --all
      echo -e "${GREEN}✅ PVCs deleted${NC}"
    else
      echo -e "${YELLOW}⊝ PVCs preserved${NC}"
    fi

    echo ""
    read -p "Delete namespace '$NAMESPACE'? (y/N): " delete_ns
    if [[ $delete_ns =~ ^[Yy]$ ]]; then
      kubectl delete namespace $NAMESPACE
      echo -e "${GREEN}✅ Namespace deleted${NC}"
    else
      echo -e "${YELLOW}⊝ Namespace preserved${NC}"
    fi
    ;;

  status)
    echo "📊 Deployment Status"
    echo ""

    # Check if Helm release exists
    if helm list -n $NAMESPACE | grep -q $RELEASE_NAME; then
      echo "=== Helm Release ==="
      helm list -n $NAMESPACE
      echo ""
      echo "=== Release Values ==="
      helm get values $RELEASE_NAME -n $NAMESPACE
      echo ""
    else
      echo -e "${YELLOW}Release '$RELEASE_NAME' not found${NC}"
    fi

    # Check if namespace exists
    if kubectl get namespace $NAMESPACE &>/dev/null; then
      echo "=== All Resources ==="
      kubectl get all -n $NAMESPACE
      echo ""
      echo "=== PVCs ==="
      kubectl get pvc -n $NAMESPACE
      echo ""
      echo "=== Ingress ==="
      kubectl get ingress -n $NAMESPACE
      echo ""
      echo "=== Recent Events ==="
      kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp' | tail -10
    else
      echo -e "${YELLOW}Namespace '$NAMESPACE' not found${NC}"
    fi
    ;;

  logs)
    COMPONENT=${2:-backend}
    echo "📝 Viewing $COMPONENT logs..."

    if [ "$COMPONENT" = "backend" ]; then
      kubectl logs -f -n $NAMESPACE -l app=finance,component=backend --tail=100
    elif [ "$COMPONENT" = "frontend" ]; then
      kubectl logs -f -n $NAMESPACE -l app=finance,component=frontend --tail=100
    elif [ "$COMPONENT" = "mysql" ]; then
      kubectl logs -f -n $NAMESPACE -l app.kubernetes.io/name=mysql --tail=100
    else
      echo -e "${RED}Unknown component. Use: backend, frontend, or mysql${NC}"
      exit 1
    fi
    ;;

  test)
    echo "🧪 Testing Finance deployment..."

    # Check if MySQL is ready
    echo "Checking MySQL..."
    kubectl exec -n $NAMESPACE mysql-0 -- mysql -u root -p"$(kubectl get secret -n $NAMESPACE finance-mysql -o jsonpath='{.data.mysql-root-password}' | base64 -d)" -e "SELECT 1" &>/dev/null
    if [ $? -eq 0 ]; then
      echo -e "${GREEN}✅ MySQL is accessible${NC}"
    else
      echo -e "${RED}❌ MySQL connection failed${NC}"
    fi

    # Check backend health
    echo "Checking backend..."
    BACKEND_HEALTH=$(kubectl exec -n $NAMESPACE deployment/finance-backend -- curl -s http://localhost:8080/api/actuator/health | grep -o '"status":"UP"')
    if [ -n "$BACKEND_HEALTH" ]; then
      echo -e "${GREEN}✅ Backend is healthy${NC}"
    else
      echo -e "${RED}❌ Backend health check failed${NC}"
    fi

    # Check frontend
    echo "Checking frontend..."
    FRONTEND_STATUS=$(kubectl exec -n $NAMESPACE deployment/finance-frontend -- curl -s -o /dev/null -w "%{http_code}" http://localhost:80/)
    if [ "$FRONTEND_STATUS" = "200" ]; then
      echo -e "${GREEN}✅ Frontend is accessible${NC}"
    else
      echo -e "${RED}❌ Frontend check failed${NC}"
    fi

    echo ""
    echo "Test complete!"
    ;;

  rollback)
    REVISION=${2:-0}
    echo "⏪ Rolling back Finance..."

    if [ "$REVISION" = "0" ]; then
      # Rollback to previous
      helm rollback $RELEASE_NAME -n $NAMESPACE
    else
      # Rollback to specific revision
      helm rollback $RELEASE_NAME $REVISION -n $NAMESPACE
    fi

    echo -e "${GREEN}✅ Rollback initiated${NC}"
    echo ""
    echo "Waiting for rollout..."
    kubectl rollout status deployment/finance-backend -n $NAMESPACE || true
    kubectl rollout status deployment/finance-frontend -n $NAMESPACE || true
    ;;

  history)
    echo "📜 Release History"
    helm history $RELEASE_NAME -n $NAMESPACE
    ;;

  *)
    echo "Usage: $0 [install|upgrade|uninstall|status|logs|test|rollback|history]"
    echo ""
    echo "Commands:"
    echo "  install   - Install the Finance chart"
    echo "  upgrade   - Upgrade an existing installation"
    echo "  uninstall - Remove the Finance chart"
    echo "  status    - Show deployment status"
    echo "  logs      - View logs (usage: ./deploy.sh logs [backend|frontend|mysql])"
    echo "  test      - Run basic health tests"
    echo "  rollback  - Rollback to previous revision (usage: ./deploy.sh rollback [revision])"
    echo "  history   - Show release history"
    exit 1
    ;;
esac
