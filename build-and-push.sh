#!/bin/bash

# Build and push Docker images to Docker Hub
# Usage: ./build-and-push.sh [version]

set -e

# Default version if not provided
VERSION=${1:-latest}
DOCKER_USERNAME="xuaustin"

echo "🐳 Building Docker images for version: $VERSION"

# Build backend image
echo "📦 Building backend image..."
cd backend
docker build -t ${DOCKER_USERNAME}/finance-backend:${VERSION} .
docker tag ${DOCKER_USERNAME}/finance-backend:${VERSION} ${DOCKER_USERNAME}/finance-backend:latest
cd ..

# Build frontend image
echo "📦 Building frontend image..."
cd frontend
docker build -t ${DOCKER_USERNAME}/finance-frontend:${VERSION} .
docker tag ${DOCKER_USERNAME}/finance-frontend:${VERSION} ${DOCKER_USERNAME}/finance-frontend:latest
cd ..

echo "✅ Images built successfully!"
echo ""
echo "🚀 Pushing images to Docker Hub..."

# Login to Docker Hub (will prompt for credentials if not logged in)
docker login

# Push images
echo "📤 Pushing backend image..."
docker push ${DOCKER_USERNAME}/finance-backend:${VERSION}
docker push ${DOCKER_USERNAME}/finance-backend:latest

echo "📤 Pushing frontend image..."
docker push ${DOCKER_USERNAME}/finance-frontend:${VERSION}
docker push ${DOCKER_USERNAME}/finance-frontend:latest

echo ""
echo "✅ All images pushed successfully!"
echo ""
echo "Backend images:"
echo "  - ${DOCKER_USERNAME}/finance-backend:${VERSION}"
echo "  - ${DOCKER_USERNAME}/finance-backend:latest"
echo ""
echo "Frontend images:"
echo "  - ${DOCKER_USERNAME}/finance-frontend:${VERSION}"
echo "  - ${DOCKER_USERNAME}/finance-frontend:latest"
