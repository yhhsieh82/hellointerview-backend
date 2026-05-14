# T4 Metrics Emission Coverage Report

## Scope

Task T4 from `resource/prds/backend/06-feedback-reliability-lab.md`: complete required metrics instrumentation and verify no required metric is missing for an S1-style steady path.

## Contract Matrix (Required -> Emitted)

- end-to-end completion latency -> `feedback_e2e_completion_latency_ms` (`outcome`, `strategy_id`, `scenario_id`, `run_id`)
- stage latency (claim, provider, finalize) -> `feedback_stage_latency_ms` (`stage`, `strategy_id`, `scenario_id`, `run_id`)
- accepted/rejected/degraded counts -> `feedback_requests_total` (`outcome`, `strategy_id`, `scenario_id`, `run_id`)
- retry triggered / retry exhausted -> `llm_provider_retry_attempts_total`, `llm_provider_retry_outcome_total`
- inflight count with timeout overlay -> `llm_provider_inflight_calls`, `llm_provider_failures_total{failure_class=provider_timeout}`
- provider failures by class (`429`, `5xx`, timeout, terminal config) -> `llm_provider_failures_total{failure_class=throttling_429|provider_5xx|provider_timeout|terminal_config_auth}`
- cost proxy (provider calls per successful feedback) -> `llm_provider_calls_per_success` (distribution) + `llm_provider_calls_total`

## Coverage Validation

### Automated Verification

Executed:

`mvn -Dtest=PracticeFeedbackServiceTest,AbstractLlmFeedbackClientTest,StubLlmFeedbackClientTest test`

Result: PASS (18 tests, 0 failures, 0 errors)

Key assertions added:

- service lifecycle emits claim/provider/finalize stage latency and e2e latency on success/failure paths
- accepted/rejected/degraded outcomes are emitted via `feedback_requests_total`
- provider timeout class is emitted as `provider_timeout`
- lab headers propagate into provider metric tags (`strategy_id`, `scenario_id`, `run_id`)

### S1 Run Evidence

Executed command:

`BASE_URL=http://localhost:8000 PRACTICE_ID=1 STRATEGY_ID=A SCENARIO_ID=S1 RUN_ID=t4-local-s1 ./resource/poc/reliability/k6/run.sh`

Observed outcomes:

- k6 completed with threshold failures (`http_req_duration`, `http_req_failed`) and exited non-zero.
- Run summary artifact generated at `resource/poc/reliability/k6/out/t4-local-s1/A/S1/summary.json`.
- Request volume from summary: `http_reqs.count=33`, `http_req_failed=14`, `checks.passes=18`, `checks.fails=15`.

Actuator metric verification after run:

- `feedback_requests_total` present with tags: `run_id=t4-local-s1`, `strategy_id=A`, `scenario_id=S1`, outcomes include `accepted|rejected|success`.
- `feedback_stage_latency_ms` present with tags: `stage=claim|provider|finalize`, `run_id=t4-local-s1`, `strategy_id=A`, `scenario_id=S1`.
- `llm_provider_calls_total` present with tags: `provider=Ollama`, `model=llama3.2:1b`, `attempt=1`, outcomes include `success|failure`.
- `llm_provider_failures_total` present with `failure_class=unknown` and lab tags (`run_id`, `strategy_id`, `scenario_id`).

### S1 Rerun With Stub Provider

Backend launch command used:

`mvn spring-boot:run -Dspring-boot.run.arguments="--ai.llm.provider=stub"`

Rerun command:

`BASE_URL=http://localhost:8000 PRACTICE_ID=1 STRATEGY_ID=A SCENARIO_ID=S1 RUN_ID=t4-local-s1-stub-fixed ./resource/poc/reliability/k6/run.sh`

Observed outcomes:

- k6 exited successfully (`exit 0`) and produced full artifacts.
- Run summary artifact generated at `resource/poc/reliability/k6/out/t4-local-s1-stub-fixed/A/S1/summary.json`.
- Request volume from summary: `http_reqs.count=15210`, `checks.passes=15210`, `checks.fails=0`.
- Thresholds passed: `http_req_duration p95=24.885ms (< 20000ms)` and `http_req_failed rate=0 (< 0.5)`.

Actuator metric verification after rerun:

- `llm_provider_calls_total` shows `provider=Stub`, `model=stub-lab-v1`.
- `run_id` includes `t4-local-s1-stub-fixed`, confirming this run used the stub provider.

## Gap Status

- Missing required metrics in validated paths: none
- Remaining operational precondition: none for S1/stub baseline; keep explicit provider pin (`--ai.llm.provider=stub`) in lab execution to avoid accidental provider drift.