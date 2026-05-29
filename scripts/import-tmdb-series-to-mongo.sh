#!/usr/bin/env bash

set -euo pipefail

CSV_FILE="${1:-input/TMDB_tv_dataset_v3.csv}"
OUTPUT_FILE="${2:-build/import/tmdb_series_media_items.ndjson}"
LIMIT="${3:-5000}"

python3 scripts/tmdb_series_to_media_ndjson.py \
  --input "$CSV_FILE" \
  --output "$OUTPUT_FILE" \
  --limit "$LIMIT"

bash scripts/import-media-ndjson-to-mongo.sh "$OUTPUT_FILE"
