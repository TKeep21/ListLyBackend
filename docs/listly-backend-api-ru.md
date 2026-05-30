# Listly Backend API для мобильного UI (актуально)

Base URL (prod VM): `http://158.160.251.150:8080`
Base URL (local dev): `http://localhost:8080`

Этот документ описывает фактический API-контракт backend для мобильного клиента.

## 1. Авторизация и роли

Для защищенных endpoint'ов передавай JWT:

```http
Authorization: Bearer <jwt>
```

JWT содержит:
- `userId`
- `role` (`USER` или `ADMIN`)

Модель доступа:
- `USER` имеет доступ к своему `user-media` и своим `folders`
- `ADMIN` дополнительно имеет доступ к изменению глобального каталога и `reindex`

## 2. Карта endpoint'ов для UI

Публичные:
- `POST /auth/register`
- `POST /auth/login`
- `GET /media/{mediaId}`
- `GET /media/items/{title}`
- `GET /media/search`
- `GET /media/discover`
- `GET /health`

Защищенные (JWT):
- `GET/POST/PATCH/DELETE /user-media...`
- `GET/POST/PATCH/DELETE /folders...`

Админские (JWT + роль `ADMIN`):
- `POST /media`
- `PATCH /media/admin/{mediaId}`
- `DELETE /media/{mediaId}`
- `POST /media/admin/reindex`

Alias-маршруты:
- `/media` и `/mediaCatalog`
- `/user-media` и `/api/user-media` (частично)
- `/folders` и `/api/folders`

## 3. Auth

### POST `/auth/register`

Request:
```json
{
  "login": "mike",
  "password": "strongpass123"
}
```

Response:
- `201 Created` (пустое тело)

Валидация:
- `login`: длина `3..20`
- `password`: длина `6..25`

Ошибки:
- `400 Bad Request`
- `409 Conflict` (пользователь уже существует)

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

Ошибки:
- `400 Bad Request`
- `401 Unauthorized`

## 4. Глобальный каталог

`MediaItem` в ответах:
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

Enum `mediaType`:
- `MOVIE | BOOK | SERIES | ANIME | GAME`

Enum `mediaStatus`:
- `FINISHED | ONGOING | ANNOUNCED`

### GET `/media/{mediaId}`
Alias: `GET /mediaCatalog/{mediaId}`

Response:
- `200 OK` + `MediaItem`
- `404 Not Found`

### GET `/media/items/{title}`
Alias: `GET /mediaCatalog/items/{title}`

Response:
- `200 OK` + `MediaItem[]`

### GET `/media/discover`
Alias: `GET /mediaCatalog/discover`

Назначение:
- стартовая выдача для вкладки поиска в мобильном UI

Логика сортировки:
- `createdAt DESC` (сначала самые новые)

Query-параметры:
- `limit` (optional, default `12`, допустимо `1..50`)
- `offset` (optional, default `0`, `>= 0`)

Пример:
```http
GET /media/discover?limit=12&offset=0
```

Response:
- `200 OK` + `MediaItem[]`

Ошибки:
- `400 Bad Request` (невалидные параметры, включая нечисловые)

### POST `/media` (ADMIN)
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
- `201 Created` + созданный `MediaItem`

Ошибки:
- `400 Bad Request`
- `403 Forbidden`

### PATCH `/media/admin/{mediaId}` (ADMIN)
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

Ошибки:
- `400 Bad Request`
- `403 Forbidden`
- `404 Not Found`

### DELETE `/media/{mediaId}` (ADMIN)
Alias: `DELETE /mediaCatalog/{mediaId}`

Response:
- `200 OK`

Ошибки:
- `403 Forbidden`
- `404 Not Found`

## 5. Поиск

### GET `/media/search`

Query-параметры:
- `query` (string)
- `limit` (optional, default `12`, допустимо `1..50`)
- `offset` (optional, default `0`, `>= 0`)

Пример:
```http
GET /media/search?query=interstellar&limit=12&offset=0
```

Response:
- `200 OK` + `MediaItem[]`

Особенности для UI:
- если `query` пустой, возвращается `[]` с `200 OK`
- поиск идет через Meili, затем элементы догружаются из MongoDB

Ошибки:
- `400 Bad Request` (невалидные `limit/offset` или `query`)
- `503 Service Unavailable` (поиск недоступен)

## 6. Админская переиндексация

### POST `/media/admin/reindex` (ADMIN)
Alias: `POST /mediaCatalog/admin/reindex`

Важно:
- в текущей реализации endpoint синхронный и возвращает `200 OK`
- `202 Accepted` и фоновые задания сейчас не используются

Response:
- `200 OK`

Ошибки:
- `403 Forbidden`
- `503 Service Unavailable` (если проблема с поисковым сервисом)

## 7. User Media

Требуется JWT.

Основные маршруты:
- `/user-media`
- alias для части операций: `/api/user-media`

`UserMediaResponse`:
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

Ошибки:
- `400 Bad Request` (невалидные поля)
- `404 Not Found` (media не найден)
- `409 Conflict` (уже есть в коллекции)

### GET `/user-media`
Alias: `GET /api/user-media`

Query-параметры:
- `status` (`PLANNED|IN_PROGRESS|COMPLETED|DROPPED`)
- `favourite` (`true|false`)
- `folderId` (string)
- `mediaType` (`MOVIE|BOOK|SERIES|ANIME|GAME`)
- `sortBy` (`createdAt|title` + синонимы)
- `sortDirection` (`asc|desc`, default `desc`)

Примеры:
```http
GET /user-media?mediaType=MOVIE&folderId=folder1&sortBy=title&sortDirection=asc
GET /user-media?sortBy=createdAt&sortDirection=desc
```

Response:
- `200 OK` + `UserMediaResponse[]`

### GET `/user-media/{userMediaId}`

Response:
- `200 OK` + `UserMediaResponse`
- `404 Not Found`

### PATCH `/user-media/{userMediaId}`
Обновление `userRating` и `note`.

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

## 8. User Folders

Требуется JWT.

Основные маршруты:
- `/folders`
- alias: `/api/folders`

`UserFolderResponse`:
```json
{
  "id": "77bb...",
  "name": "Favorites",
  "createdAt": 1737912377000,
  "updatedAt": 1737912399000
}
```

Важно для UI:
- при первом `GET /folders` backend может автоматически создать дефолтные папки:
`watched`, `watching`, `planned`, `dropped`

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

Типовые ошибки папок:
- `400 Bad Request`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

## 9. Health

### GET `/health`

Response `200 OK`:
```json
{
  "status": "UP",
  "services": {
    "api": "UP",
    "search": "UP"
  }
}
```

`search` может быть `DOWN`, при этом endpoint все равно отдает `200`.

## 10. Единый формат ошибок

Большинство доменных и валидационных ошибок:
```json
{
  "error": "Human readable message"
}
```

`500`:
```json
{
  "error": "Internal server error"
}
```

## 11. Рекомендации для мобильного UI

Для вкладки поиска:
1. При пустом запросе вызывать `GET /media/discover?limit=12&offset=0`
2. При непустом запросе вызывать `GET /media/search?query=...&limit=12&offset=0`
3. Для догрузки использовать увеличение `offset`

Для коллекции:
1. Основной экран: `GET /user-media` с фильтрами
2. Быстрые действия: `/status`, `/favourite`, `/folders`
3. Карточка заметки/рейтинга: `PATCH /user-media/{id}`

Для папок:
1. На старте профиля вызывать `GET /folders`
2. Учитывать, что дефолтные папки могут появиться автоматически
