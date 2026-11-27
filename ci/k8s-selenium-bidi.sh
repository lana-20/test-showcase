#!/usr/bin/env bash
# Deploy the demo app and a Selenium Grid (hub + Chrome, Firefox and Edge
# nodes) to the kind cluster, then run the Selenium BiDi suite as a Job.
# Expects the demo-app and selenium-bidi images tagged :ci, and /tmp/k8s-out
# shared with the node (k8s/kind-config.yaml).
set -euo pipefail
cd "$(dirname "$0")/.."
NS=showcase
diagnose() {
  kubectl -n $NS get all,events 2>/dev/null | tail -50
  kubectl -n $NS describe pods 2>/dev/null | tail -80
  kubectl -n $NS logs deployment/selenium-hub --tail=80 2>/dev/null || true
}
trap 'code=$?; [ $code -ne 0 ] && diagnose; exit $code' EXIT
REG="${REGISTRY:-ghcr.io/lana-20}"
kind load docker-image "$REG/test-showcase-demo-app:ci" "$REG/test-showcase-selenium-bidi:ci" --name showcase

kubectl apply -f k8s/namespace.yaml
kubectl -n $NS create configmap run-info --from-literal=run-url="${RUN_URL:-}" \
  --from-literal=commit="${GITHUB_SHA:-}" --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/demo-app.yaml -f k8s/selenium-grid.yaml
# The node images are large; the first pull takes a few minutes.
for d in demo-app selenium-hub selenium-node-chrome selenium-node-firefox selenium-node-edge; do
  kubectl -n $NS rollout status "deployment/$d" --timeout=600s
done

# Ready pods are not enough: wait until all three nodes have registered with the hub.
for _ in $(seq 1 60); do
  status="$(kubectl -n $NS exec deployment/selenium-hub -- curl -fsS http://localhost:4444/status 2>/dev/null || true)"
  nodes="$(printf '%s' "$status" | python3 -c 'import json,sys; print(sum(1 for n in json.load(sys.stdin)["value"]["nodes"] if n["availability"] == "UP"))' 2>/dev/null || echo 0)"
  echo "Grid nodes up: $nodes/3"
  [ "$nodes" = "3" ] && break
  sleep 5
done
[ "$nodes" = "3" ]
printf '%s' "$status" | python3 -c '
import json, sys
for n in json.load(sys.stdin)["value"]["nodes"]:
    s = n["slots"][0]["stereotype"]
    print(f"  {s.get(\"browserName\")} {s.get(\"browserVersion\")}: {len(n[\"slots\"])} slots, {n[\"availability\"]}")'

kubectl -n $NS delete job selenium-bidi --ignore-not-found
kubectl apply -f k8s/selenium-bidi-job.yaml

# Wait for the Job to finish either way, then show its log.
for _ in $(seq 1 240); do
  ok=$(kubectl -n $NS get job selenium-bidi -o jsonpath='{.status.succeeded}')
  bad=$(kubectl -n $NS get job selenium-bidi -o jsonpath='{.status.failed}')
  [ -n "$ok" ] || [ -n "$bad" ] && break
  sleep 5
done
kubectl -n $NS logs job/selenium-bidi --tail=400 || true
kubectl -n $NS get pods -o wide
[ "$(kubectl -n $NS get job selenium-bidi -o jsonpath='{.status.succeeded}')" = "1" ]
