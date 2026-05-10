#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../../../" && pwd)"

: "${BASE_URL:?BASE_URL is required}"
: "${PRACTICE_ID:?PRACTICE_ID is required}"
: "${RUN_ID:?RUN_ID is required}"

STRATEGIES="${STRATEGIES:-A,B,C}"
SCENARIOS="${SCENARIOS:-S1,S2,S3,S4,S5}"
STOP_ON_FAILURE="${STOP_ON_FAILURE:-true}"
RESUME="${RESUME:-false}"
OUTPUT_DIR="${OUTPUT_DIR:-${ROOT_DIR}/resource/poc/reliability/k6/out}"
RUN_DIR="${OUTPUT_DIR}/${RUN_ID}"
STATUS_CSV="${RUN_DIR}/matrix-status.csv"
MANIFEST_JSON="${RUN_DIR}/matrix-manifest.json"

ALLOWED_STRATEGIES=("A" "B" "C" "D")
ALLOWED_SCENARIOS=("S1" "S2" "S3" "S4" "S5")

declare -a STRATEGY_LIST=()
declare -a SCENARIO_LIST=()

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}" >&2
    exit 1
  fi
}

contains() {
  local value="$1"
  shift
  local item
  for item in "$@"; do
    if [[ "${item}" == "${value}" ]]; then
      return 0
    fi
  done
  return 1
}

split_csv() {
  local raw="$1"
  IFS=',' read -r -a _split <<< "${raw}"
  local token
  for token in "${_split[@]}"; do
    token="${token//[[:space:]]/}"
    if [[ -n "${token}" ]]; then
      printf '%s\n' "${token}"
    fi
  done
}

normalize_bool() {
  local value
  value="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"
  case "${value}" in
    true|false)
      printf '%s\n' "${value}"
      ;;
    *)
      echo "Invalid boolean value '$1'. Use true|false." >&2
      exit 1
      ;;
  esac
}

