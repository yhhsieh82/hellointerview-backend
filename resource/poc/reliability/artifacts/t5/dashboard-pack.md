# T5 Dashboard Pack (POC)

## Scope

Task T5 from `resource/prds/backend/06-feedback-reliability-lab.md`: build minimum dashboard views for operator diagnosis during the POC phase. Alert implementation is deferred.

## Dashboard Filters (All Panels)

- `strategy_id`: `A|B|C|D`
- `scenario_id`: `S1|S2|S3|S4|S5`
- `run_id`
- `provider`
- `model`
- Time presets: `15m`, `1h`, `24h`

## Panel 1: SLO Panel

Purpose: quick pass/fail read against the T1 contract.

- `feedback_success_rate` (target >= `99.0%`, 30m rolling)
- `provider_path_latency_p95_ms` (target <= `8000`, 10m rolling)
- `provider_path_latency_p99_ms` (target <= `20000`, 10m rolling)
- `retry_exhausted_rate` (target <= `1.0%`, 30m rolling)

Drill-down path: SLO breach -> Panel 2 (Saturation) and Panel 4 (Provider Failures)

## Panel 2: Saturation Panel

Purpose: identify local pressure and timeout drift.

- `llm_provider_inflight_calls`
- `feedback_requests_total{outcome=accepted|rejected|degraded}`
- `llm_provider_failures_total{failure_class=provider_timeout}`

Drill-down path: inflight growth or timeout spike -> Panel 5 (Admission)

## Panel 3: Retry Behavior Panel

Purpose: determine if retries help or amplify cost/latency.

- `llm_provider_retry_attempts_total`
- `llm_provider_retry_outcome_total`
- `success_after_retry_ratio`
- `provider_calls_per_success`

Drill-down path: poor retry efficiency -> Panel 4 (Provider Failures)

## Panel 4: Provider Failure Panel

Purpose: classify transient vs terminal pressure source.

- `llm_provider_failures_total{failure_class=throttling_429}`
- `llm_provider_failures_total{failure_class=provider_5xx}`
- `llm_provider_failures_total{failure_class=provider_timeout}`
- `llm_provider_failures_total{failure_class=terminal_config_auth}`
- `llm_provider_failures_total{failure_class=unknown}`

Drill-down path: dominant class selected -> Panel 3 (Retry) or Panel 5 (Admission)

## Panel 5: Admission Panel

Purpose: verify bounded overload behavior.

- `feedback_requests_total{outcome=accepted}`
- `feedback_requests_total{outcome=rejected}`
- `feedback_requests_total{outcome=degraded}`
- rejection ratio over time

Drill-down path: rejection growth with stable provider failures -> tune strategy profile and rerun same scenario

## POC Validation Checklist

- SLO panel renders all 4 contract indicators for any `(strategy_id, scenario_id, run_id)`
- Saturation panel shows inflight and timeout signals in the same window
- Retry panel exposes both retry volume and retry effectiveness
- Provider failure panel splits classes using T1 taxonomy
- Admission panel shows accepted/rejected/degraded trend clearly
- At least one diagnosis run for `S4` and one for `S5` is recorded in `drill-log.md`

## Deferred Items (Post-POC)

- Alert rule definitions and routing
- Alert-to-action mapping and severity model
- Escalation policy integration