#!/bin/bash

set -e
set -o pipefail

echo "Starting deletion...."

kubectl delete -f k8s/components.yaml
kubectl delete -f k8s/ingress-controller.yaml
kubectl delete -f k8s/kafka
kubectl delete -f k8s/transactions-svc
kubectl delete -f k8s/ingress.yaml

echo "All Component Has Been Deleted"