write_manifest_start() {
  local started_at
  if [[ "${RESUME}" == "true" ]] && [[ -f "${MANIFEST_JSON}" ]]; then
    started_at="$(
      python3 -c 'import json,sys
path = sys.argv[1]
try:
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    v = data.get("started_at")
    if isinstance(v, str) and v:
        print(v)
except (OSError, json.JSONDecodeError, TypeError):
    pass
' "${MANIFEST_JSON}" 2>/dev/null || true
    )"
  fi
  if [[ -z "${started_at:-}" ]]; then
    started_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  fi
  local strategies_json="[]"
  local scenarios_json="[]"

  if ((${#STRATEGY_LIST[@]} > 0)); then
    strategies_json="$(printf '%s\n' "${STRATEGY_LIST[@]}" | python3 -c 'import json,sys; print(json.dumps([x.strip() for x in sys.stdin if x.strip()]))')"
  fi
  if ((${#SCENARIO_LIST[@]} > 0)); then
    scenarios_json="$(printf '%s\n' "${SCENARIO_LIST[@]}" | python3 -c 'import json,sys; print(json.dumps([x.strip() for x in sys.stdin if x.strip()]))')"
  fi

  cat > "${MANIFEST_JSON}" <<EOF
{
  "run_id": "${RUN_ID}",
  "base_url": "${BASE_URL}",
  "practice_id": "${PRACTICE_ID}",
  "strategies": ${strategies_json},
  "scenarios": ${scenarios_json},
  "resume": ${RESUME},
  "stop_on_failure": ${STOP_ON_FAILURE},
  "started_at": "${started_at}",
  "ended_at": null
}
EOF
}

write_manifest_end() {
  local ended_at
  ended_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  python3 - <<'PY' "${MANIFEST_JSON}" "${ended_at}"
import json
import sys

manifest_path, ended_at = sys.argv[1], sys.argv[2]
with open(manifest_path, "r", encoding="utf-8") as f:
    manifest = json.load(f)
manifest["ended_at"] = ended_at
with open(manifest_path, "w", encoding="utf-8") as f:
    json.dump(manifest, f, indent=2)
    f.write("\n")
PY
}

append_status() {
  local strategy_id="$1"
  local scenario_id="$2"
  local status="$3"
  local started_at="$4"
  local ended_at="$5"
  local error_message="$6"

  local escaped_error="${error_message//\"/\"\"}"
  printf '%s,%s,%s,%s,%s,"%s"\n' \
    "${strategy_id}" \
    "${scenario_id}" \
    "${status}" \
    "${started_at}" \
    "${ended_at}" \
    "${escaped_error}" >> "${STATUS_CSV}"
}

render_experiment_summary() {
  local template="${ROOT_DIR}/resource/poc/reliability/artifacts/t7/experiment_summary.template.md"
  local out_md="${RUN_DIR}/experiment_summary.md"
  local lab_env="${LAB_ENV:-local}"
  local lab_provider="${LAB_PROVIDER:-${LLM_PROVIDER:-stub}}"
  local lab_model="${LAB_MODEL:-}"
  if [[ -z "${lab_model}" ]]; then
    if [[ -n "${STUB_MODEL:-}" ]]; then
      lab_model="${STUB_MODEL}"
    elif [[ -n "${GEMINI_MODEL:-}" ]]; then
      lab_model="${GEMINI_MODEL}"
    elif [[ -n "${OLLAMA_MODEL:-}" ]]; then
      lab_model="${OLLAMA_MODEL}"
    fi
  fi
  if [[ ! -f "${template}" ]]; then
    echo "Skipping experiment_summary.md render (missing template ${template})." >&2
    return 0
  fi
  python3 "${SCRIPT_DIR}/collect-experiment-row.py" render \
    --template "${template}" \
    --out "${out_md}" \
    --run-dir "${RUN_DIR}" \
    --run-id "${RUN_ID}" \
    --manifest "${MANIFEST_JSON}" \
    --experiment-csv "${RUN_DIR}/experiment_summary.csv" \
    --status-csv "${STATUS_CSV}" \
    --lab-env "${lab_env}" \
    --lab-provider "${lab_provider}" \
    --lab-model "${lab_model}" || echo "Warning: experiment_summary.md render failed." >&2
}

has_success() {
  local strategy_id="$1"
  local scenario_id="$2"

  if [[ ! -f "${STATUS_CSV}" ]]; then
    return 1
  fi

  python3 - <<'PY' "${STATUS_CSV}" "${strategy_id}" "${scenario_id}"
import csv
import sys

status_path, strategy_id, scenario_id = sys.argv[1:]
with open(status_path, newline="", encoding="utf-8") as f:
    for row in csv.DictReader(f):
        if row.get("strategy_id") == strategy_id and row.get("scenario_id") == scenario_id and row.get("status") == "success":
            raise SystemExit(0)
raise SystemExit(1)
PY
}

preflight() {
  require_cmd "k6"
  require_cmd "python3"
  require_cmd "curl"

  if ! curl -sSf -o /dev/null --max-time 5 "${BASE_URL}/actuator/health"; then
    echo "Backend reachability check failed at ${BASE_URL}/actuator/health" >&2
    exit 1
  fi
}

parse_and_validate() {
  local token
  while IFS= read -r token; do
    if ! contains "${token}" "${ALLOWED_STRATEGIES[@]}"; then
      echo "Invalid strategy '${token}'. Allowed: ${ALLOWED_STRATEGIES[*]}" >&2
      exit 1
    fi
    STRATEGY_LIST+=("${token}")
  done < <(split_csv "${STRATEGIES}")

  while IFS= read -r token; do
    if ! contains "${token}" "${ALLOWED_SCENARIOS[@]}"; then
      echo "Invalid scenario '${token}'. Allowed: ${ALLOWED_SCENARIOS[*]}" >&2
      exit 1
    fi
    SCENARIO_LIST+=("${token}")
  done < <(split_csv "${SCENARIOS}")

  if ((${#STRATEGY_LIST[@]} == 0)); then
    echo "No strategies selected." >&2
    exit 1
  fi
  if ((${#SCENARIO_LIST[@]} == 0)); then
    echo "No scenarios selected." >&2
    exit 1
  fi
}

main() {
  STOP_ON_FAILURE="$(normalize_bool "${STOP_ON_FAILURE}")"
  RESUME="$(normalize_bool "${RESUME}")"

  parse_and_validate
  preflight

  mkdir -p "${RUN_DIR}"
  if [[ ! -f "${STATUS_CSV}" ]]; then
    echo "strategy_id,scenario_id,status,started_at,ended_at,error_message" > "${STATUS_CSV}"
  fi
  write_manifest_start

  local failures=0
  local strategy_id
  local scenario_id

  for strategy_id in "${STRATEGY_LIST[@]}"; do
    for scenario_id in "${SCENARIO_LIST[@]}"; do
      if [[ "${RESUME}" == "true" ]] && has_success "${strategy_id}" "${scenario_id}"; then
        echo "Skipping ${strategy_id}/${scenario_id} (already successful)."
        continue
      fi

      local started_at ended_at
      started_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

      echo "Running matrix cell strategy=${strategy_id} scenario=${scenario_id}"
      set +e
      BASE_URL="${BASE_URL}" \
      PRACTICE_ID="${PRACTICE_ID}" \
      STRATEGY_ID="${strategy_id}" \
      SCENARIO_ID="${scenario_id}" \
      RUN_ID="${RUN_ID}" \
      OUTPUT_DIR="${OUTPUT_DIR}" \
      LAB_CELL_WINDOW_START_UTC="${started_at}" \
      bash "${SCRIPT_DIR}/run.sh"
      local exit_code=$?
      set -e

      ended_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

      if [[ ${exit_code} -eq 0 ]]; then
        append_status "${strategy_id}" "${scenario_id}" "success" "${started_at}" "${ended_at}" ""
      else
        failures=$((failures + 1))
        append_status "${strategy_id}" "${scenario_id}" "failed" "${started_at}" "${ended_at}" "run.sh exited with code ${exit_code}"
        echo "Cell failed strategy=${strategy_id} scenario=${scenario_id}" >&2
        if [[ "${STOP_ON_FAILURE}" == "true" ]]; then
          write_manifest_end
          render_experiment_summary
          echo "Halting matrix due to STOP_ON_FAILURE=true." >&2
          exit "${exit_code}"
        fi
      fi
    done
  done

  write_manifest_end
  render_experiment_summary

  if [[ ${failures} -gt 0 ]]; then
    echo "Matrix completed with ${failures} failed cell(s)." >&2
    exit 1
  fi

  echo "Matrix completed successfully. Run artifacts: ${RUN_DIR}"
}

main "$@"
