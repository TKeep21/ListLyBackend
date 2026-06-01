# Listly Backend API Contract (RU)

Base URL (VM): `http://158.160.251.150:8080`

## Правила Документа (Версии + Локализация)

- Версия API-контракта в этом документе: `v1`.
- Этот файл (`docs/listly-backend-api-ru.md`) и английская локализация (`docs/API.md`) должны описывать одинаковое поведение API.
- Допустимые различия между файлами: только язык локализации.
- Любое изменение API должно обновлять оба файла в одном коммите.
- Политика ломающих изменений:
1. Предпочтительны аддитивные изменения (новые endpoint'ы/поля) без удаления текущего поведения.
2. Если ломающие изменения необходимы, создается контракт `v2` и клиенты мигрируют постепенно.

## Модель Авторизации

- Защищенные endpoint'ы требуют заголовок:

```http
Authorization: Bearer <jwt>
```

- Claims JWT, используемые backend:
- `userId`
- `role` (`USER` или `ADMIN`)

## Роли и Доступ

| Раздел | USER | ADMIN |
|---|---|---|
| `GET /media/*` | да | да |
| `GET /media/search` | да | да |
| `POST /media` | нет | да |
| `PATCH /media/admin/{mediaId}` | нет | да |
| `DELETE /media/{mediaId}` | нет | да |
| `POST /media/admin/reindex` | нет | да |
| `/user-media*` | да (только свои данные) | да |
| `/folders*` | да (только свои данные) | да |

Alias-маршруты:
- `/mediaCatalog` и `/media`
- `/user-media` и `/api/user-media` (частичные alias)
- `/folders` и `/api/folders`

## 1) Auth

### POST `/auth/register`

Создание пользователя.

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
- длина login: `3..20`
- длина password: `6..25`

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

## 2) Глобальный Каталог Медиа

Сущность, которую возвращает API:

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

Enum'ы:
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

### POST `/media` (только ADMIN)
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
- `403 Forbidden` (не админ)

### PATCH `/media/admin/{mediaId}` (только ADMIN)
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

### DELETE `/media/{mediaId}` (только ADMIN)
Alias: `DELETE /mediaCatalog/{mediaId}`

Response:
- `200 OK`

Ошибки:
- `403 Forbidden`
- `404 Not Found`

## 3) Поиск

### GET `/media/search`

Query-параметры:
- `query` (строка)
- `limit` (необязательный, по умолчанию `12`, допустимо `1..50`)
- `offset` (необязательный, по умолчанию `0`, должен быть `>= 0`)

Пример:

`GET /media/search?query=interstellar&limit=12&offset=0`

Response `200 OK`:
- `MediaItem[]`

Поведение:
- Пустой query возвращает `[]` с `200 OK`.
- Поиск работает через Meilisearch, затем медиа догружается из БД по id.

Ошибки:
- `400 Bad Request` (невалидные `limit`/`offset`)
- `503 Service Unavailable` (поиск недоступен)

## 4) Admin Reindex (Search)

### POST `/media/admin/reindex` (только ADMIN)
Alias: `POST /mediaCatalog/admin/reindex`

Response:
- `200 OK`

Ошибки:
- `403 Forbidden` (не админ)
- `503 Service Unavailable` (поиск недоступен)

## 5) Пользовательская Коллекция Медиа

Требуется JWT (`auth-jwt`).

Основные маршруты:
- `/user-media`
- Alias: `/api/user-media`

Сущность:

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

Alias: `POST /api/user-media`

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
- `400 Bad Request`
- `404 Not Found` (media не найдено)
- `409 Conflict` (уже существует)

### GET `/user-media`
Alias: `GET /api/user-media`

Необязательные query-параметры:
- `status` (`PLANNED|IN_PROGRESS|COMPLETED|DROPPED`)
- `favourite` (`true|false`)
- `folderId` (строка)
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
Обновление рейтинга/заметки.

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

## 6) Пользовательские Папки

Требуется JWT (`auth-jwt`).

Основные маршруты:
- `/folders`
- Alias: `/api/folders`

Сущность:

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

Общие ошибки для папок:
- `400 Bad Request` (невалидное имя)
- `403 Forbidden` (папка не принадлежит пользователю)
- `404 Not Found`
- `409 Conflict` (дубликат имени папки)

## Формат Ошибок Ответа

Большинство доменных/валидационных ошибок:

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
