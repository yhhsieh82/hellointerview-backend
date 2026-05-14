# T8 evaluation rubric

Maps [PRD §9](../../../../prds/backend/06-feedback-reliability-lab.md) to observable signals. Use with `experiment_summary.csv`, T5 dashboards, and operator notes.

## Primary criteria (§9.1)

### SLO preservation under burst


| Signal                  | Source                                                             | POC check                                                                                    |
| ----------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| Success rate            | CSV `feedback_success_rate`                                        | Compare to **≥ 0.99** (fraction); low `logical_submissions` → treat as **warn** only         |
| Provider path p95 / p99 | CSV latency columns (k6 proxy) **and** T5 SLO panel (if available) | Proxy: **≤ 8000 ms** p95, **≤ 20000 ms** p99 on CSV; confirm in backend metrics if disputing |
| Retry exhaustion        | CSV `retry_exhausted_rate`                                         | **≤ 0.01** (fraction)                                                                        |
| Fault scenarios         | Scenarios S2–S5                                                    | Pay extra attention to S2 burst, S3/S4/S5 faults                                             |


### Bounded overload behavior (no runaway inflight)


| Signal              | Source                                                                      | POC check                                                                                                   |
| ------------------- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| Inflight / timeouts | T5 Saturation panel (`llm_provider_inflight_calls`, timeout class failures) | No unexplained sustained inflight growth; timeouts explainable by scenario window                           |
| Admission / rejects | T5 Admission panel (`feedback_requests_total` by outcome)                   | Rejections during overload should be **deterministic** and tied to strategy (e.g. B fail-fast), not chaotic |


### Diagnosis clarity and operational controllability


| Signal             | Source                                               | POC check                                                                                            |
| ------------------ | ---------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Time to root cause | `[drill-log.md](../t5/drill-log.md)`, operator notes | Record minutes-to-diagnosis for at least one `429` and one timeout-style scenario (T5 done criteria) |
| Panel drill path   | `[dashboard-pack.md](../t5/dashboard-pack.md)`       | SLO breach → saturation → admission / provider failures is repeatable                                |


### Acceptable cost per successful feedback


| Signal     | Source                           | POC check                                                                                  |
| ---------- | -------------------------------- | ------------------------------------------------------------------------------------------ |
| Cost proxy | CSV `provider_calls_per_success` | Compare **across strategies** for the **same** `scenario_id`; lower is better if SLOs hold |


## Secondary criteria (§9.2)


| Criterion                    | How to judge                                                                                 |
| ---------------------------- | -------------------------------------------------------------------------------------------- |
| Configuration simplicity     | Fewer moving parts (e.g. **A** baseline vs extra admission tuning)                           |
| Rollback safety              | Can revert to strategy **A** and default stub/prod knobs without data migration (see T3 doc) |
| Queue/fairness compatibility | Note constraints for a future fairness phase; no implementation required in T8               |


## Decision rule (§9.3)

Among strategies that **pass** primary checks (with qualitative overload sign-off):

1. Prefer **A** over **B** over **C** over **D** when outcomes and cost are **roughly equivalent** (simplest first).
2. If **A** fails SLO under realistic burst/fault mix but **B** or **C** restores stability with acceptable cost, choose the **simplest** of those that passes.
3. Document explicit **non-go** strategies (e.g. higher cost with no SLO gain).

## Threshold constants (T1 §4.1, numeric)


| Metric                                 | Target    |
| -------------------------------------- | --------- |
| `feedback_success_rate`                | ≥ `0.99`  |
| `retry_exhausted_rate`                 | ≤ `0.01`  |
| `provider_path_latency_p95_ms` (proxy) | ≤ `8000`  |
| `provider_path_latency_p99_ms` (proxy) | ≤ `20000` |


These are applied heuristically in `[evaluate-experiment-run.py](../../k6/evaluate-experiment-run.py)`; formal rolling-window evaluation belongs in production observability.