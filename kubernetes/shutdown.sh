#!/bin/bash

set -e
set -o pipefail

echo "Starting deletion...."

kubectl delete -f k8s/components.yaml
kubectl delete -f k8s/ingress-controller.yaml
kubectl delete -f k8s/postgres
kubectl delete -f k8s/rabbitmq
kubectl delete -f k8s/redis
kubectl delete -f k8s/starline_scc
kubectl delete -f k8s/starline_registry
kubectl delete -f k8s/starline_gateway
kubectl delete -f k8s/resi
kubectl delete -f k8s/scrapper_svc
kubectl delete -f k8s/user_svc
kubectl delete -f k8s/github-secret.yaml
kubectl delete -f k8s/starline_scc
kubectl delete -f k8s/frontend

echo "All Component Has Been Deleted"