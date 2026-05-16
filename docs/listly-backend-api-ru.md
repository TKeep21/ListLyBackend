# Listly Backend API (актуальная версия)

Base URL (dev): `http://158.160.251.150:8080`

## Модель авторизации для UI

Большинство пользовательских endpoint'ов требуют JWT-токен в заголовке:

```http
Authorization: Bearer <jwt>
```

JWT содержит:

- `userId`
- `role` (`USER` или `ADMIN`)

Если пользователь без роли `ADMIN` обращается к admin-endpoint'ам, сервер возвращает:

```http
403 Forbidden
```

---

## Роли и доступ

| Раздел | USER | ADMIN |
|---|---|---|
| `GET /media/*` | да | да |
| `GET /media/search` | да | да |
| `POST /media` | нет | да |
| `PATCH /media/admin/{mediaId}` | нет | да |
| `DELETE /media/{mediaId}` | нет | да |
| `POST /media/admin/reindex` | нет | да |
| `/user-media*` | да, только свои данные | да |
| `/folders*` | да, только свои данные | да |

Примечание: у некоторых endpoint'ов есть alias-маршруты:

- `/mediaCatalog` и `/media`
- `/user-media` и `/api/user-media`
- `/folders` и `/api/folders`

---

# 1. Авторизация

## POST `/auth/register`

Создание пользователя.

### Request

```json
{
  "login": "mike",
  "password": "strongpass123"
}
```

### Response

- `201 Created` — пустое тело ответа

### Валидация

- `login`: длина `3..20`
- `password`: длина `6..25`

### Ошибки

- `400 Bad Request`
- `409 Conflict` — пользователь уже существует

---

## POST `/auth/login`

Авторизация пользователя.

### Request

```json
{
  "login": "mike",
  "password": "strongpass123"
}
```

### Response `200 OK`

```json
{
  "token": "<jwt>"
}
```

### Ошибки

- `400 Bad Request`
- `401 Unauthorized`

---

# 2. Глобальный каталог медиа

Пример сущности, возвращаемой API:

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

## Enum'ы

### `mediaType`

- `MOVIE`
- `BOOK`
- `SERIES`
- `ANIME`
- `GAME`

### `mediaStatus`

- `FINISHED`
- `ONGOING`
- `ANNOUNCED`

---

## GET `/media/{mediaId}`

Alias: `GET /mediaCatalog/{mediaId}`

### Response

- `200 OK` + `MediaItem`
- `404 Not Found`

---

## GET `/media/items/{title}`

Alias: `GET /mediaCatalog/items/{title}`

### Response

- `200 OK` + `MediaItem[]`

---

## POST `/media` — только ADMIN

Alias: `POST /mediaCatalog`

Создание нового элемента в глобальном каталоге медиа.

### Request

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

### Response

- `201 Created` + созданный `MediaItem`

### Ошибки

- `400 Bad Request`
- `403 Forbidden` — пользователь не является администратором

---

## PATCH `/media/admin/{mediaId}` — только ADMIN

Alias: `PATCH /mediaCatalog/admin/{mediaId}`

Частичное обновление элемента глобального каталога.

### Request

```json
{
  "title": "Interstellar (2014)",
  "mediaStatus": "FINISHED",
  "genres": ["Sci-Fi"]
}
```

### Response

- `200 OK`

### Ошибки

- `400 Bad Request`
- `403 Forbidden`
- `404 Not Found`

---

## DELETE `/media/{mediaId}` — только ADMIN

Alias: `DELETE /mediaCatalog/{mediaId}`

Удаление элемента из глобального каталога.

### Response

- `200 OK`

### Ошибки

- `403 Forbidden`
- `404 Not Found`

---

# 3. Поиск

## GET `/media/search`

Поиск по глобальному каталогу медиа.

### Query параметры

- `query` — строка поиска
- `limit` — необязательный параметр, по умолчанию `12`, допустимые значения `1..50`
- `offset` — необязательный параметр, по умолчанию `0`, должен быть `>= 0`

### Пример

```http
GET /media/search?query=interstellar&limit=12&offset=0
```

### Response `200 OK`

```json
[
  {
    "...": "MediaItem"
  }
]
```

### Важное поведение для UI

Если `query` пустой, backend возвращает пустой массив с кодом `200 OK`:

```json
[]
```

Поиск работает через Meilisearch. После получения id из поискового индекса медиа догружается из базы данных.

### Ошибки

- `400 Bad Request` — некорректный `limit` или `offset`
- `503 Service Unavailable` — search backend недоступен

---

