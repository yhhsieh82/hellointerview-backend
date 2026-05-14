# Reliability lab — recommended default strategy

**Run ID:** `{{RUN_ID}}`  
**Evaluation date (UTC):** {{EVAL_DATE_UTC}}  
**Evaluator(s):** {{EVALUATORS}}  
**Classification / contract version:** {{CLASSIFICATION_VERSION}}

## Artifacts reviewed

- `experiment_summary.csv`: {{EXPERIMENT_SUMMARY_CSV_PATH}}
- `evaluation_scorecard.md` (if generated): {{SCORECARD_PATH}}
- `matrix-status.csv`: {{MATRIX_STATUS_PATH}}
- Dashboard snapshots / queries: {{DASHBOARD_EVIDENCE_PATHS}}
- Drill log entries: {{DRILL_LOG_REFERENCES}}

## Quantitative summary

_(Paste key rows from the scorecard or summarize per-strategy breach counts.)_

| strategy_id | Notes (SLO / cost / worst scenario) |
|-------------|--------------------------------------|
| A | |
| B | |
| C | |
| D | |

## Qualitative overload and operations

_(Bounded inflight, admission behavior, diagnosis time — cite T5 panels.)_

## Recommendation

**Default strategy for subsequent lab / pre-production profile:** **{{CHOSEN_STRATEGY_ID}}**

**Rationale (tie to §9.1):**

- SLO outcomes:
- Overload behavior:
- Cost proxy (`provider_calls_per_success`):
- Simplicity (§9.3):

**Runner-up:** {{RUNNER_UP_STRATEGY_ID}} — {{RUNNER_UP_REASON}}

**Explicit non-go:**

| strategy_id | Reason |
|-------------|--------|
| | |

**Scenarios where the chosen strategy is weakest:** {{WEAK_SCENARIOS}}

## Follow-ups before production lock-in

- [ ] Confirm provider-path percentiles from observability (not k6-only).
- [ ] Re-run matrix after config change with same `run_id` convention or new baseline.
- [ ] T9 runbook updated with emergency knobs (separate task).
