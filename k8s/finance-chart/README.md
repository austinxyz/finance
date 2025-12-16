# Finance Helm Chart

A complete Helm chart for deploying the Personal Finance Management System with MySQL database.

## Features

- **Complete Stack**: Frontend (Vue.js), Backend (Spring Boot), Database (MySQL)
- **MySQL Included**: Uses Bitnami MySQL chart as dependency
- **Auto-scaling**: Optional HPA for both frontend and backend
- **Health Checks**: Liveness and readiness probes
- **Persistent Storage**: MySQL data persistence with PVC
- **Ingress**: TLS-enabled ingress with cert-manager support
- **Init Container**: Backend waits for MySQL to be ready before starting

## Prerequisites

- Kubernetes cluster (1.19+)
- Helm 3.x
- kubectl configured
- Storage class available for PVC
- (Optional) cert-manager for TLS certificates
- (Optional) nginx-ingress-controller

## Quick Start

### 1. Add Bitnami Repository

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

### 2. Install Chart

```bash
# Install with default values
helm install finance ./finance-chart -n finance --create-namespace

# Or install with custom values
helm install finance ./finance-chart -n finance --create-namespace -f custom-values.yaml
```

### 3. Verify Installation

```bash
# Check all resources
kubectl get all -n finance

# Check PVC
kubectl get pvc -n finance

# Check pods
kubectl get pods -n finance -w
```

### 4. Access Application

```bash
# Port forward (for testing)
kubectl port-forward -n finance svc/finance-frontend 3000:80

# Or via ingress (if configured)
# https://your-domain.com
```

## Configuration

### Key Values to Customize

```yaml
# values.yaml

# Domain configuration
ingress:
  hosts:
    - host: finance.your-domain.com

# MySQL authentication
mysql:
  auth:
    rootPassword: "change-me-in-production"
    database: "finance"
    username: "financeuser"
    password: "change-me-in-production"

# Database connection
database:
  host: mysql
  port: 3306
  name: finance
  username: financeuser
  password: change-me-in-production

# Storage
mysql:
  primary:
    persistence:
      size: 20Gi
      storageClass: "your-storage-class"

# Image versions
backend:
  image:
    tag: v1.0.1
frontend:
  image:
    tag: v1.0.1
```

### Common Customizations

#### Use External MySQL

```yaml
# values.yaml
mysql:
  enabled: false

database:
  host: external-mysql.example.com
  port: 3306
  name: finance
  username: financeuser
  password: external-db-password
```

#### Enable Auto-scaling

```yaml
# values.yaml
autoscaling:
  backend:
    enabled: true
    minReplicas: 2
    maxReplicas: 10
  frontend:
    enabled: true
    minReplicas: 2
    maxReplicas: 5
```

#### Disable Ingress

```yaml
# values.yaml
ingress:
  enabled: false
```

## Installation Examples

### Development Environment

```bash
helm install finance ./finance-chart \
  --namespace finance \
  --create-namespace \
  --set mysql.primary.persistence.size=5Gi \
  --set backend.replicaCount=1 \
  --set frontend.replicaCount=1 \
  --set ingress.enabled=false
```

### Production Environment

```bash
helm install finance ./finance-chart \
  --namespace finance \
  --create-namespace \
  --set mysql.auth.rootPassword="$(openssl rand -base64 32)" \
  --set mysql.auth.password="$(openssl rand -base64 32)" \
  --set mysql.primary.persistence.size=50Gi \
  --set mysql.primary.persistence.storageClass=fast-ssd \
  --set backend.replicaCount=3 \
  --set frontend.replicaCount=3 \
  --set autoscaling.backend.enabled=true \
  --set autoscaling.frontend.enabled=true \
  --set ingress.hosts[0].host=finance.example.com
```

### With Custom Values File

