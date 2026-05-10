#!/usr/bin/env python3
"""
T7 lab collector: actuator counter snapshots (delta) + k6 HTTP latency proxy for §14.6 CSV rows.

Usage:
  collect-experiment-row.py snapshot --base-url URL --run-id R --strategy-id S --scenario-id C --out PATH
  collect-experiment-row.py append-row --before PATH --after PATH --summary PATH \\
      --window-start UTC --window-end UTC --run-id R --strategy-id S --scenario-id C \\
      --output-csv PATH --headers PATH [--lab-env E] [--lab-provider P] [--lab-model M]
  collect-experiment-row.py render --template PATH --out PATH --run-dir PATH \\
      [--manifest PATH] [--experiment-csv PATH] [--status-csv PATH] \\
      [--lab-env E] [--lab-provider P] [--lab-model M]
"""

from __future__ import annotations

import argparse
import csv
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

DEFAULT_CLASSIFICATION_VERSION = "T1-14.6-2026-05-10"


def _http_get_json(url: str, timeout: float = 30.0) -> dict[str, Any]:
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _metric_names(base_url: str) -> list[str]:
    try:
        data = _http_get_json(f"{base_url.rstrip('/')}/actuator/metrics")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return []
        raise
    return list(data.get("names", []))


def _resolve_meter_name(base_url: str, fragments: tuple[str, ...]) -> str | None:
    names = _metric_names(base_url)
    for name in names:
        lower = name.lower()
        if all(f.lower() in lower for f in fragments):
            return name
    return None


def _resolve_meter_preferred(base_url: str, exact_candidates: tuple[str, ...], fragments: tuple[str, ...]) -> str | None:
    names = _metric_names(base_url)
    name_set = set(names)
    for candidate in exact_candidates:
        if candidate in name_set:
            return candidate
    return _resolve_meter_name(base_url, fragments)


def _tag_query(tags: dict[str, str]) -> str:
    """Build Spring Boot 3 actuator tag query: tag=key:value (value segment quoted when needed)."""
    parts: list[str] = []
    for k, v in sorted(tags.items()):
        enc_v = urllib.parse.quote(str(v), safe="")
        parts.append(f"tag={k}:{enc_v}")
    return "&".join(parts)


def _counter_value(base_url: str, meter: str, tags: dict[str, str]) -> float:
    """Single-series counter value; returns 0 if missing or non-counter."""
    qp = _tag_query(tags)
    path = urllib.parse.quote(meter, safe="")
    url = f"{base_url.rstrip('/')}/actuator/metrics/{path}"
    if qp:
        url = f"{url}?{qp}"
    try:
        data = _http_get_json(url)
    except urllib.error.HTTPError as e:
        if e.code in (404, 400):
            return 0.0
        raise
    total = 0.0
    for m in data.get("measurements", []):
        stat = m.get("statistic", "")
        if stat in ("COUNT", "TOTAL"):
            total += float(m.get("value", 0.0))
    if total == 0.0:
        for m in data.get("measurements", []):
            total += float(m.get("value", 0.0))
    return total


def collect_lab_metrics(base_url: str, run_id: str, strategy_id: str, scenario_id: str) -> dict[str, Any]:
    base_tags = {"run_id": run_id, "strategy_id": strategy_id, "scenario_id": scenario_id}

    fb_name = _resolve_meter_preferred(
        base_url,
        ("feedback_requests_total", "feedback.requests.total"),
        ("feedback", "requests", "total"),
    )
    calls_name = _resolve_meter_preferred(
        base_url,
        ("llm_provider_calls_total", "llm.provider.calls.total"),
        ("llm", "provider", "calls", "total"),
    )
    retry_name = _resolve_meter_preferred(
        base_url,
        ("llm_provider_retry_outcome_total", "llm.provider.retry.outcome.total"),
        ("llm", "provider", "retry", "outcome", "total"),
    )

    out: dict[str, Any] = {"_resolved": {"feedback_requests": fb_name, "llm_calls": calls_name, "retry_outcome": retry_name}}

    feedback: dict[str, float] = {}
    if fb_name:
        for outcome in ("accepted", "success", "rejected", "degraded"):
            tags = dict(base_tags)
            tags["outcome"] = outcome
            feedback[outcome] = _counter_value(base_url, fb_name, tags)
    out["feedback_requests_total"] = feedback

    if calls_name:
        out["llm_provider_calls_total"] = _counter_value(base_url, calls_name, base_tags)
    else:
        out["llm_provider_calls_total"] = 0.0

    retry_out: dict[str, float] = {}
    if retry_name:
        for outcome in ("exhausted", "success", "retry", "aborted"):
            tags = dict(base_tags)
            tags["outcome"] = outcome
            v = _counter_value(base_url, retry_name, tags)
            if v:
                retry_out[outcome] = v
    out["llm_provider_retry_outcome_total"] = retry_out

    return out


