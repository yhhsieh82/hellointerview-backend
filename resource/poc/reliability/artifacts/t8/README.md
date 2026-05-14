# T8 — Evaluation and default selection

This pack supports [T8 in the reliability lab PRD](../../../../prds/backend/06-feedback-reliability-lab.md): compare strategy × scenario results, pick a **recommended default profile**, and capture **rollback guidance**, using T6/T7 artifacts plus T5 dashboard evidence.

## Workflow

1. **Run the matrix** (T6): [`run-matrix.sh`](../../k6/run-matrix.sh) writes `out/{run_id}/matrix-manifest.json`, `matrix-status.csv`, and per-cell k6 artifacts. T7 appends rows to `out/{run_id}/experiment_summary.csv` when actuator snapshots succeed.
2. **Validate completeness:** Open `matrix-status.csv`. Every `success` row should have a matching `(strategy_id, scenario_id)` line in `experiment_summary.csv`. Failed cells need a documented rerun or exclusion before you compare strategies.
3. **Quantitative pass/fail (heuristic):** Run the scorecard generator:

   ```bash
   python3 resource/poc/reliability/k6/evaluate-experiment-run.py \
     --experiment-csv resource/poc/reliability/k6/out/<run_id>/experiment_summary.csv \
     --status-csv resource/poc/reliability/k6/out/<run_id>/matrix-status.csv \
     --out-md resource/poc/reliability/k6/out/<run_id>/evaluation_scorecard.md
   ```

4. **Qualitative review (required for sign-off):** Use [T5 dashboard pack](../t5/dashboard-pack.md) and [drill log](../t5/drill-log.md) to judge bounded overload, admission behavior, and diagnosis time. The CSV does **not** replace saturation/admission panels.
5. **Record the decision:** Copy [`recommendation.template.md`](recommendation.template.md) and [`rollback-guidance.template.md`](rollback-guidance.template.md) into the same `out/{run_id}/` folder (or version control elsewhere), fill them in, and attach dashboard snapshot paths.

## Caveats (read before sign-off)

- **Rolling windows vs cell windows:** T1 SLOs (e.g. 30m success rate) are defined for production-like rolling windows. A single k6 cell is much shorter; treat numeric checks as **directional**, not a formal SLO certification.
- **Latency columns:** `provider_path_latency_p95_ms` / `p99_ms` in `experiment_summary.csv` come from **k6 HTTP** timing (T7), not the T1 provider-stage SLI. For contract-true percentiles, query your observability backend (T5) or Prometheus histograms.
- **Automation limits:** `evaluate-experiment-run.py` flags obvious threshold breaches; it does **not** choose the winning strategy. A human applies [§9.3](../../../../prds/backend/06-feedback-reliability-lab.md) (simplest strategy that meets the bar).

## Related docs

- Rubric: [`evaluation-rubric.md`](evaluation-rubric.md)
- Strategy semantics / knobs: [`08-feedback-reliability-strategy-switcher.md`](../../../../prds/backend/08-feedback-reliability-strategy-switcher.md) (T3)
- T7 CSV schema: [`../t7/README.md`](../t7/README.md)
