#!/usr/bin/env python3
"""
T8 heuristic scorecard: compare experiment_summary.csv rows to T1 §4.1 numeric targets.

Does not select a winning strategy — use artifacts/t8/recommendation.template.md for that.

Usage:
  evaluate-experiment-run.py --experiment-csv PATH [--status-csv PATH] --out-md PATH
"""

from __future__ import annotations

import argparse
import csv
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

# T1 §4.1 fractional / millisecond targets (POC cell windows — heuristic only).
MIN_FEEDBACK_SUCCESS_RATE = 0.99
MAX_RETRY_EXHAUSTED_RATE = 0.01
MAX_PROVIDER_P95_MS = 8000.0
MAX_PROVIDER_P99_MS = 20000.0

STRATEGY_SIMPLICITY = {"A": 0, "B": 1, "C": 2, "D": 3}


def _parse_float(raw: str) -> float | None:
    raw = (raw or "").strip()
    if not raw:
        return None
    try:
        return float(raw)
    except ValueError:
        return None


def _parse_int(raw: str) -> int:
    raw = (raw or "").strip()
    if not raw:
        return 0
    try:
        return int(round(float(raw)))
    except ValueError:
        return 0


@dataclass
class CellResult:
    run_id: str
    strategy_id: str
    scenario_id: str
    logical: int
    issues: list[str] = field(default_factory=list)
    warns: list[str] = field(default_factory=list)

    @property
    def status(self) -> str:
        if self.issues:
            return "fail"
        if self.warns:
            return "warn"
        return "pass"


def evaluate_row(row: dict[str, str]) -> CellResult:
    run_id = row.get("run_id", "").strip()
    strategy_id = row.get("strategy_id", "").strip()
    scenario_id = row.get("scenario_id", "").strip()
    logical = _parse_int(row.get("logical_submissions", ""))

    r = CellResult(run_id=run_id, strategy_id=strategy_id, scenario_id=scenario_id, logical=logical)

    if logical <= 0:
        r.warns.append("logical_submissions=0 (rates not meaningful for SLO check)")

    sr = _parse_float(row.get("feedback_success_rate", ""))
    if sr is None:
        if logical > 0:
            r.warns.append("feedback_success_rate empty")
    elif logical > 0 and sr < MIN_FEEDBACK_SUCCESS_RATE:
        r.issues.append(f"feedback_success_rate {sr:.6f} < {MIN_FEEDBACK_SUCCESS_RATE}")

    rer = _parse_float(row.get("retry_exhausted_rate", ""))
    if rer is None:
        if logical > 0:
            r.warns.append("retry_exhausted_rate empty")
    elif logical > 0 and rer > MAX_RETRY_EXHAUSTED_RATE:
        r.issues.append(f"retry_exhausted_rate {rer:.6f} > {MAX_RETRY_EXHAUSTED_RATE}")

    p95 = _parse_float(row.get("provider_path_latency_p95_ms", ""))
    if p95 is None:
        r.warns.append("provider_path_latency_p95_ms empty (k6 HTTP proxy may be missing)")
    elif p95 > MAX_PROVIDER_P95_MS:
        r.issues.append(f"provider_path_latency_p95_ms {p95:.3f} > {MAX_PROVIDER_P95_MS} (proxy)")

    p99 = _parse_float(row.get("provider_path_latency_p99_ms", ""))
    if p99 is None:
        r.warns.append("provider_path_latency_p99_ms empty (k6 HTTP proxy may be missing)")
    elif p99 > MAX_PROVIDER_P99_MS:
        r.issues.append(f"provider_path_latency_p99_ms {p99:.3f} > {MAX_PROVIDER_P99_MS} (proxy)")

    if not (row.get("success_after_retry_ratio") or "").strip():
        r.warns.append("success_after_retry_ratio not populated (expected in POC)")

    return r


def load_status_success_pairs(path: Path) -> tuple[set[tuple[str, str]], list[tuple[str, str, str]]]:
    """Returns (successful strategy,scenario pairs), and list of (s,sc,status) for failures."""
    ok: set[tuple[str, str]] = set()
    problems: list[tuple[str, str, str]] = []
    with path.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            s = (row.get("strategy_id") or "").strip()
            sc = (row.get("scenario_id") or "").strip()
            st = (row.get("status") or "").strip().lower()
            if not s or not sc:
                continue
            if st == "success":
                ok.add((s, sc))
            else:
                problems.append((s, sc, st))
    return ok, problems


