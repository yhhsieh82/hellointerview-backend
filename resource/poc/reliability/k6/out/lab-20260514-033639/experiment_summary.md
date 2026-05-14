# Feedback reliability lab — experiment summary

**Run ID:** lab-20260514-033639  
**Contract / taxonomy version:** T1-14.6-2026-05-10  
**Artifacts root:** `/Users/nathan.hsieh/IdeaProjects/hellointerview-backend/resource/poc/reliability/k6/out/lab-20260514-033639`  
**Matrix manifest:** `/Users/nathan.hsieh/IdeaProjects/hellointerview-backend/resource/poc/reliability/k6/out/lab-20260514-033639/matrix-manifest.json`  
**Experiment CSV:** `/Users/nathan.hsieh/IdeaProjects/hellointerview-backend/resource/poc/reliability/k6/out/lab-20260514-033639/experiment_summary.csv`  
**Matrix status:** `/Users/nathan.hsieh/IdeaProjects/hellointerview-backend/resource/poc/reliability/k6/out/lab-20260514-033639/matrix-status.csv`

## Environment tags (for cross-run comparison)


| Tag      | Value            |
| -------- | ---------------- |
| env      | local      |
| provider | stub |
| model    |     |


## Section 9 — Evaluation (fill per run)

### 9.1 Primary criteria

- **SLO preservation under burst:** (notes, link to dashboard / query)
- **Bounded overload behavior:** (inflight, 429 rate, admission rejects)
- **Diagnosis clarity:** (time to root cause; which panel was decisive)
- **Cost proxy:** compare `provider_calls_per_success` across strategies for the same scenario

### 9.2 Secondary criteria

- **Configuration simplicity:**
- **Rollback safety:**
- **Queue/fairness compatibility:**

## Section 10 — Deliverables checklist

- `experiment_summary.csv` for this `run_id` (all planned cells)
- Dashboard snapshot set (paths below)
- Operator observations and diagnosis time

### Dashboard snapshot paths

*(Paste paths or URLs.)*

### Operator notes

*(Free text.)*

---

**POC notice:** Latency columns in `experiment_summary.csv` are k6 HTTP percentiles unless overwritten from your observability stack. Do not treat them as the finalized T1 `provider_path_latency_`* SLI without backend histogram or TSDB queries.