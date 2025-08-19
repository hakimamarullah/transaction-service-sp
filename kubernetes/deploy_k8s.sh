#!/bin/bash

set -e
set -o pipefail

echo "Starting deployment...."

kubectl apply -f k8s/components.yaml
kubectl apply -f k8s/ingress-controller.yaml
kubectl apply -f k8s/kafka
kubectl apply -f k8s/transactions-svc

echo "All Component Has Been Created"