def _delta_feedback(after: dict[str, float], before: dict[str, float]) -> dict[str, float]:
    keys = set(after) | set(before)
    return {k: max(0.0, float(after.get(k, 0.0)) - float(before.get(k, 0.0))) for k in keys}


def _delta_float(after: float, before: float) -> float:
    return max(0.0, float(after) - float(before))


def _delta_retry(after: dict[str, float], before: dict[str, float]) -> dict[str, float]:
    keys = set(after) | set(before)
    return {k: max(0.0, float(after.get(k, 0.0)) - float(before.get(k, 0.0))) for k in keys}


def compute_row(
    before: dict[str, Any],
    after: dict[str, Any],
    summary_path: Path,
    window_start: str,
    window_end: str,
    run_id: str,
    strategy_id: str,
    scenario_id: str,
    classification_version: str,
) -> dict[str, str]:
    fb0 = before.get("feedback_requests_total") or {}
    fb1 = after.get("feedback_requests_total") or {}
    if not isinstance(fb0, dict):
        fb0 = {}
    if not isinstance(fb1, dict):
        fb1 = {}
    d_fb = _delta_feedback(fb1, fb0)

    logical = int(round(d_fb.get("accepted", 0.0)))
    successful = int(round(d_fb.get("success", 0.0)))

    prov_before = float(before.get("llm_provider_calls_total") or 0.0)
    prov_after = float(after.get("llm_provider_calls_total") or 0.0)
    provider_attempts = int(round(_delta_float(prov_after, prov_before)))

    r0 = before.get("llm_provider_retry_outcome_total") or {}
    r1 = after.get("llm_provider_retry_outcome_total") or {}
    if not isinstance(r0, dict):
        r0 = {}
    if not isinstance(r1, dict):
        r1 = {}
    d_retry = _delta_retry(r1, r0)
    exhausted = int(round(d_retry.get("exhausted", 0.0)))

    def rate(num: float, den: int) -> str:
        if den <= 0:
            return ""
        return f"{(num / den):.6f}"

    success_rate = rate(float(successful), logical)
    retry_exhausted_rate = rate(float(exhausted), logical)

    pcs = ""
    if successful > 0:
        pcs = f"{(provider_attempts / successful):.6f}"

    p95_ms = ""
    p99_ms = ""
    if summary_path.is_file():
        try:
            with summary_path.open(encoding="utf-8") as f:
                summary = json.load(f)
            hrd = (summary.get("metrics") or {}).get("http_req_duration") or {}
            p95 = hrd.get("p(95)")
            p99 = hrd.get("p(99)")
            if p95 is not None:
                p95_ms = f"{float(p95):.3f}"
            if p99 is not None:
                p99_ms = f"{float(p99):.3f}"
        except (OSError, json.JSONDecodeError, TypeError, ValueError):
            pass

    return {
        "run_id": run_id,
        "strategy_id": strategy_id,
        "scenario_id": scenario_id,
        "window_start_utc": window_start,
        "window_end_utc": window_end,
        "logical_submissions": str(logical),
        "successful_feedbacks": str(successful),
        "provider_attempts": str(provider_attempts),
        "exhausted_submissions": str(exhausted),
        "feedback_success_rate": success_rate,
        "provider_path_latency_p95_ms": p95_ms,
        "provider_path_latency_p99_ms": p99_ms,
        "retry_exhausted_rate": retry_exhausted_rate,
        "success_after_retry_ratio": "",
        "provider_calls_per_success": pcs,
        "failure_class": "AGGREGATE",
        "classification_version": classification_version,
    }


def cmd_snapshot(args: argparse.Namespace) -> int:
    data = collect_lab_metrics(args.base_url, args.run_id, args.strategy_id, args.scenario_id)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    return 0


