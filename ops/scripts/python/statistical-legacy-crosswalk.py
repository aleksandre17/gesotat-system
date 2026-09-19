#!/usr/bin/env python3
"""Builds the machine-readable crosswalk from the legacy statistical metric registry to versioned measure
references of the common statistical contract (decision register Q46).

Input : a pipe-separated extract of the legacy registry, one metric per line (read-only query, see --help):
        metric_code|measure_code|unit_code|quantity_kind|scale_factor|denominator_text|aggregation|status|
        inference_method|inference_confidence|source_resource_key
Output: JSON on stdout. Nothing is written to any database; every row is a PROPOSAL for the steward.

Rules (none of them decides semantics):
  * identity   : the legacy code keeps its meaning-bearing part; the site prefix is dropped from the code and
                 becomes the namespace, so the target is measure:<NS>:<CODE>(1.0.0).
  * equivalence: never inferred from a name. Two legacy metrics are never merged here (mappingKind is always 1:1).
  * evidence   : the legacy unit was itself inferred from resource titles; a confidence below the threshold, a
                 ratio without a typed denominator, or a scaled unit blocks automatic registration.
"""
import argparse
import json
import sys
from collections import Counter

AUTO_CONFIDENCE = 0.99


def target_code(metric_code: str, site_prefix: str) -> str:
    return metric_code[len(site_prefix):] if metric_code.startswith(site_prefix) else metric_code


def row_to_entry(parts, namespace, site_prefix):
    (metric, measure, unit, kind, scale, denominator, aggregation, status, method, confidence, resource) = (parts + [""] * 11)[:11]
    confidence = float(confidence or 0)
    scale = float(scale or 0)
    blockers = []
    if confidence < AUTO_CONFIDENCE:
        blockers.append("INFERRED_UNIT_BELOW_CONFIDENCE")
    if kind == "RATIO":
        blockers.append("RATIO_NEEDS_TYPED_DENOMINATOR_AND_BASE")          # Q23: free text cannot define a ratio
    if scale not in (0.0, 1.0):
        blockers.append("SCALED_UNIT_NEEDS_UNIT_MULT_DECISION")            # multiplier belongs to the unit, not the value
    if not unit:
        blockers.append("NO_UNIT_EVIDENCE")
    return {
        "legacy": {"metricCode": metric, "measureCode": measure, "unitCode": unit or None, "quantityKind": kind or None,
                   "scaleFactor": scale, "denominatorText": denominator or None, "aggregation": aggregation,
                   "status": status, "sourceResourceKey": resource or None,
                   "unitInference": {"method": method or None, "confidence": confidence}},
        "target": {"measureRef": f"measure:{namespace}:{target_code(metric, site_prefix)}(1.0.0)",
                   "conceptRef": f"concept:{namespace}:{target_code(metric, site_prefix)}(1.0.0)",
                   "unitRef": f"unit:{namespace}:{unit}(1.0.0)" if unit else None,
                   "numericEnvelope": {"precision": 28, "scale": 10, "note": "canonical envelope; the steward narrows it per measure"}},
        "mappingKind": "1:1",
        "decision": "PROPOSED",
        "automaticRegistration": "BLOCKED" if blockers else "ALLOWED_AFTER_STEWARD_SIGN_OFF",
        "blockers": blockers,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("extract", help="pipe-separated legacy registry extract")
    parser.add_argument("--namespace", required=True, help="target namespace of the proposed references")
    parser.add_argument("--site-prefix", default="", help="prefix dropped from legacy metric codes")
    parser.add_argument("--source", default="", help="free-text description of where the extract came from")
    args = parser.parse_args()

    entries = []
    with open(args.extract, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or "|" not in line:
                continue
            entries.append(row_to_entry([p.strip() for p in line.split("|")], args.namespace, args.site_prefix))
    entries.sort(key=lambda e: e["legacy"]["metricCode"])

    codes = Counter(e["target"]["measureRef"] for e in entries)
    collisions = sorted(ref for ref, n in codes.items() if n > 1)
    blockers = Counter(b for e in entries for b in e["blockers"])
    json.dump({
        "schema": "geostat.statistical-legacy-crosswalk.v1",
        "source": args.source,
        "rules": "identity by code, never by name similarity; no merge; blocked rows need a steward decision before any registration",
        "summary": {"legacyMetrics": len(entries), "distinctLegacyMeasures": len({e["legacy"]["measureCode"] for e in entries}),
                    "allowedAfterSignOff": sum(1 for e in entries if not e["blockers"]),
                    "blocked": sum(1 for e in entries if e["blockers"]), "blockers": dict(sorted(blockers.items())),
                    "targetCollisions": collisions},
        "entries": entries,
    }, sys.stdout, ensure_ascii=False, indent=2)
    sys.stdout.write("\n")
    return 1 if collisions else 0


if __name__ == "__main__":
    sys.exit(main())
