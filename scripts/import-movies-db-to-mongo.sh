#!/usr/bin/env bash

set -euo pipefail

CSV_FILE="${1:-input/movies_DB.csv}"
OUTPUT_FILE="${2:-build/import/movies_db_media_items.ndjson}"
LIMIT="${3:-5000}"

python3 scripts/movies_db_to_media_ndjson.py \
  --input "$CSV_FILE" \
  --output "$OUTPUT_FILE" \
  --limit "$LIMIT"

bash scripts/import-media-ndjson-to-mongo.sh "$OUTPUT_FILE"