def cmd_append_row(args: argparse.Namespace) -> int:
    with Path(args.before).open(encoding="utf-8") as f:
        before = json.load(f)
    with Path(args.after).open(encoding="utf-8") as f:
        after = json.load(f)

    row = compute_row(
        before,
        after,
        Path(args.summary),
        args.window_start,
        args.window_end,
        args.run_id,
        args.strategy_id,
        args.scenario_id,
        args.classification_version,
    )

    out_csv = Path(args.output_csv)
    out_csv.parent.mkdir(parents=True, exist_ok=True)
    headers_path = Path(args.headers)
    with headers_path.open(encoding="utf-8") as f:
        header_line = f.readline().strip("\n\r")
    fieldnames = next(csv.reader([header_line]))

    write_header = not out_csv.exists() or out_csv.stat().st_size == 0
    with out_csv.open("a", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        if write_header:
            w.writeheader()
        w.writerow({k: row.get(k, "") for k in fieldnames})
    return 0


def cmd_render(args: argparse.Namespace) -> int:
    template = Path(args.template).read_text(encoding="utf-8")
    run_dir = Path(args.run_dir).resolve()
    run_id = args.run_id
    manifest_path = Path(args.manifest) if args.manifest else run_dir / "matrix-manifest.json"
    exp_csv = Path(args.experiment_csv) if args.experiment_csv else run_dir / "experiment_summary.csv"
    status_csv = Path(args.status_csv) if args.status_csv else run_dir / "matrix-status.csv"

    repl = {
        "{{RUN_ID}}": run_id,
        "{{CLASSIFICATION_VERSION}}": args.classification_version,
        "{{RUN_DIR}}": str(run_dir),
        "{{MATRIX_MANIFEST_PATH}}": str(manifest_path) if manifest_path.is_file() else "(not found)",
        "{{EXPERIMENT_SUMMARY_CSV}}": str(exp_csv) if exp_csv.is_file() else str(exp_csv),
        "{{MATRIX_STATUS_CSV}}": str(status_csv) if status_csv.is_file() else str(status_csv),
        "{{LAB_ENV}}": args.lab_env,
        "{{LAB_PROVIDER}}": args.lab_provider,
        "{{LAB_MODEL}}": args.lab_model,
    }
    for k, v in repl.items():
        template = template.replace(k, v)
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    Path(args.out).write_text(template, encoding="utf-8")
    return 0


def main() -> int:
    p = argparse.ArgumentParser(description="T7 experiment row collector")
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("snapshot", help="Write Micrometer lab snapshot JSON")
    s.add_argument("--base-url", required=True)
    s.add_argument("--run-id", required=True)
    s.add_argument("--strategy-id", required=True)
    s.add_argument("--scenario-id", required=True)
    s.add_argument("--out", required=True)
    s.set_defaults(func=cmd_snapshot)

    a = sub.add_parser("append-row", help="Append one §14.6 CSV row")
    a.add_argument("--before", required=True)
    a.add_argument("--after", required=True)
    a.add_argument("--summary", required=True)
    a.add_argument("--window-start", required=True)
    a.add_argument("--window-end", required=True)
    a.add_argument("--run-id", required=True)
    a.add_argument("--strategy-id", required=True)
    a.add_argument("--scenario-id", required=True)
    a.add_argument("--output-csv", required=True)
    a.add_argument("--headers", required=True)
    a.add_argument(
        "--classification-version",
        default=DEFAULT_CLASSIFICATION_VERSION,
    )
    a.set_defaults(func=cmd_append_row)

    r = sub.add_parser("render", help="Fill experiment_summary markdown template")
    r.add_argument("--template", required=True)
    r.add_argument("--out", required=True)
    r.add_argument("--run-dir", required=True)
    r.add_argument("--run-id", required=True)
    r.add_argument("--manifest", default="")
    r.add_argument("--experiment-csv", default="")
    r.add_argument("--status-csv", default="")
    r.add_argument("--lab-env", default="")
    r.add_argument("--lab-provider", default="")
    r.add_argument("--lab-model", default="")
    r.add_argument(
        "--classification-version",
        default=DEFAULT_CLASSIFICATION_VERSION,
    )
    r.set_defaults(func=cmd_render)

    args = p.parse_args()
    return int(args.func(args))


if __name__ == "__main__":
    raise SystemExit(main())
