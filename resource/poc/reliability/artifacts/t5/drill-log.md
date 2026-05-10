# T5 Drill Log (POC)

## Purpose

Capture timed operator diagnosis drills for T5 dashboard usability validation.

## Log Schema

- `drill_id`
- `run_id`
- `strategy_id`
- `scenario_id`
- `signal_start_utc`
- `first_detection_utc`
- `diagnosis_completed_utc`
- `diagnosis_time_seconds`
- `suspected_cause_class`
- `chosen_mitigation`
- `result`
- `notes`

## Entries

### Entry 1

- `drill_id`: `t5-poc-drill-001`
- `run_id`: `t4-local-s1-stub-fixed`
- `strategy_id`: `A`
- `scenario_id`: `S1`
- `signal_start_utc`: `2026-05-09T00:00:00Z` (reference run window)
- `first_detection_utc`: `2026-05-09T00:00:45Z`
- `diagnosis_completed_utc`: `2026-05-09T00:01:30Z`
- `diagnosis_time_seconds`: `45`
- `suspected_cause_class`: `none (baseline healthy)`
- `chosen_mitigation`: `no action`
- `result`: `baseline readability confirmed`
- `notes`: dashboard flow validated on baseline run; use this as control before `S4` and `S5` drills.

### Entry 2

- `drill_id`: `t5-poc-drill-002`
- `run_id`: `t5-local-s4-stub-rerun`
- `strategy_id`: `A`
- `scenario_id`: `S4`
- `signal_start_utc`: `2026-05-09T07:55:53Z` (k6 run start)
- `first_detection_utc`: `2026-05-09T07:56:35Z`
- `diagnosis_completed_utc`: `2026-05-09T07:57:40Z`
- `diagnosis_time_seconds`: `65`
- `suspected_cause_class`: `throttling_429`
- `chosen_mitigation`: `reduce offered load and switch to stricter retry/admission profile`
- `result`: `cause class identified and first mitigation selected`
- `notes`: artifacts written under `resource/poc/reliability/k6/out/t5-local-s4-stub-rerun/A/S4`; run completed with `http_req_failed` elevated (`7122/8083`, value `0.1189`) while thresholds remained below configured fail criteria.

### Entry 3

- `drill_id`: `t5-poc-drill-003`
- `run_id`: `t5-local-s5-stub-rerun`
- `strategy_id`: `A`
- `scenario_id`: `S5`
- `signal_start_utc`: `2026-05-09T08:31:33Z` (k6 run start)
- `first_detection_utc`: `2026-05-09T08:32:24Z`
- `diagnosis_completed_utc`: `2026-05-09T08:33:44Z`
- `diagnosis_time_seconds`: `80`
- `suspected_cause_class`: `provider_timeout`
- `chosen_mitigation`: `tighten timeout budget and reduce retry aggressiveness for slow-tail window`
- `result`: `slow-tail latency pattern identified and mitigation path selected`
- `notes`: artifacts written under `resource/poc/reliability/k6/out/t5-local-s5-stub-rerun/A/S5`; `http_req_waiting p99=3035.299ms` with `http_req_duration p99=3035.481ms` indicates timeout-tail style waiting inflation under scenario S5.

## Pending Required Drills

- `S4` (`429` burst): DONE (`t5-poc-drill-002`)
- `S5` (slow-provider timeout tail): DONE (`t5-poc-drill-003`)

