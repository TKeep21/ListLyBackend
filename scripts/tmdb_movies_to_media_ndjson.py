#!/usr/bin/env python3

import argparse
import ast
import csv
import json
import sys
import time
from datetime import date
from pathlib import Path

POSTER_BASE_DEFAULT = "https://image.tmdb.org/t/p/w500"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert TMDB movies_metadata.csv into Listly media NDJSON"
    )
    parser.add_argument(
        "--input",
        default="input/movies_metadata.csv",
        help="Path to movies_metadata.csv",
    )
    parser.add_argument(
        "--output",
        default="build/import/global_media_items.ndjson",
        help="Output NDJSON path",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=0,
        help="Max number of mapped documents (0 = no limit)",
    )
    parser.add_argument(
        "--include-adult",
        action="store_true",
        help="Include rows where adult=True",
    )
    parser.add_argument(
        "--poster-base",
        default=POSTER_BASE_DEFAULT,
        help="Base URL for poster_path",
    )
    return parser.parse_args()


def parse_bool(value: str) -> bool:
    return (value or "").strip().lower() in {"true", "1", "yes", "y", "t"}


def parse_genres(raw: str) -> list[str]:
    if not raw:
        return []

    text = raw.strip()
    if not text:
        return []

    try:
        parsed = ast.literal_eval(text)
    except (ValueError, SyntaxError):
        return []

    if not isinstance(parsed, list):
        return []

    seen = set()
    result: list[str] = []
    for item in parsed:
        if not isinstance(item, dict):
            continue

        name = item.get("name")
        if not isinstance(name, str):
            continue

        normalized = name.strip()
        if not normalized:
            continue

        key = normalized.casefold()
        if key in seen:
            continue

        seen.add(key)
        result.append(normalized)

    return result


def parse_release_date(raw: str) -> date | None:
    if not raw:
        return None

    text = raw.strip()
    if not text:
        return None

    try:
        return date.fromisoformat(text)
    except ValueError:
        return None


def map_status(raw_status: str, release_dt: date | None) -> str:
    status = (raw_status or "").strip().lower()

    if status == "released":
        return "FINISHED"

    if status in {"in production", "post production"}:
        return "ONGOING"

    if status in {"planned", "rumored", "cancelled", "canceled"}:
        return "ANNOUNCED"

    if release_dt is not None and release_dt <= date.today():
        return "FINISHED"

    return "ANNOUNCED"


def build_poster_url(poster_path: str, poster_base: str) -> str | None:
    if not poster_path:
        return None

    path = poster_path.strip()
    if not path:
        return None

    if path.startswith("http://") or path.startswith("https://"):
        return path

    if not path.startswith("/"):
        path = "/" + path

    base = poster_base.rstrip("/")
    return base + path


def build_media_doc(row: dict[str, str], now_ms: int, poster_base: str) -> tuple[dict | None, str | None]:
    raw_id = (row.get("id") or "").strip()
    if not raw_id:
        return None, "missing_id"

    if not raw_id.isdigit():
        return None, "non_numeric_id"

    title = (row.get("title") or "").strip()
    if not title:
        title = (row.get("original_title") or "").strip()
    if not title:
        return None, "missing_title"

    overview = (row.get("overview") or "").strip()
    release_dt = parse_release_date(row.get("release_date") or "")

    media = {
        "id": f"tmdb-{raw_id}",
        "title": title,
        "description": overview or None,
        "mediaType": "MOVIE",
        "mediaStatus": map_status(row.get("status") or "", release_dt),
        "genres": parse_genres(row.get("genres") or ""),
        "posterUrl": build_poster_url(row.get("poster_path") or "", poster_base),
        "userRatingSum": 0.0,
        "userRatingCount": 0,
        "createdAt": now_ms,
        "updatedAt": now_ms,
    }

    return media, None


def main() -> int:
    args = parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"Input file not found: {input_path}", file=sys.stderr)
        return 1

    output_path.parent.mkdir(parents=True, exist_ok=True)

    mapped = 0
    rows_total = 0
    skipped_adult = 0
    skipped_reason: dict[str, int] = {}
    seen_ids: set[str] = set()
    duplicate_ids = 0

    now_ms = int(time.time() * 1000)

    with input_path.open("r", encoding="utf-8", newline="") as in_f, output_path.open(
        "w", encoding="utf-8", newline=""
    ) as out_f:
        reader = csv.DictReader(in_f)

        for row in reader:
            rows_total += 1

            if not args.include_adult and parse_bool(row.get("adult") or ""):
                skipped_adult += 1
                continue

            media, reason = build_media_doc(row, now_ms, args.poster_base)
            if media is None:
                key = reason or "unknown"
                skipped_reason[key] = skipped_reason.get(key, 0) + 1
                continue

            media_id = media["id"]
            if media_id in seen_ids:
                duplicate_ids += 1
                continue

            seen_ids.add(media_id)
            out_f.write(json.dumps(media, ensure_ascii=False) + "\n")
            mapped += 1

            if args.limit > 0 and mapped >= args.limit:
                break

    print("TMDB mapping complete")
    print(f"input: {input_path}")
    print(f"output: {output_path}")
    print(f"rows_total: {rows_total}")
    print(f"mapped: {mapped}")
    print(f"skipped_adult: {skipped_adult}")
    print(f"duplicate_ids: {duplicate_ids}")

    if skipped_reason:
        print("skipped_by_reason:")
        for key in sorted(skipped_reason.keys()):
            print(f"  - {key}: {skipped_reason[key]}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