def main() -> int:
    p = argparse.ArgumentParser(description="T8 experiment scorecard (heuristic)")
    p.add_argument("--experiment-csv", required=True, type=Path)
    p.add_argument("--status-csv", type=Path, default=None)
    p.add_argument("--out-md", required=True, type=Path)
    args = p.parse_args()

    exp_path: Path = args.experiment_csv
    if not exp_path.is_file():
        print(f"experiment csv not found: {exp_path}", file=sys.stderr)
        return 1

    rows: list[dict[str, str]] = []
    with exp_path.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        if not fieldnames:
            print("experiment csv has no header", file=sys.stderr)
            return 1
        for row in reader:
            rows.append(row)

    results = [evaluate_row(row) for row in rows]
    by_strategy: dict[str, list[CellResult]] = defaultdict(list)
    for res in results:
        by_strategy[res.strategy_id].append(res)

    lines: list[str] = []
    run_ids = sorted({r.run_id for r in results if r.run_id})
    lines.append("# Evaluation scorecard (automated heuristic)")
    lines.append("")
    lines.append("**Sources:** T1 §4.1 numeric targets applied per CSV row. Cell duration is **not** a rolling production window.")
    lines.append("")
    lines.append("**Latency columns** are k6 HTTP proxies from T7 unless you replace them from observability.")
    lines.append("")
    if run_ids:
        lines.append(f"**run_id:** {', '.join(run_ids)}")
    lines.append("")
    lines.append("## Per-cell results")
    lines.append("")
    lines.append("| run_id | strategy_id | scenario_id | logical_submissions | status | notes |")
    lines.append("|--------|-------------|-------------|---------------------|--------|-------|")
    for res, row in zip(results, rows):
        notes = "; ".join(res.issues + res.warns) or "—"
        lines.append(
            f"| {res.run_id} | {res.strategy_id} | {res.scenario_id} | {res.logical} | **{res.status}** | {notes} |"
        )
    lines.append("")

    lines.append("## Per-strategy summary")
    lines.append("")
    lines.append("| strategy_id | pass | warn | fail | simplicity_rank (§9.3 tie-break) |")
    lines.append("|-------------|------|------|------|-----------------------------------|")
    for sid in sorted(by_strategy.keys(), key=lambda x: (STRATEGY_SIMPLICITY.get(x, 99), x)):
        cells = by_strategy[sid]
        pc = sum(1 for c in cells if c.status == "pass")
        wc = sum(1 for c in cells if c.status == "warn")
        fc = sum(1 for c in cells if c.status == "fail")
        rank = STRATEGY_SIMPLICITY.get(sid, 99)
        lines.append(f"| {sid} | {pc} | {wc} | {fc} | {rank} (lower is simpler) |")
    lines.append("")
    lines.append("_Human step: among strategies with acceptable qualitative overload behavior (T5), prefer the lowest simplicity rank when numeric results are comparable._")
    lines.append("")

    if args.status_csv and args.status_csv.is_file():
        ok_pairs, failures = load_status_success_pairs(args.status_csv)
        exp_pairs = {(r.strategy_id, r.scenario_id) for r in results}
        lines.append("## Matrix status cross-check")
        lines.append("")
        missing = []
        for s, sc in sorted(ok_pairs):
            if (s, sc) not in exp_pairs:
                missing.append((s, sc))
        if missing:
            lines.append("**Warning:** `matrix-status.csv` marks success but no matching `(strategy_id, scenario_id)` row in experiment CSV:")
            for s, sc in missing:
                lines.append(f"- {s} / {sc}")
            lines.append("")
        extra = []
        for s, sc in sorted(exp_pairs):
            if (s, sc) not in ok_pairs:
                extra.append((s, sc))
        if extra:
            lines.append("**Note:** Experiment CSV has rows not marked success in status file (failed matrix cell or stale CSV):")
            for s, sc in extra:
                lines.append(f"- {s} / {sc}")
            lines.append("")
        if failures:
            lines.append("**Non-success cells in matrix-status.csv:**")
            for s, sc, st in failures:
                lines.append(f"- {s} / {sc}: {st}")
            lines.append("")
    elif args.status_csv:
        lines.append("## Matrix status cross-check")
        lines.append("")
        lines.append(f"_status csv not found: {args.status_csv}_")
        lines.append("")

    args.out_md.parent.mkdir(parents=True, exist_ok=True)
    args.out_md.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {args.out_md}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
