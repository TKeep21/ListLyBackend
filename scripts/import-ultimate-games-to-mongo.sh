#!/usr/bin/env bash

set -euo pipefail

CSV_FILE="${1:-input/Ultimate_Games_Dataset.csv}"
OUTPUT_FILE="${2:-build/import/ultimate_games_media_items.ndjson}"
LIMIT="${3:-2000}"

python3 scripts/ultimate_games_to_media_ndjson.py \
  --input "$CSV_FILE" \
  --output "$OUTPUT_FILE" \
  --limit "$LIMIT"

bash scripts/import-media-ndjson-to-mongo.sh "$OUTPUT_FILE"
