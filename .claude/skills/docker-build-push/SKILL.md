---
name: Docker Build and Push
description: Build and push multi-architecture Docker images (amd64/arm64) for frontend and backend to xuaustin's Docker Hub. Use this skill when the user asks to build, push, or deploy Docker images.
---

# Docker Build and Push Skill

Build and push multi-architecture Docker images using the existing `build-multiarch.sh` script.

## Usage

```bash
# Build with specific version
./build-multiarch.sh v1.2.3

# Build with default version (v1.0.0)
./build-multiarch.sh
```

## What It Does

1. Creates/uses Docker buildx builder for multi-platform support
2. Logs into Docker Hub (xuaustin account)
3. Builds backend image: `xuaustin/finance-backend:VERSION` + `latest`
4. Builds frontend image: `xuaustin/finance-frontend:VERSION` + `latest`
5. Pushes to Docker Hub with both amd64 and arm64 platforms

## Verification

```bash
# Check platforms
docker buildx imagetools inspect xuaustin/finance-backend:latest
docker buildx imagetools inspect xuaustin/finance-frontend:latest
```

## Troubleshooting

- **buildx not found**: `docker buildx install`
- **Builder not found**: `docker buildx create --name multiarch --use`
- **Login required**: `docker login` (use xuaustin credentials)
- **Permission denied**: `chmod +x build-multiarch.sh`
