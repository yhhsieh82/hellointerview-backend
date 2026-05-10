# T7 — Experiment summary CSV and report template

This folder holds the **normative header row** for `experiment_summary.csv` ([`experiment_summary.headers.csv`](experiment_summary.headers.csv), T1 §14.6 column set) and the **markdown skeleton** for per-run evaluation notes ([`experiment_summary.template.md`](experiment_summary.template.md)).

## How rows are produced

1. [`run.sh`](../../k6/run.sh) snapshots Micrometer counters from `${BASE_URL}/actuator/metrics` immediately **before** and **after** each k6 cell (`metrics-before.json`, `metrics-after.json` under the cell directory).
2. [`collect-experiment-row.py`](../../k6/collect-experiment-row.py) computes **tag-filtered deltas** for the lab tuple `(run_id, strategy_id, scenario_id)` and appends one row to:

   `resource/poc/reliability/k6/out/{run_id}/experiment_summary.csv`

3. After a full matrix, [`run-matrix.sh`](../../k6/run-matrix.sh) writes `experiment_summary.md` next to that CSV by filling [`experiment_summary.template.md`](experiment_summary.template.md).

## Denominators (T1 §14.5)

| Field | Denominator / rule |
|-------|-------------------|
| `feedback_success_rate` | `successful_feedbacks / logical_submissions` |
| `retry_exhausted_rate` | `exhausted_submissions / logical_submissions` |
| `success_after_retry_ratio` | `successful_with_attempt_gt_1 / successful_feedbacks` (empty in POC until a dedicated counter exists) |
| `provider_calls_per_success` | `provider_attempts / successful_feedbacks` |

`logical_submissions` is approximated by the delta of `feedback_requests_total` with `outcome=accepted` (one per HTTP submit that passes early validation).

## Isolation and JVM lifetime

Deltas assume **monotonic counters** on a single JVM. Avoid concurrent lab traffic against the same process. For a clean counter baseline, restart the backend between full `run_id` campaigns.

## Latency columns (POC caveat)

`provider_path_latency_p95_ms` and `provider_path_latency_p99_ms` are filled from **k6** `http_req_duration` percentiles for the cell. That is an **HTTP client** proxy, not the T1 provider-stage SLI from observability. Use your metrics backend (T5) for contract-true provider-path percentiles before SLO sign-off.

## Tags (T1 §14.6)

`strategy_id`, `scenario_id`, and `classification_version` appear as CSV columns. Also set at collection time: `contract_version` is represented by `classification_version` for the failure taxonomy snapshot; bump when §14.4–14.6 changes. Optional environment defaults for traceability in the markdown report: `LAB_ENV`, `LLM_PROVIDER`, `STUB_MODEL` / `GEMINI_MODEL` / `OLLAMA_MODEL`.

## Disable collection

If you need k6-only artifacts without hitting actuator:

```bash
export LAB_SKIP_EXPERIMENT_SUMMARY=1
```
