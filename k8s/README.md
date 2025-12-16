# Finance Helm Chart Deployment

Complete Helm-based deployment for Personal Finance Management System with integrated MySQL database.

## Quick Start

### 1. Add Bitnami Repository

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

### 2. Deploy

```bash
cd k8s
./deploy.sh install
```

### 3. Access

```bash
kubectl port-forward -n finance svc/finance-frontend 3000:80
# Open: http://localhost:3000
```

## What's Included

- **Frontend**: Vue.js application (Nginx)
- **Backend**: Spring Boot REST API
- **Database**: MySQL 8.0 (deployed via Bitnami Helm dependency)
- **Storage**: Persistent volume for MySQL data
- **Ingress**: Optional TLS-enabled ingress
- **Auto-scaling**: Optional HPA configuration
- **Health Checks**: Liveness and readiness probes
- **Init Containers**: Backend waits for MySQL to be ready

## Directory Structure

```
k8s/
├── finance-chart/               # Helm chart
│   ├── Chart.yaml              # Chart metadata + MySQL dependency
│   ├── values.yaml             # Configuration
│   ├── templates/              # Application manifests
│   │   ├── namespace.yaml
│   │   ├── configmap.yaml
│   │   ├── secret.yaml
│   │   ├── backend-deployment.yaml
│   │   ├── backend-service.yaml
│   │   ├── frontend-deployment.yaml
│   │   ├── frontend-service.yaml
│   │   ├── ingress.yaml
│   │   ├── hpa.yaml
│   │   └── _helpers.tpl
│   ├── README.md               # Chart documentation
│   └── .helmignore
├── deploy.sh                   # Deployment script
└── README.md                   # This file
```

**Note**: MySQL部署通过Helm dependency自动管理（在Chart.yaml中定义），不需要手动创建MySQL deployment文件。

## Deployment Commands

```bash
# Install
./deploy.sh install

# Upgrade
./deploy.sh upgrade

# Check status
./deploy.sh status

# View logs
./deploy.sh logs backend
./deploy.sh logs frontend
./deploy.sh logs mysql

# Test deployment
./deploy.sh test

# Rollback
./deploy.sh rollback [revision]

# View history
./deploy.sh history

# Uninstall
./deploy.sh uninstall
```

## Custom Configuration

编辑 `finance-chart/values.yaml` 或使用自定义values文件：

```bash
# 使用自定义配置安装
helm install finance finance-chart/ -n finance --create-namespace -f my-values.yaml
```

## Common Configuration Examples

### Update Image Version

```bash
helm upgrade finance finance-chart/ -n finance \
  --set backend.image.tag=v1.0.2 \
  --set frontend.image.tag=v1.0.2
```

### Change MySQL Passwords (Production)

```bash
helm upgrade finance finance-chart/ -n finance \
  --set mysql.auth.rootPassword=secure-root-pass \
  --set mysql.auth.password=secure-user-pass
```

### Scale Replicas

```bash
helm upgrade finance finance-chart/ -n finance \
  --set backend.replicaCount=3 \
  --set frontend.replicaCount=2
```

### Configure Ingress

```bash
helm upgrade finance finance-chart/ -n finance \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=finance.example.com
```

## Monitoring and Debugging

```bash
# Check pod status
kubectl get pods -n finance

# View logs (or use ./deploy.sh logs <component>)
kubectl logs -f -n finance -l component=backend
kubectl logs -f -n finance -l component=frontend
kubectl logs -f -n finance -l app.kubernetes.io/name=mysql

# Connect to MySQL
kubectl exec -it -n finance mysql-0 -- mysql -u root -p

# Check backend health
kubectl exec -n finance deployment/finance-backend -- \
  curl -s http://localhost:8080/api/actuator/health
```

## Backup MySQL

```bash
# Get password
MYSQL_ROOT_PASSWORD=$(kubectl get secret -n finance finance-mysql \
  -o jsonpath="{.data.mysql-root-password}" | base64 -d)

# Backup
kubectl exec -n finance mysql-0 -- \
  mysqldump -u root -p${MYSQL_ROOT_PASSWORD} finance > backup-$(date +%Y%m%d).sql

# Restore
kubectl exec -i -n finance mysql-0 -- \
  mysql -u root -p${MYSQL_ROOT_PASSWORD} finance < backup.sql
```

## Troubleshooting

```bash
# Check events
kubectl get events -n finance --sort-by='.lastTimestamp'

# Check init container (if backend stuck)
kubectl logs -n finance <pod-name> -c wait-for-mysql

# Test MySQL connectivity
kubectl exec -it -n finance deployment/finance-backend -- \
  nc -zv mysql.finance.svc.cluster.local 3306
```

## Production Checklist

- [ ] 修改MySQL密码
- [ ] 配置storage class和容量
- [ ] 设置资源限制
- [ ] 配置ingress域名
- [ ] 启用TLS
- [ ] 配置备份策略

## Version Compatibility

- **Kubernetes**: 1.19+
- **Helm**: 3.x
- **MySQL Chart**: 9.14.4 (Bitnami)
