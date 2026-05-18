# Listly Backend API (Actual)

Base URL (dev): `http://localhost:8080`

## Auth model for UI

- Most write/read user endpoints require `Authorization: Bearer <jwt>`.
- JWT now contains:
- `userId`
- `role` (`USER` or `ADMIN`)
- Admin-only endpoints return `403 Forbidden` when token role is not `ADMIN`.

Example header:

```http
Authorization: Bearer eyJhbGciOi...
```

## Roles and access

| Area | USER | ADMIN |
|---|---|---|
| `GET /media/*` | yes | yes |
| `GET /media/search` | yes | yes |
| `POST /media` | no | yes |
| `PATCH /media/admin/{mediaId}` | no | yes |
| `DELETE /media/{mediaId}` | no | yes |
| `POST /media/admin/reindex` | no | yes |
| `/user-media*` | yes (own data) | yes |
| `/folders*` | yes (own data) | yes |

Note: there are route aliases for some areas (`/mediaCatalog` and `/media`, `/user-media` and `/api/user-media`, `/folders` and `/api/folders`).

## 1) Auth

### POST `/auth/register`

Create user.

Request:

```json
{
  "login": "mike",
  "password": "strongpass123"
}
```

Response:
- `201 Created` (empty body)

Validation:
- login length: `3..20`
- password length: `6..25`

Errors:
- `400 Bad Request`
- `409 Conflict` (user already exists)

### POST `/auth/login`

Request:

