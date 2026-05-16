#!/usr/bin/env bash

set -euo pipefail

INPUT_FILE="${1:-build/import/global_media_items.ndjson}"

if [[ ! -f .env ]]; then
  echo "Error: .env not found in project root"
  exit 1
fi

# shellcheck disable=SC1091
source .env

if [[ -z "${MONGO_ROOT_USERNAME:-}" || -z "${MONGO_ROOT_PASSWORD:-}" ]]; then
  echo "Error: MONGO_ROOT_USERNAME/MONGO_ROOT_PASSWORD are not set in .env"
  exit 1
fi

if [[ ! -f "$INPUT_FILE" ]]; then
  echo "Error: input NDJSON not found: $INPUT_FILE"
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker is required"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Error: docker daemon is not running"
  exit 1
fi

if ! docker compose ps -q mongo >/dev/null 2>&1; then
  echo "Error: docker compose project is not available"
  exit 1
fi

CONTAINER_ID="$(docker compose ps -q mongo)"
if [[ -z "$CONTAINER_ID" ]]; then
  echo "Error: mongo container is not running. Start it with: docker compose up -d mongo"
  exit 1
fi

REMOTE_FILE="/tmp/listly_global_media_items.ndjson"

echo "Copying '$INPUT_FILE' -> mongo:$REMOTE_FILE"
docker cp "$INPUT_FILE" "$CONTAINER_ID:$REMOTE_FILE"

echo "Importing into ListlyDB.globalMediaItems (upsert by 'id')"
docker compose exec -T mongo mongoimport \
  --username "$MONGO_ROOT_USERNAME" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --db ListlyDB \
  --collection globalMediaItems \
  --file "$REMOTE_FILE" \
  --type json \
  --mode upsert \
  --upsertFields id

echo "Done"
