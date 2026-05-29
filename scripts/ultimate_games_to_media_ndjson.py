#!/usr/bin/env python3

import argparse
import csv
import json
import sys
import time
from datetime import date
from pathlib import Path

DEFAULT_LIMIT = 2000


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert Ultimate_Games_Dataset.csv into Listly media NDJSON"
    )
    parser.add_argument(
        "--input",
        default="input/Ultimate_Games_Dataset.csv",
        help="Path to Ultimate_Games_Dataset.csv",
    )
    parser.add_argument(
        "--output",
        default="build/import/ultimate_games_media_items.ndjson",
        help="Output NDJSON path",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=DEFAULT_LIMIT,
        help=f"Max number of mapped documents (0 = no limit, default = {DEFAULT_LIMIT})",
    )
    return parser.parse_args()


def parse_float(value: str) -> float:
    text = (value or "").strip()
    if not text:
        return 0.0

    try:
        return float(text)
    except ValueError:
        return 0.0


def parse_release_date(raw: str) -> date | None:
    text = (raw or "").strip()
    if not text:
        return None

    try:
        return date.fromisoformat(text)
    except ValueError:
        return None


def parse_genres(raw: str) -> list[str]:
    seen = set()
    result: list[str] = []

    for item in (raw or "").split("|"):
        genre = item.strip()
        if not genre:
            continue

        key = genre.casefold()
        if key in seen:
            continue

        seen.add(key)
        result.append(genre)

    return result


def map_status(release_dt: date | None) -> str:
    if release_dt is not None and release_dt <= date.today():
        return "FINISHED"
    return "ANNOUNCED"


def build_media_doc(row: dict[str, str], now_ms: int) -> tuple[dict | None, str | None]:
    raw_id = (row.get("game_id") or "").strip()
    if not raw_id:
        return None, "missing_id"

    if not raw_id.isdigit():
        return None, "non_numeric_id"

    title = (row.get("title") or "").strip()
    if not title:
        return None, "missing_title"

    release_dt = parse_release_date(row.get("release_date") or "")
    description = (row.get("description_clean") or "").strip() or None
    poster_url = (row.get("cover_image_url") or "").strip() or None

    media = {
        "id": f"rawg-{raw_id}",
        "title": title,
        "description": description,
        "mediaType": "GAME",
        "mediaStatus": map_status(release_dt),
        "genres": parse_genres(row.get("all_genres") or ""),
        "posterUrl": poster_url,
        "userRatingSum": 0.0,
        "userRatingCount": 0,
        "createdAt": now_ms,
        "updatedAt": now_ms,
    }

    return media, None


def sort_key(row: dict[str, str]) -> tuple[float, float, float, str]:
    return (
        parse_float(row.get("popularity_score") or ""),
        parse_float(row.get("library_count") or ""),
        parse_float(row.get("ratings_count") or ""),
        row.get("game_id") or "",
    )


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
    skipped_reason: dict[str, int] = {}
    seen_ids: set[str] = set()
    duplicate_ids = 0
    now_ms = int(time.time() * 1000)

    with input_path.open("r", encoding="utf-8", newline="") as in_f:
        reader = csv.DictReader(in_f)
        rows = sorted(reader, key=sort_key, reverse=True)

    with output_path.open("w", encoding="utf-8", newline="") as out_f:
        for row in rows:
            rows_total += 1

            media, reason = build_media_doc(row, now_ms)
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

    print("Ultimate games mapping complete")
    print(f"input: {input_path}")
    print(f"output: {output_path}")
    print(f"rows_total: {rows_total}")
    print(f"mapped: {mapped}")
    print(f"duplicate_ids: {duplicate_ids}")

    if skipped_reason:
        print("skipped_by_reason:")
        for key in sorted(skipped_reason.keys()):
            print(f"  - {key}: {skipped_reason[key]}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
