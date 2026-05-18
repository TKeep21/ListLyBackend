# Listly Backend API v1

Base URL (dev): `http://localhost:8080`

## Auth

### POST /auth/register
Request:
```json
{
  "login": "mahach",
  "password": "strongpass123"
}
```
Response: `201 Created`

Errors:
- `400 Bad Request` invalid body/validation
- `409 Conflict` user already exists

### POST /auth/login
Request:
```json
{
  "login": "mahach",
  "password": "strongpass123"
}
```
Response `200 OK`:
```json
{
  "token": "<jwt>"
}
```

Errors:
- `400 Bad Request`
- `401 Unauthorized`

## Global Media Catalog

Entity `MediaItem` (global, shared by all users):
```json
{
  "id": "65ff...",
  "title": "Interstellar",
  "description": "...",
  "genre": ["Sci-Fi", "Drama"],
  "type": "MOVIE",
  "mediaStatus": "FINISHED",
  "globalRating": 8.6,
  "createdAt": 1737912345000
}
```

Enums:
- `type`: `MOVIE | BOOK | SERIES | ANIME | GAME`
- `mediaStatus`: `FINISHED | ONGOING | PLANNED`

### GET /media
Returns global catalog.

Optional query params:
- `type`
- `q` (search by title)
- `limit`, `offset`

Response `200 OK`:
```json
[
  {
    "id": "65ff...",
    "title": "Interstellar",
    "description": "...",
    "genre": ["Sci-Fi", "Drama"],
    "type": "MOVIE",
    "mediaStatus": "FINISHED",
    "globalRating": 8.6,
    "createdAt": 1737912345000
  }
]
```

### GET /media/{mediaId}
Response `200 OK` with one `MediaItem`.

Errors:
- `404 Not Found`

### GET /media/discover
Returns initial media list for Search tab without query text.

Optional query params:
- `limit` (default `12`, allowed `1..50`)
- `offset` (default `0`, must be `>= 0`)

Response `200 OK`:
```json
[
  {
    "id": "65ff...",
    "title": "Interstellar"
  }
]
```

Errors:
- `400 Bad Request` invalid `limit`/`offset`

### POST /media
Auth required (JWT).

Request:
```json
{
  "title": "Interstellar",
  "description": "...",
  "genre": ["Sci-Fi", "Drama"],
  "type": "MOVIE",
  "mediaStatus": "FINISHED",
  "globalRating": 8.6
}
```

Response `201 Created` with created item.

Errors:
- `400 Bad Request`
- `409 Conflict` (duplicate global media)

## User Collection

Entity `UserMediaItem` (private user state linked to global media):
```json
{
  "id": "66aa...",
  "userId": "64bb...",
  "mediaId": "65ff...",
  "userMediaStatus": "COMPLETED",
  "userRating": 9.5,
  "note": "Amazing soundtrack",
  "createdAt": 1737912377000,
  "updatedAt": 1737912399000
}
```

Enum `userMediaStatus`:
- `PLANNED | IN_PROGRESS | COMPLETED | DROPPED`

### POST /user-media
Auth required.

Request:
```json
{
  "mediaId": "65ff...",
  "userMediaStatus": "PLANNED",
  "userRating": 8.0,
  "note": "watch this weekend"
}
```

Response: `201 Created`

Errors:
- `400 Bad Request`
- `404 Not Found` media does not exist
- `409 Conflict` already in user collection

### GET /user-media
Auth required.

Response `200 OK` (recommended response for mobile):
```json
[
  {
    "id": "66aa...",
    "media": {
      "id": "65ff...",
      "title": "Interstellar",
      "type": "MOVIE",
      "description": "...",
      "genre": ["Sci-Fi", "Drama"],
      "mediaStatus": "FINISHED",
      "globalRating": 8.6
    },
    "userMediaStatus": "COMPLETED",
    "userRating": 9.5,
    "note": "Amazing soundtrack",
    "createdAt": 1737912377000,
    "updatedAt": 1737912399000
  }
]
```

### GET /user-media/{userMediaId}
Auth required.

Response `200 OK` with one user-media item.

Errors:
- `404 Not Found`

### PATCH /user-media/{userMediaId}
Auth required.

Request (partial):
```json
{
  "userMediaStatus": "COMPLETED",
  "userRating": 9.2,
  "note": "updated note"
}
```

Response: `200 OK`

Errors:
- `400 Bad Request`
- `404 Not Found`

### DELETE /user-media/{userMediaId}
Auth required.

Response: `200 OK`

Errors:
- `404 Not Found`

## Error format

Use one stable shape for all errors:
```json
{
  "error": "Human-readable message"
}
```

## Integration checklist for Android

- Save JWT from `/auth/login` and send in header:
  - `Authorization: Bearer <token>`
- Do not hardcode enums on client without syncing this file.
- Any API change requires updating this document in the same PR.
