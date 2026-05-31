# Listly Backend API Contract (EN)

Base URL (VM): `http://158.160.251.150:8080`

## Document Rules (Versioning + Localization)

- API contract version in this document: `v1`.
- This file (`docs/API.md`) and the Russian localization (`docs/listly-backend-api-ru.md`) must describe the same API behavior.
- Allowed differences between the two files: localization language only.
- Any API change must update both files in the same commit.
- Breaking changes policy:
1. Prefer additive changes (new endpoints/fields) without removing existing behavior.
2. If breaking changes are required, create `v2` contract docs and migrate clients gradually.

## Auth Model

- Protected endpoints require header:

```http
Authorization: Bearer <jwt>
```

- JWT claims used by backend:
- `userId`
- `role` (`USER` or `ADMIN`)

## Roles and Access

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

Route aliases:
- `/mediaCatalog` and `/media`
- `/user-media` and `/api/user-media` (partial aliases)
- `/folders` and `/api/folders`

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

## 2) Global Media Catalog

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
- `mediaType`: `MOVIE | SERIES | ANIME | GAME`
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

Behavior:
- Blank query returns `[]` with `200 OK`.
- Search is backed by Meilisearch, then media is loaded from DB by ids.

Errors:
- `400 Bad Request` (invalid `limit`/`offset`)
- `503 Service Unavailable` (search unavailable)

## 4) Admin Reindex (Search)

### POST `/media/admin/reindex` (ADMIN only)
Alias: `POST /mediaCatalog/admin/reindex`

Response:
- `200 OK`

Errors:
- `403 Forbidden` (not admin)
- `503 Service Unavailable` (search unavailable)

## 5) User Media Collection

Requires JWT (`auth-jwt`).

Primary routes:
- `/user-media`
- Alias for selected actions: `/api/user-media`

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
- `400 Bad Request`
- `404 Not Found` (media not found)
- `409 Conflict` (already exists)

### GET `/user-media`
Alias: `GET /api/user-media`

Optional query params:
- `status` (`PLANNED|IN_PROGRESS|COMPLETED|DROPPED`)
- `favourite` (`true|false`)
- `folderId` (string)
- `mediaType` (`MOVIE|SERIES|ANIME|GAME`)
- `sortBy` (`added_date|title`)
- `sortDir` (`asc|desc`)

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
Alias: `PATCH /api/user-media/{userMediaId}/status`

Request:

```json
{
  "status": "COMPLETED"
}
```

Response:
- `200 OK`

### PATCH `/user-media/{userMediaId}/favourite`
Alias: `PATCH /api/user-media/{userMediaId}/favourite`

Request:

```json
{
  "isFavourite": true
}
```

Response:
- `200 OK`

### PATCH `/user-media/{userMediaId}/folders`
Alias: `PATCH /api/user-media/{userMediaId}/folders`

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

## 6) User Folders

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
Alias: `POST /api/folders`

Request:

```json
{
  "name": "Favorites"
}
```

Response:
- `201 Created` + `UserFolderResponse`

### GET `/folders`
Alias: `GET /api/folders`

Response:
- `200 OK` + `UserFolderResponse[]`

### PATCH `/folders/{folderId}`
Alias: `PATCH /api/folders/{folderId}`

Request:

```json
{
  "name": "New Name"
}
```

Response:
- `200 OK`

### DELETE `/folders/{folderId}`
Alias: `DELETE /api/folders/{folderId}`

Response:
- `200 OK`

Common folder errors:
- `400 Bad Request` (invalid name)
- `403 Forbidden` (folder not owned by user)
- `404 Not Found`
- `409 Conflict` (duplicate folder name)

## Error Response Format

Most domain/validation errors:

```json
{
  "error": "Human readable message"
}
```

`500 Internal Server Error`:

```json
{
  "error": "Internal server error"
}
```