# 4. Admin reindex поиска

## POST `/media/admin/reindex` — только ADMIN

Alias: `POST /mediaCatalog/admin/reindex`

Запускает асинхронную переиндексацию поиска.

HTTP-запрос не блокируется: сервер сразу отвечает, а reindex выполняется отдельно.

### Response `202 Accepted`

```json
{
  "message": "Reindex started"
}
```

### Response `409 Conflict`

```json
{
  "error": "Reindex is already running"
}
```

### Ошибки

- `403 Forbidden` — пользователь не является администратором

---

# 5. Пользовательская коллекция медиа

Требуется JWT (`auth-jwt`).

Основные маршруты:

- `/user-media`
- `/api/user-media` — alias для некоторых действий

## Сущность

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

## Enum `collectionStatus`

- `PLANNED`
- `IN_PROGRESS`
- `COMPLETED`
- `DROPPED`

---

## POST `/user-media`

Добавление медиа в пользовательскую коллекцию.

### Request

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

### Response

- `201 Created`

### Ошибки

- `400 Bad Request` — например, невалидный рейтинг или заметка
- `404 Not Found` — media не найдено
- `409 Conflict` — запись уже существует

---

## GET `/user-media`

Alias: `GET /api/user-media`

Получение пользовательской коллекции медиа.

### Query параметры

- `status` — `PLANNED | IN_PROGRESS | COMPLETED | DROPPED`
- `favourite` — `true | false`
- `folderId` — строка

### Response

- `200 OK` + `UserMediaResponse[]`

---

## GET `/user-media/{userMediaId}`

Получение конкретной записи из пользовательской коллекции.

### Response

- `200 OK`
- `404 Not Found`

---

## PATCH `/user-media/{userMediaId}`

Обновление рейтинга и заметки.

### Request

```json
{
  "userRating": 9.0,
  "note": "updated note"
}
```

### Response

- `200 OK`

---

## PATCH `/user-media/{userMediaId}/status`

Alias: `PATCH /api/user-media/{userMediaId}/status`

Обновление статуса в пользовательской коллекции.

### Request

```json
{
  "status": "COMPLETED"
}
```

### Response

- `200 OK`

---

## PATCH `/user-media/{userMediaId}/favourite`

Alias: `PATCH /api/user-media/{userMediaId}/favourite`

Добавление или удаление из избранного.

### Request

```json
{
  "isFavourite": true
}
```

### Response

- `200 OK`

---

## PATCH `/user-media/{userMediaId}/folders`

Alias: `PATCH /api/user-media/{userMediaId}/folders`

Обновление списка папок, к которым относится пользовательская запись.

### Request

```json
{
  "folderIds": ["folder1", "folder2"]
}
```

### Response

- `200 OK`

---

## DELETE `/user-media/{userMediaId}`

Удаление записи из пользовательской коллекции.

### Response

- `200 OK`
- `404 Not Found`

---

# 6. Пользовательские папки

Требуется JWT (`auth-jwt`).

Основные маршруты:

- `/folders`
- `/api/folders` — alias

## Сущность

```json
{
  "id": "77bb...",
  "name": "Favorites",
  "createdAt": 1737912377000,
  "updatedAt": 1737912399000
}
```

---

## POST `/folders`

Alias: `POST /api/folders`

Создание пользовательской папки.

### Request

```json
{
  "name": "Favorites"
}
```

### Response

- `201 Created` + `UserFolderResponse`

---

## GET `/folders`

Alias: `GET /api/folders`

Получение списка пользовательских папок.

### Response

- `200 OK` + `UserFolderResponse[]`

---

## PATCH `/folders/{folderId}`

Alias: `PATCH /api/folders/{folderId}`

Переименование пользовательской папки.

### Request

```json
{
  "name": "New Name"
}
```

### Response

- `200 OK`

---

## DELETE `/folders/{folderId}`

Alias: `DELETE /api/folders/{folderId}`

Удаление пользовательской папки.

### Response

- `200 OK`

---

## Общие ошибки для папок

- `400 Bad Request` — невалидное имя
- `403 Forbidden` — папка не принадлежит пользователю
- `404 Not Found`
- `409 Conflict` — папка с таким именем уже существует

---

# Формат ошибок

Большинство доменных ошибок и ошибок валидации возвращаются в формате:

```json
{
  "error": "Human readable message"
}
```

## Примечания

Некоторые ошибки поиска сейчас возвращаются plain string, а не JSON-объектом.

Ошибка `500` возвращается в формате:

```json
{
  "error": "Internal server error"
}
```
