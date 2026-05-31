#!/usr/bin/env python3

import argparse
import csv
import html
import json
import re
import sys
import time
from datetime import date
from pathlib import Path

DEFAULT_LIMIT = 5000
HTML_TAG_RE = re.compile(r"<[^>]+>")


def raise_csv_field_limit() -> None:
    max_size = sys.maxsize
    while True:
        try:
            csv.field_size_limit(max_size)
            return
        except OverflowError:
            max_size //= 10


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert AniList anime CSV into Listly media NDJSON"
    )
    parser.add_argument(
        "--input",
        default="input/anilist_anime_data_complete.csv",
        help="Path to anilist_anime_data_complete.csv",
    )
    parser.add_argument(
        "--output",
        default="build/import/anilist_anime_media_items.ndjson",
        help="Output NDJSON path",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=DEFAULT_LIMIT,
        help=f"Max number of mapped documents (0 = no limit, default = {DEFAULT_LIMIT})",
    )
    parser.add_argument(
        "--include-adult",
        action="store_true",
        help="Include rows where isAdult=True",
    )
    return parser.parse_args()


def parse_bool(value: str) -> bool:
    return (value or "").strip().lower() in {"true", "1", "yes", "y", "t"}


def parse_float(value: str) -> float:
    text = (value or "").strip()
    if not text:
        return 0.0

    try:
        return float(text)
    except ValueError:
        return 0.0


def parse_int(value: str) -> int:
    return int(parse_float(value))


def parse_json_list(raw: str) -> list:
    text = (raw or "").strip()
    if not text:
        return []

    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        return []

    return parsed if isinstance(parsed, list) else []


def parse_genres(raw: str) -> list[str]:
    seen = set()
    result: list[str] = []

    for item in parse_json_list(raw):
        if not isinstance(item, str):
            continue

        genre = item.strip()
        if not genre:
            continue

        key = genre.casefold()
        if key in seen:
            continue

        seen.add(key)
        result.append(genre)

    return result


def clean_description(raw: str) -> str | None:
    text = (raw or "").strip()
    if not text:
        return None

    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.IGNORECASE)
    text = HTML_TAG_RE.sub("", text)
    text = html.unescape(text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = text.strip()

    return text or None


def parse_start_date(row: dict[str, str]) -> date | None:
    year = parse_int(row.get("startDate_year") or "")
    if year <= 0:
        return None

    month = parse_int(row.get("startDate_month") or "") or 1
    day = parse_int(row.get("startDate_day") or "") or 1

    try:
        return date(year, month, day)
    except ValueError:
        return None


def map_status(raw_status: str, start_dt: date | None) -> str:
    status = (raw_status or "").strip().upper()

    if status == "FINISHED":
        return "FINISHED"

    if status in {"RELEASING", "HIATUS"}:
        return "ONGOING"

    if status in {"NOT_YET_RELEASED", "CANCELLED", "CANCELED"}:
        return "ANNOUNCED"

    if start_dt is not None and start_dt <= date.today():
        return "ONGOING"

    return "ANNOUNCED"


def choose_title(row: dict[str, str]) -> str:
    for field in ("title_english", "title_romaji", "title_userPreferred", "title_native"):
        title = (row.get(field) or "").strip()
        if title:
            return title

    return ""


def choose_poster_url(row: dict[str, str]) -> str | None:
    for field in ("coverImage_extraLarge", "coverImage_large", "coverImage_medium"):
        value = (row.get(field) or "").strip()
        if value:
            return value

    return None


def build_media_doc(row: dict[str, str], now_ms: int) -> tuple[dict | None, str | None]:
    raw_id = (row.get("id") or "").strip()
    if not raw_id:
        return None, "missing_id"

    if not raw_id.isdigit():
        return None, "non_numeric_id"

    title = choose_title(row)
    if not title:
        return None, "missing_title"

    start_dt = parse_start_date(row)
    media = {
        "id": f"anilist-{raw_id}",
        "title": title,
        "description": clean_description(row.get("description") or ""),
        "mediaType": "ANIME",
        "mediaStatus": map_status(row.get("status") or "", start_dt),
        "genres": parse_genres(row.get("genres") or ""),
        "posterUrl": choose_poster_url(row),
        "userRatingSum": 0.0,
        "userRatingCount": 0,
        "createdAt": now_ms,
        "updatedAt": now_ms,
    }

    return media, None


def sort_key(row: dict[str, str]) -> tuple[float, float, float, str]:
    return (
        parse_float(row.get("popularity") or ""),
        parse_float(row.get("favourites") or ""),
        parse_float(row.get("averageScore") or ""),
        row.get("id") or "",
    )


def main() -> int:
    raise_csv_field_limit()
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

    with input_path.open("r", encoding="utf-8", newline="") as in_f:
        reader = csv.DictReader(in_f)
        rows = sorted(reader, key=sort_key, reverse=True)

    with output_path.open("w", encoding="utf-8", newline="") as out_f:
        for row in rows:
            rows_total += 1

            if not args.include_adult and parse_bool(row.get("isAdult") or ""):
                skipped_adult += 1
                continue

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

    print("AniList anime mapping complete")
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
