#!/usr/bin/env bash
# Builds the Docker image of every service and, optionally, pushes them.
#
# Usage:
#   scripts/build-images.sh <docker-hub-user> [tag] [--push]
#
# Example:
#   scripts/build-images.sh myuser 1.1.0 --push
#
# Then set the same user and tag in k8s/kustomization.yaml.
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <docker-hub-user> [tag] [--push]" >&2
  exit 1
fi

REGISTRY_USER="$1"
TAG="${2:-1.1.0}"
PUSH="${3:-}"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SERVICES=(eureka-server api-gateway cake-catalog-service order-service rating-service notification-service frontend)

for service in "${SERVICES[@]}"; do
  image="${REGISTRY_USER}/${service}:${TAG}"
  echo "Building ${image}"
  docker build -t "${image}" "${ROOT_DIR}/${service}"
  if [ "${PUSH}" = "--push" ]; then
    docker push "${image}"
  fi
done
