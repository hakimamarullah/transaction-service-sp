#!/bin/bash

set -e
set -o pipefail

kubectl create configmap general-configmap --from-env-file=.env.resi.local