#!/bin/bash

set -e
set -o pipefail

echo "Starting ingress...."

kubectl apply -f k8s/ingress.yaml

echo "Ingress created"