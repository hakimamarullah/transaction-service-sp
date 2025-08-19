#!/bin/bash

set -e
set -o pipefail

kubectl delete configmap general-configmap --from-env-file=.env.resi.local