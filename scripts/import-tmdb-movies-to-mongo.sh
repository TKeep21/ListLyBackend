#!/usr/bin/env bash

set -euo pipefail

CSV_FILE="${1:-input/movies_metadata.csv}"
OUTPUT_FILE="${2:-build/import/global_media_items.ndjson}"
LIMIT="${3:-0}"

python3 scripts/tmdb_movies_to_media_ndjson.py \
  --input "$CSV_FILE" \
  --output "$OUTPUT_FILE" \
  --limit "$LIMIT" \
  --validate-poster-urls

bash scripts/import-media-ndjson-to-mongo.sh "$OUTPUT_FILE"
