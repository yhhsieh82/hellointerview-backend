# Rollback guidance — feedback reliability lab profile

**Context:** After promoting a non-default lab strategy (**B**, **C**, or **D**) or tuning stub/provider retry knobs, use this checklist to revert safely. Full operator runbook is **T9**; this template captures the lab-specific minimum.

**Run / decision reference:** {{RUN_ID}} — {{RECOMMENDATION_DOC_PATH}}

## Immediate lab rollback (k6 + stub)

1. **Strategy header:** Ensure load generator sends `X-Lab-Strategy-Id: A` (or omit header; T3 defaults to **A**). See [T3 strategy switcher](../../../../prds/backend/08-feedback-reliability-strategy-switcher.md).
2. **Stub defaults:** Restore `ai.llm.stub.*` in [`application.yml`](../../../../../src/main/resources/application.yml) (or env overrides) to the known-good baseline used before the experiment branch.
3. **Provider mode:** For lab reproducibility, keep `LLM_PROVIDER=stub` unless intentionally testing real providers; document any change.

## If latency or error rate regressed after a promotion

1. Revert to strategy **A** and baseline stub `max-attempts`, backoff, and `latency-delay-millis`.
2. Re-run the failing **scenario only** (`STRATEGIES=…`, `SCENARIOS=…` on [`run-matrix.sh`](../../k6/run-matrix.sh)) with a new `RUN_ID` and compare `experiment_summary.csv`.
3. Use T5 saturation and admission panels to confirm inflight and reject rates normalized.

## Production-oriented placeholders (fill when applicable)

_(Not required for stub-only POC.)_

| Knob | Previous value | Rollback value | Owner |
|------|----------------|----------------|-------|
| Admission threshold | | | |
| Retry max attempts | | | |
| Provider routing | | | |

## Verification

- [ ] Health check passes (`/actuator/health`).
- [ ] Spot-check one feedback submit (non-lab traffic) succeeds.
- [ ] Metrics tags for the old `run_id` are no longer conflated with new traffic (use a fresh `RUN_ID` for the next campaign).
