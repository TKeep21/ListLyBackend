#!/usr/bin/env bash

set -euo pipefail

# Usage:
#   ./scripts/seed-usermedia-from-global.sh <login> [count]
# Example:
#   ./scripts/seed-usermedia-from-global.sh mike 5

LOGIN="${1:-}"
COUNT="${2:-5}"

if [[ -z "$LOGIN" ]]; then
  echo "Usage: $0 <login> [count]"
  exit 1
fi

if ! [[ "$COUNT" =~ ^[0-9]+$ ]] || [[ "$COUNT" -lt 1 ]]; then
  echo "Error: count must be a positive integer"
  exit 1
fi

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

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker is required"
  exit 1
fi

if ! command -v docker compose >/dev/null 2>&1; then
  echo "Error: docker compose is required"
  exit 1
fi

echo "Seeding up to $COUNT user-media items for login='$LOGIN'..."

docker compose exec -T mongo mongosh \
  --username "$MONGO_ROOT_USERNAME" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --eval "
const login = '$LOGIN';
const count = Number('$COUNT');
const dbx = db.getSiblingDB('ListlyDB');
const userColName = dbx.getCollectionNames().includes('user') ? 'user' : 'users';
const userCol = dbx.getCollection(userColName);
const user = userCol.findOne({ login: login });

if (!user) {
  print('ERROR: user not found: ' + login);
  quit(2);
}

const globalCol = dbx.getCollection('globalMediaItems');
const userMediaCol = dbx.getCollection('userMediaItems');

const globals = globalCol.find({ id: { \$exists: true, \$type: 'string' } }, { projection: { id: 1, title: 1 } }).limit(count).toArray();
if (globals.length === 0) {
  print('ERROR: no global media with field id found in globalMediaItems');
  quit(3);
}

const statuses = ['PLANNED', 'IN_PROGRESS', 'COMPLETED'];
let created = 0;
let skipped = 0;

for (let i = 0; i < globals.length; i++) {
  const g = globals[i];
  const exists = userMediaCol.findOne({ userId: user._id.toString(), mediaId: g.id });
  if (exists) {
    skipped++;
    continue;
  }

  const now = Date.now();
  const doc = {
    id: new ObjectId().toString(),
    userId: user._id.toString(),
    mediaId: g.id,
    collectionStatus: statuses[i % statuses.length],
    isFavourite: i % 2 === 0,
    folderIds: [],
    userRating: null,
    note: 'seeded for tests',
    createdAt: now,
    updatedAt: now
  };

  userMediaCol.insertOne(doc);
  created++;
  print('[CREATED] ' + g.title + ' -> mediaId=' + g.id);
}

print('DONE: created=' + created + ', skipped=' + skipped + ', checked=' + globals.length);
" 
