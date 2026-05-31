#!/usr/bin/env bash

set -euo pipefail

CSV_FILE="${1:-input/anilist_anime_data_complete.csv}"
OUTPUT_FILE="${2:-build/import/anilist_anime_media_items.ndjson}"
LIMIT="${3:-5000}"

python3 scripts/anilist_anime_to_media_ndjson.py \
  --input "$CSV_FILE" \
  --output "$OUTPUT_FILE" \
  --limit "$LIMIT"

bash scripts/import-media-ndjson-to-mongo.sh "$OUTPUT_FILE"
