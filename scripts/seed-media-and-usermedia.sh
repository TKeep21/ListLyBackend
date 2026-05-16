#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
LOGIN="${2:-test_admin}"
PASSWORD="${3:-test_admin_123}"

require_bin() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: '$1' is required"
    exit 1
  fi
}

require_bin curl
require_bin jq

echo "[1/4] Login as '$LOGIN' on $BASE_URL"
TOKEN=$(curl -sS "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"login\":\"$LOGIN\",\"password\":\"$PASSWORD\"}" | jq -r '.token')

if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "Failed to get token. Check credentials/user exists."
  exit 1
fi

echo "[2/4] Create global media item"
CREATE_MEDIA_RESPONSE=$(curl -sS -w "\n%{http_code}" "$BASE_URL/media" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Seed: Blade Runner 2049",
    "description":"Seeded by script",
    "mediaType":"MOVIE",
    "mediaStatus":"FINISHED",
    "genres":["Sci-Fi","Drama"]
  }')

MEDIA_BODY=$(echo "$CREATE_MEDIA_RESPONSE" | sed '$d')
MEDIA_CODE=$(echo "$CREATE_MEDIA_RESPONSE" | tail -n1)

if [[ "$MEDIA_CODE" != "201" ]]; then
  echo "Failed to create global media. HTTP $MEDIA_CODE"
  echo "$MEDIA_BODY"
  exit 1
fi

MEDIA_ID=$(echo "$MEDIA_BODY" | jq -r '.id')
if [[ -z "$MEDIA_ID" || "$MEDIA_ID" == "null" ]]; then
  echo "Failed to parse media id"
  echo "$MEDIA_BODY"
  exit 1
fi

echo "Created global media id: $MEDIA_ID"

echo "[3/4] Create userMedia for current user"
CREATE_USER_MEDIA_RESPONSE=$(curl -sS -w "\n%{http_code}" "$BASE_URL/user-media" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"mediaId\":\"$MEDIA_ID\",
    \"collectionStatus\":\"PLANNED\",
    \"isFavourite\":true,
    \"userRating\":8.5,
    \"note\":\"Seeded userMedia item\"
  }")

USER_MEDIA_BODY=$(echo "$CREATE_USER_MEDIA_RESPONSE" | sed '$d')
USER_MEDIA_CODE=$(echo "$CREATE_USER_MEDIA_RESPONSE" | tail -n1)

if [[ "$USER_MEDIA_CODE" != "201" ]]; then
  echo "Failed to create userMedia. HTTP $USER_MEDIA_CODE"
  echo "$USER_MEDIA_BODY"
  exit 1
fi

USER_MEDIA_ID=$(echo "$USER_MEDIA_BODY" | jq -r '.id')

echo "[4/4] Done"
echo "Global media id: $MEDIA_ID"
echo "User media id:   $USER_MEDIA_ID"
