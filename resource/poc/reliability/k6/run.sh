#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../../../" && pwd)"

: "${BASE_URL:?BASE_URL is required}"
: "${PRACTICE_ID:?PRACTICE_ID is required}"
: "${STRATEGY_ID:?STRATEGY_ID is required}"
: "${SCENARIO_ID:?SCENARIO_ID is required}"
: "${RUN_ID:?RUN_ID is required}"

OUTPUT_DIR="${OUTPUT_DIR:-${ROOT_DIR}/resource/poc/reliability/k6/out}"
RUN_DIR="${OUTPUT_DIR}/${RUN_ID}/${STRATEGY_ID}/${SCENARIO_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_JSON="${RUN_DIR}/summary.json"
REQUESTS_NDJSON="${RUN_DIR}/requests.ndjson"
METRICS_CSV="${RUN_DIR}/metrics.csv"

echo "Running k6 scenario=${SCENARIO_ID} strategy=${STRATEGY_ID} run_id=${RUN_ID}"
k6 run \
  --summary-export "${SUMMARY_JSON}" \
  --out "json=${REQUESTS_NDJSON}" \
  "${SCRIPT_DIR}/feedback-lab.js"

python3 - <<'PY' "${REQUESTS_NDJSON}" "${METRICS_CSV}" "${RUN_ID}" "${STRATEGY_ID}" "${SCENARIO_ID}"
import csv
import json
import sys
from collections import Counter

infile, outfile, run_id, strategy_id, scenario_id = sys.argv[1:]
status_counter = Counter()
total = 0

with open(infile, "r", encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
        except json.JSONDecodeError:
            continue
        if obj.get("type") != "Point":
            continue
        metric = obj.get("metric")
        if metric != "http_reqs":
            continue
        total += 1
        status = obj.get("data", {}).get("tags", {}).get("status", "unknown")
        status_counter[status] += 1

with open(outfile, "w", newline="", encoding="utf-8") as f:
    writer = csv.writer(f)
    writer.writerow(["run_id", "strategy_id", "scenario_id", "total_requests", "status", "count", "ratio"])
    for status in sorted(status_counter.keys()):
        count = status_counter[status]
        ratio = (count / total) if total else 0.0
        writer.writerow([run_id, strategy_id, scenario_id, total, status, count, f"{ratio:.6f}"])
PY

echo "Artifacts written to ${RUN_DIR}"

# Repeatability gate: compare this run against previous run for same strategy/scenario if provided.
# Usage:
#   PREV_RUN_ID=run-previous BASE_URL=... PRACTICE_ID=... STRATEGY_ID=... SCENARIO_ID=... RUN_ID=run-current ./run.sh
if [[ -n "${PREV_RUN_ID:-}" ]]; then
  PREV_CSV="${OUTPUT_DIR}/${PREV_RUN_ID}/${STRATEGY_ID}/${SCENARIO_ID}/metrics.csv"
  if [[ -f "${PREV_CSV}" ]]; then
    python3 - <<'PY' "${PREV_CSV}" "${METRICS_CSV}"
import csv
import sys

prev_csv, curr_csv = sys.argv[1:]

def load_totals(path):
    total = 0
    non2xx = 0
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            total = int(row["total_requests"])
            status = row["status"]
            count = int(row["count"])
            if not status.startswith("2"):
                non2xx += count
    return total, non2xx

prev_total, prev_non2xx = load_totals(prev_csv)
curr_total, curr_non2xx = load_totals(curr_csv)

def drift(a, b):
    if a == 0:
        return 0.0 if b == 0 else 1.0
    return abs(b - a) / a

total_drift = drift(prev_total, curr_total)
prev_ratio = (prev_non2xx / prev_total) if prev_total else 0.0
curr_ratio = (curr_non2xx / curr_total) if curr_total else 0.0
ratio_pp = abs(curr_ratio - prev_ratio) * 100.0

print(f"Repeatability check total_drift={total_drift:.4f}, non2xx_ratio_pp={ratio_pp:.4f}")

if total_drift > 0.05:
    raise SystemExit("Repeatability gate failed: request count drift > 5%")
if ratio_pp > 3.0:
    raise SystemExit("Repeatability gate failed: non-2xx ratio drift > 3 percentage points")
PY
  else
    echo "Previous metrics not found at ${PREV_CSV}; skipping repeatability gate."
  fi
fi