```bash
# Create production-values.yaml
cat > production-values.yaml <<EOF
mysql:
  auth:
    rootPassword: "production-root-password"
    password: "production-user-password"
  primary:
    persistence:
      size: 100Gi
      storageClass: premium-ssd

backend:
  replicaCount: 5
  image:
    tag: v1.0.1

frontend:
  replicaCount: 3
  image:
    tag: v1.0.1

autoscaling:
  backend:
    enabled: true
    maxReplicas: 20
  frontend:
    enabled: true
    maxReplicas: 10

ingress:
  hosts:
    - host: finance.mycompany.com
EOF

# Install with custom values
helm install finance ./finance-chart -n finance --create-namespace -f production-values.yaml
```

## Upgrade

```bash
# Upgrade with new image version
helm upgrade finance ./finance-chart -n finance \
  --set backend.image.tag=v1.0.2 \
  --set frontend.image.tag=v1.0.2

# Upgrade with custom values file
helm upgrade finance ./finance-chart -n finance -f production-values.yaml

# Upgrade with reuse of existing values
helm upgrade finance ./finance-chart -n finance --reuse-values
```

## Rollback

```bash
# List releases
helm list -n finance

# Show history
helm history finance -n finance

# Rollback to previous version
helm rollback finance -n finance

# Rollback to specific revision
helm rollback finance 2 -n finance
```

## Uninstall

```bash
# Uninstall release (keeps PVCs)
helm uninstall finance -n finance

# Delete PVCs (WARNING: This deletes all data!)
kubectl delete pvc -n finance -l app.kubernetes.io/instance=finance

# Delete namespace
kubectl delete namespace finance
```

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -n finance
kubectl describe pod <pod-name> -n finance
kubectl logs <pod-name> -n finance
```

### MySQL Not Starting

```bash
# Check MySQL logs
kubectl logs -n finance -l app.kubernetes.io/component=primary

# Check PVC
kubectl get pvc -n finance

# Check storage class
kubectl get storageclass
```

### Backend Can't Connect to Database

```bash
# Check secret
kubectl get secret -n finance
kubectl describe secret finance-backend-secret -n finance

# Check database connectivity from backend pod
kubectl exec -it -n finance deployment/finance-backend -- /bin/bash
# Inside pod:
# curl -v telnet://mysql.finance.svc.cluster.local:3306
```

### Init Container Failing

```bash
# Check init container logs
kubectl logs -n finance <pod-name> -c wait-for-mysql
```

## Chart Dependencies

This chart depends on:
- Bitnami MySQL Chart v9.14.4

Dependencies are automatically downloaded during `helm install`.

To manually update dependencies:

```bash
helm dependency update ./finance-chart
```

## Values Reference

See [values.yaml](values.yaml) for complete configuration options.

## Database Schema

The database schema is automatically created by Spring Boot's `ddl-auto=update` feature.

For manual schema initialization, you can add SQL scripts in `values.yaml`:

```yaml
mysql:
  initdbScripts:
    02-custom-schema.sql: |
      USE finance;
      -- Your custom SQL here
```

## Backup and Restore

### Backup MySQL Data

```bash
# Create backup
kubectl exec -n finance mysql-0 -- mysqldump -u root -p<root-password> finance > backup.sql

# Or use a CronJob for automated backups
```

### Restore MySQL Data

```bash
# Restore from backup
kubectl exec -i -n finance mysql-0 -- mysql -u root -p<root-password> finance < backup.sql
```

## Monitoring

### Health Endpoints

- Backend: `http://finance-backend:8080/api/actuator/health`
- Frontend: `http://finance-frontend:80/`

### Metrics (if enabled)

- Backend: `http://finance-backend:8080/api/actuator/metrics`
- Backend: `http://finance-backend:8080/api/actuator/prometheus`

## Security Considerations

1. **Change Default Passwords**: Always change MySQL passwords in production
2. **Use Secrets Management**: Consider using sealed-secrets or external secrets
3. **Enable TLS**: Configure ingress with proper TLS certificates
4. **Network Policies**: Add network policies to restrict pod communication
5. **RBAC**: Configure proper service accounts and RBAC rules
6. **Image Scanning**: Scan images for vulnerabilities before deployment

## License

MIT
