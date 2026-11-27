#!/usr/bin/env bash
# Deploy the demo app to the kind cluster and run the Playwright suite as a Job.
# Expects the images tagged :ci and loaded, and /tmp/k8s-out shared with the node.
set -euo pipefail
REG="${REGISTRY:-ghcr.io/lana-20}"
kind load docker-image "$REG/test-showcase-demo-app:ci" "$REG/test-showcase-playwright:ci" --name showcase

kubectl apply -f k8s/namespace.yaml
kubectl -n showcase create configmap run-info --from-literal=run-url="${RUN_URL:-}" \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/demo-app.yaml
kubectl -n showcase rollout status deployment/demo-app --timeout=120s

kubectl -n showcase delete job playwright --ignore-not-found
kubectl apply -f k8s/playwright-job.yaml

# Wait for the Job to finish either way, then show its log.
for _ in $(seq 1 120); do
  ok=$(kubectl -n showcase get job playwright -o jsonpath='{.status.succeeded}')
  bad=$(kubectl -n showcase get job playwright -o jsonpath='{.status.failed}')
  [ -n "$ok" ] || [ -n "$bad" ] && break
  sleep 5
done
kubectl -n showcase logs job/playwright --tail=200 || true
kubectl -n showcase get pods -o wide
[ "$(kubectl -n showcase get job playwright -o jsonpath='{.status.succeeded}')" = "1" ]