```json
{
  "login": "mike",
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

## 2) Global media catalog

Entity returned by API:

```json
{
  "id": "664f...",
  "title": "Interstellar",
  "description": "Space sci-fi",
  "mediaType": "MOVIE",
  "mediaStatus": "FINISHED",
  "genres": ["Sci-Fi", "Drama"],
  "posterUrl": "https://...",
  "userRatingSum": 18.0,
  "userRatingCount": 2,
  "createdAt": 1737912345000,
  "updatedAt": 1737912399000
}
```

Enums:
- `mediaType`: `MOVIE | BOOK | SERIES | ANIME | GAME`
- `mediaStatus`: `FINISHED | ONGOING | ANNOUNCED`

### GET `/media/{mediaId}`
Alias: `GET /mediaCatalog/{mediaId}`

Response:
- `200 OK` + `MediaItem`
- `404 Not Found`

### GET `/media/items/{title}`
Alias: `GET /mediaCatalog/items/{title}`

Response:
- `200 OK` + `MediaItem[]`

### POST `/media` (ADMIN only)
Alias: `POST /mediaCatalog`

Request:

```json
{
  "title": "Interstellar",
  "description": "Space sci-fi",
  "mediaType": "MOVIE",
  "mediaStatus": "FINISHED",
  "genres": ["Sci-Fi", "Drama"],
  "posterUrl": "https://..."
}
```

Response:
- `201 Created` + created `MediaItem`

Errors:
- `400 Bad Request`
- `403 Forbidden` (not admin)

### PATCH `/media/admin/{mediaId}` (ADMIN only)
Alias: `PATCH /mediaCatalog/admin/{mediaId}`

Request (partial):

```json
{
  "title": "Interstellar (2014)",
  "mediaStatus": "FINISHED",
  "genres": ["Sci-Fi"]
}
```

Response:
- `200 OK`

Errors:
- `400 Bad Request`
- `403 Forbidden`
- `404 Not Found`

### DELETE `/media/{mediaId}` (ADMIN only)
Alias: `DELETE /mediaCatalog/{mediaId}`

Response:
- `200 OK`

Errors:
- `403 Forbidden`
- `404 Not Found`

## 3) Search

### GET `/media/search`

Query params:
- `query` (string)
- `limit` (optional, default `12`, allowed `1..50`)
- `offset` (optional, default `0`, must be `>= 0`)

Example:

`GET /media/search?query=interstellar&limit=12&offset=0`

Response `200 OK`:
- `MediaItem[]`

Important behavior for UI:
- If `query` is blank, backend returns `[]` with `200`.
- Search is backed by Meili, then media is loaded from DB by ids.

Errors:
- `400 Bad Request` (invalid `limit`/`offset`)
- `503 Service Unavailable` (search backend unavailable)

## 4) Admin reindex (search)

### POST `/media/admin/reindex` (ADMIN only)
Alias: `POST /mediaCatalog/admin/reindex`

Starts async reindex task (non-blocking HTTP request).

Responses:
- `202 Accepted`

```json
{
  "message": "Reindex started"
}
```

- `409 Conflict`

```json
{
  "error": "Reindex is already running"
}
```

- `403 Forbidden` (not admin)

## 5) User media collection

Requires JWT (`auth-jwt`).

Primary routes:
- `/user-media`
- Alias for some actions: `/api/user-media`

Entity:

```json
{
  "id": "66aa...",
  "mediaId": "65ff...",
  "collectionStatus": "PLANNED",
  "isFavourite": false,
  "folderIds": [],
  "userRating": 8.5,
  "note": "watch soon",
  "createdAt": 1737912377000,
  "updatedAt": 1737912399000
}
```

Enum `collectionStatus`:
- `PLANNED | IN_PROGRESS | COMPLETED | DROPPED`

### POST `/user-media`

Request:

```json
{
  "mediaId": "65ff...",
  "collectionStatus": "PLANNED",
  "isFavourite": false,
  "folderIds": [],
  "userRating": 8.0,
  "note": "watch this weekend"
}
```

Response:
- `201 Created`

Errors:
- `400 Bad Request` (e.g. invalid rating/note)
- `404 Not Found` (media not found)
- `409 Conflict` (already exists)

### GET `/user-media`
Alias: `GET /api/user-media`

Optional query params:
- `status` (`PLANNED|IN_PROGRESS|COMPLETED|DROPPED`)
- `favourite` (`true|false`)
- `folderId` (string)

Response:
- `200 OK` + `UserMediaResponse[]`

### GET `/user-media/{userMediaId}`

Response:
- `200 OK`
- `404 Not Found`

### PATCH `/user-media/{userMediaId}`
Update rating/note.

Request:

```json
{
  "userRating": 9.0,
  "note": "updated note"
}
```

Response:
- `200 OK`

### PATCH `/user-media/{userMediaId}/status`
Alias: `/api/user-media/{userMediaId}/status`

Request:

```json
{
  "status": "COMPLETED"
}
```

Response:
- `200 OK`

### PATCH `/user-media/{userMediaId}/favourite`
Alias: `/api/user-media/{userMediaId}/favourite`

Request:

```json
{
  "isFavourite": true
}
```

Response:
- `200 OK`

### PATCH `/user-media/{userMediaId}/folders`
Alias: `/api/user-media/{userMediaId}/folders`

Request:

```json
{
  "folderIds": ["folder1", "folder2"]
}
```

Response:
- `200 OK`

### DELETE `/user-media/{userMediaId}`

Response:
- `200 OK`
- `404 Not Found`

## 6) User folders

Requires JWT (`auth-jwt`).

Primary routes:
- `/folders`
- Alias: `/api/folders`

Entity:

```json
{
  "id": "77bb...",
  "name": "Favorites",
  "createdAt": 1737912377000,
  "updatedAt": 1737912399000
}
```

### POST `/folders`
Alias: `/api/folders`

Request:

```json
{
  "name": "Favorites"
}
```

Response:
- `201 Created` + `UserFolderResponse`

### GET `/folders`
Alias: `/api/folders`

Response:
- `200 OK` + `UserFolderResponse[]`

### PATCH `/folders/{folderId}`
Alias: `/api/folders/{folderId}`

Request:

```json
{
  "name": "New Name"
}
```

Response:
- `200 OK`

### DELETE `/folders/{folderId}`
Alias: `/api/folders/{folderId}`

Response:
- `200 OK`

Common folder errors:
- `400 Bad Request` (invalid name)
- `403 Forbidden` (folder not owned by user)
- `404 Not Found`
- `409 Conflict` (duplicate folder name)

## Error response format

Most domain/validation errors:

```json
{
  "error": "Human readable message"
}
```

Notes:
- Some search errors currently return plain string bodies (not JSON object).  
- `500` returns:

```json
{
  "error": "Internal server error"
}
```
