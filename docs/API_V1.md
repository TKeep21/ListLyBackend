# Listly Backend API v1

Актуальный контракт backend для UI-клиента. Сверено с routes, DTO и сервисами проекта.

Base URL локально: `http://localhost:8080`

Для внешнего dev/stage-стенда UI должен менять только host, пути endpoint'ов остаются такими же.

## Общие правила

### JSON

Все request/response тела, где они есть, передаются как JSON.

```http
Content-Type: application/json
```

### Авторизация

Публичные endpoint'ы:

- `GET /health`
- `GET /json/kotlinx-serialization`
- `POST /auth/register`
- `POST /auth/login`
- `GET /media/{mediaId}`
- `GET /media/items/{title}`
- `GET /media/search`

Пользовательские и admin endpoint'ы требуют JWT:

```http
Authorization: Bearer <jwt>
```

JWT живет 24 часа и содержит claims:

- `userId`
- `role`: `USER` или `ADMIN`

Если JWT отсутствует или невалиден, Ktor auth обычно возвращает `401 Unauthorized`. Если JWT валиден, но у пользователя нет роли `ADMIN`, admin endpoint'ы возвращают:

```json
{
  "error": "Admin role required"
}
```

### Роли

| Раздел | USER | ADMIN |
|---|---:|---:|
| `GET /media/{mediaId}` | да | да |
| `GET /media/items/{title}` | да | да |
| `GET /media/search` | да | да |
| `POST /media` | нет | да |
| `PATCH /media/admin/{mediaId}` | нет | да |
| `POST /media/admin/reindex` | нет | да |
| `DELETE /media/{mediaId}` | нет | да |
| `/user-media*` | да, только свои записи | да, только свои записи |
| `/folders*` | да, только свои папки | да, только свои папки |

### Формат ошибок

Большинство доменных ошибок возвращаются так:

```json
{
  "error": "Human-readable message"
}
```

Некоторые ответы от Ktor auth или ручные `BadRequest`/`NotFound` могут быть без JSON-тела. `500 Internal Server Error` возвращается так:

```json
{
  "error": "Internal server error"
}
```

---

# 1. Health и служебные endpoint'ы

## GET `/health`

Проверка состояния API и Meilisearch.

### Response `200 OK`

```json
{
  "status": "UP",
  "services": {
    "api": "UP",
    "search": "UP"
  }
}
```

`services.search` может быть `"DOWN"`, если Meilisearch недоступен. HTTP-код при этом все равно `200 OK`.

## GET `/json/kotlinx-serialization`

Тестовый endpoint из настройки serialization. UI использовать не должен.

### Response `200 OK`

```json
{
  "hello": "world"
}
```

---

# 2. Авторизация

## POST `/auth/register`

Создает пользователя с ролью `USER`.

### Request

```json
{
  "login": "mike",
  "password": "strongpass123"
}
```

### Валидация

- `login` не должен быть пустым
- `password` не должен быть пустым
- `login.length >= 3`
- `login.length <= 20`
- `password.length >= 6`
- `password.length <= 25`

Важно: текущие сообщения в коде говорят "more than 3/6", но проверка фактически допускает длину `3` и `6`.

### Response

- `201 Created` — без тела

### Ошибки

- `400 Bad Request` — пустые поля или невалидная длина
- `409 Conflict` — пользователь уже существует

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

- `400 Bad Request` — невалидный JSON/body
- `401 Unauthorized` — неправильный логин или пароль

---

# 3. Глобальный каталог медиа

Основные пути:

- `/media`
- `/mediaCatalog` — полный alias для тех же catalog endpoint'ов

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

## `MediaItem`

Сущность, которую возвращают catalog и search endpoint'ы:

```json
{
  "id": "664f...",
  "title": "Interstellar",
  "description": "Space sci-fi",
  "mediaType": "MOVIE",
  "mediaStatus": "FINISHED",
  "genres": ["Sci-Fi", "Drama"],
  "posterUrl": "https://image.tmdb.org/t/p/w500/poster.jpg",
  "userRatingSum": 18.0,
  "userRatingCount": 2,
  "createdAt": 1737912345000,
  "updatedAt": 1737912399000
}
```

Для UI:

- средний рейтинг можно считать как `userRatingSum / userRatingCount`, если `userRatingCount > 0`
- `posterUrl` может быть `null`
- backend нормализует TMDB poster path в `https://image.tmdb.org/t/p/w500/...`
- не-TMDB URL принимается только если scheme `https`
- `http` poster URL для обычных внешних ссылок будет превращен в `null`

## GET `/media/{mediaId}`

Alias: `GET /mediaCatalog/{mediaId}`

Получение одного элемента глобального каталога.

### Response

- `200 OK` + `MediaItem`
- `404 Not Found` — без JSON-тела, если элемент не найден

## GET `/media/items/{title}`

Alias: `GET /mediaCatalog/items/{title}`

Точное получение элементов по title. Это не полнотекстовый поиск.

### Response `200 OK`

```json
[
  {
    "id": "664f...",
    "title": "Interstellar",
    "description": "Space sci-fi",
    "mediaType": "MOVIE",
    "mediaStatus": "FINISHED",
    "genres": ["Sci-Fi"],
    "posterUrl": null,
    "userRatingSum": 0.0,
    "userRatingCount": 0,
    "createdAt": 1737912345000,
    "updatedAt": 1737912345000
  }
]
```

## POST `/media` — только ADMIN

Alias: `POST /mediaCatalog`

Создание элемента в глобальном каталоге.

### Request

```json
{
  "title": "Interstellar",
  "description": "Space sci-fi",
  "mediaType": "MOVIE",
  "mediaStatus": "FINISHED",
  "genres": ["Sci-Fi", "Drama"],
  "posterUrl": "https://image.tmdb.org/t/p/w500/poster.jpg"
}
```

### Поля

| Поле | Тип | Обязательное | Комментарий |
|---|---|---:|---|
| `title` | `String` | да | не должен быть пустым |
| `description` | `String?` | нет | пустая строка сохраняется как `null` |
| `mediaType` | `MediaType` | да | enum |
| `mediaStatus` | `MediaStatus` | да | enum |
| `genres` | `String[]` | нет | default `[]`, пустые жанры отфильтровываются |
| `posterUrl` | `String?` | нет | нормализуется, невалидное значение станет `null` |

### Response

- `201 Created` + созданный `MediaItem`

### Ошибки

- `400 Bad Request` — например, пустой `title`, невалидный enum/body
- `401 Unauthorized` — нет/невалидный JWT
- `403 Forbidden` — JWT есть, но роль не `ADMIN`

## PATCH `/media/admin/{mediaId}` — только ADMIN

Alias: `PATCH /mediaCatalog/admin/{mediaId}`

Частичное обновление элемента каталога.

### Request

```json
{
  "title": "Interstellar (2014)",
  "description": "Space sci-fi",
  "mediaStatus": "FINISHED",
  "genres": ["Sci-Fi"],
  "posterUrl": "poster.jpg"
}
```

### Поля

Все поля опциональные:

- `title`
- `description`
- `mediaStatus`
- `genres`
- `posterUrl`

Важно для UI:

- `mediaType` через этот endpoint обновить нельзя
- `title`, если передан, не должен быть пустым
- `null` для `description` или `posterUrl` сейчас не очищает поле, потому что backend обновляет только non-null поля
- если body пустой или все поля `null`, backend вернет `200 OK`, но ничего не изменит

### Response

- `200 OK` — без тела

### Ошибки

- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

## POST `/media/admin/reindex` — только ADMIN

Alias: `POST /mediaCatalog/admin/reindex`

Переиндексация Meilisearch из MongoDB.

### Response

- `200 OK` — без тела

### Ошибки

- `401 Unauthorized`
- `403 Forbidden`
- `500 Internal Server Error` — если reindex упал с ошибкой клиента Meilisearch

Важно: старые документы могли описывать `202 Accepted` и JSON `{"message":"Reindex started"}`. В текущем коде endpoint отвечает именно `200 OK` без тела.

## DELETE `/media/{mediaId}` — только ADMIN

Alias: `DELETE /mediaCatalog/{mediaId}`

Удаляет элемент из глобального каталога и пытается удалить его из search index.

### Response

- `200 OK` — без тела

### Ошибки

- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

# 4. Поиск

## GET `/media/search`

Публичный полнотекстовый поиск по глобальному каталогу через Meilisearch.

### Query параметры

| Параметр | Тип | Default | Ограничение |
|---|---|---:|---|
| `query` | `String` | `""` | после trim пустой query вернет `[]` |
| `limit` | `Int` | `12` | `1..50` |
| `offset` | `Int` | `0` | `>= 0` |

`query` обрезается backend'ом до 150 символов перед отправкой в Meilisearch.
Если `limit` или `offset` переданы не числом, backend использует default-значения `12` и `0`.

### Пример

```http
GET /media/search?query=interstellar&limit=12&offset=0
```

### Response `200 OK`

```json
[
  {
    "id": "664f...",
    "title": "Interstellar",
    "description": "Space sci-fi",
    "mediaType": "MOVIE",
    "mediaStatus": "FINISHED",
    "genres": ["Sci-Fi", "Drama"],
    "posterUrl": "https://image.tmdb.org/t/p/w500/poster.jpg",
    "userRatingSum": 18.0,
    "userRatingCount": 2,
    "createdAt": 1737912345000,
    "updatedAt": 1737912399000
  }
]
```

Если `query` пустой:

```json
[]
```

Если index в Meilisearch не найден, backend также возвращает `[]` с `200 OK`.

### Ошибки

- `400 Bad Request` — `limit` вне `1..50` или `offset < 0`
- `503 Service Unavailable` — Meilisearch недоступен или search request упал

---

# 5. Пользовательская коллекция медиа

Требуется JWT.

Основной путь: `/user-media`

Частичный alias: `/api/user-media`

Alias `/api/user-media` поддерживает только:

- `GET /api/user-media`
- `PATCH /api/user-media/{userMediaId}/status`
- `PATCH /api/user-media/{userMediaId}/favourite`
- `PATCH /api/user-media/{userMediaId}/folders`

Он не поддерживает `POST`, `GET /{id}`, обычный `PATCH /{id}` и `DELETE`.

## Enum `collectionStatus`

- `PLANNED`
- `IN_PROGRESS`
- `COMPLETED`
- `DROPPED`

## `UserMediaResponse`

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

Важно для UI: user-media response содержит только `mediaId`, а не вложенный `MediaItem`. Чтобы показать title/poster/description, UI должен догрузить глобальное медиа через `GET /media/{mediaId}` или использовать отдельный клиентский кэш.

## POST `/user-media`

Добавляет медиа в коллекцию текущего пользователя.

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

### Поля

| Поле | Тип | Обязательное | Default/ограничение |
|---|---|---:|---|
| `mediaId` | `String` | да | должен существовать в global catalog |
| `collectionStatus` | `UserCollectionStatus?` | нет | default `PLANNED` |
| `isFavourite` | `Boolean` | нет | default `false` |
| `folderIds` | `String[]` | нет | default `[]`, дубликаты удаляются |
| `userRating` | `Double?` | нет | `0.0..10.0` |
| `note` | `String?` | нет | максимум 400 символов |

DTO также содержит `createdAt` и `updatedAt`, но сервис при создании через request их игнорирует. UI не должен их отправлять.

### Response

- `201 Created` — без тела

### Ошибки

- `400 Bad Request` — невалидный рейтинг/заметка/body
- `401 Unauthorized`
- `403 Forbidden` — один из `folderIds` не принадлежит пользователю
- `404 Not Found` — `mediaId` не найден
- `409 Conflict` — это медиа уже есть в коллекции пользователя

## GET `/user-media`

Alias: `GET /api/user-media`

Возвращает коллекцию текущего пользователя.

### Query параметры

| Параметр | Тип | Пример |
|---|---|---|
| `status` | `UserCollectionStatus` | `COMPLETED` |
| `favourite` | `Boolean` | `true` |
| `folderId` | `String` | `77bb...` |

Фильтры можно комбинировать.

### Response `200 OK`

```json
[
  {
    "id": "66aa...",
    "mediaId": "65ff...",
    "collectionStatus": "COMPLETED",
    "isFavourite": true,
    "folderIds": ["77bb..."],
    "userRating": 9.0,
    "note": "updated note",
    "createdAt": 1737912377000,
    "updatedAt": 1737912399000
  }
]
```

### Ошибки

- `400 Bad Request` — неизвестный `status` или `favourite` не `true|false`
- `401 Unauthorized`

## GET `/user-media/{userMediaId}`

Возвращает одну запись коллекции текущего пользователя.

### Response

- `200 OK` + `UserMediaResponse`
- `404 Not Found`

### Ошибки

- `401 Unauthorized`

## PATCH `/user-media/{userMediaId}`

Обновляет рейтинг и заметку.

### Request

```json
{
  "userRating": 9.0,
  "note": "updated note"
}
```

### Поля

- `userRating`: optional, `0.0..10.0`
- `note`: optional, максимум 400 символов

Важно для UI:

- этот endpoint не обновляет `collectionStatus`, `isFavourite` или `folderIds`
- `null` сейчас не очищает `userRating` или `note`, потому что backend обновляет только non-null поля
- если body пустой или все поля `null`, backend вернет `200 OK`, но ничего не изменит

### Response

- `200 OK` — без тела

### Ошибки

- `400 Bad Request`
- `401 Unauthorized`
- `404 Not Found`

## PATCH `/user-media/{userMediaId}/status`

Alias: `PATCH /api/user-media/{userMediaId}/status`

Обновляет статус записи.

### Request

```json
{
  "status": "COMPLETED"
}
```

### Response

- `200 OK` — без тела

### Ошибки

- `400 Bad Request` — невалидный enum/body
- `401 Unauthorized`
- `404 Not Found`

## PATCH `/user-media/{userMediaId}/favourite`

Alias: `PATCH /api/user-media/{userMediaId}/favourite`

Добавляет или убирает запись из избранного.

### Request

```json
{
  "isFavourite": true
}
```

### Response

- `200 OK` — без тела

### Ошибки

- `400 Bad Request`
- `401 Unauthorized`
- `404 Not Found`

## PATCH `/user-media/{userMediaId}/folders`

Alias: `PATCH /api/user-media/{userMediaId}/folders`

Полностью заменяет список папок у записи.

### Request

```json
{
  "folderIds": ["77bb...", "88cc..."]
}
```

### Response

- `200 OK` — без тела

### Ошибки

- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden` — одна из папок не принадлежит пользователю или не существует для него
- `404 Not Found`

## DELETE `/user-media/{userMediaId}`

Удаляет запись из коллекции текущего пользователя. Если у записи был `userRating`, backend уменьшает агрегированный рейтинг у глобального media.

### Response

- `200 OK` — без тела

### Ошибки

- `401 Unauthorized`
- `404 Not Found`

---

# 6. Пользовательские папки

Требуется JWT.

Основные пути:

- `/folders`
- `/api/folders` — полный alias для тех же folder endpoint'ов

## `UserFolderResponse`

```json
{
  "id": "77bb...",
  "name": "Favorites",
  "createdAt": 1737912377000,
  "updatedAt": 1737912399000
}
```

## POST `/folders`

Alias: `POST /api/folders`

Создает папку текущего пользователя.

### Request

```json
{
  "name": "Favorites"
}
```

### Валидация

- `name.trim()` не должен быть пустым
- длина после trim: `1..40`
- имя должно быть уникальным для пользователя без учета регистра

### Response `201 Created`

```json
{
  "id": "77bb...",
  "name": "Favorites",
  "createdAt": 1737912377000,
  "updatedAt": 1737912377000
}
```

### Ошибки

- `400 Bad Request`
- `401 Unauthorized`
- `409 Conflict` — папка с таким именем уже существует

## GET `/folders`

Alias: `GET /api/folders`

Возвращает папки текущего пользователя.

### Response `200 OK`

```json
[
  {
    "id": "77bb...",
    "name": "Favorites",
    "createdAt": 1737912377000,
    "updatedAt": 1737912377000
  }
]
```

### Ошибки

- `401 Unauthorized`

## PATCH `/folders/{folderId}`

Alias: `PATCH /api/folders/{folderId}`

Переименовывает папку.

### Request

```json
{
  "name": "New Name"
}
```

### Response

- `200 OK` — без тела

### Ошибки

- `400 Bad Request` — невалидное имя
- `401 Unauthorized`
- `403 Forbidden` — папка не принадлежит пользователю
- `404 Not Found`
- `409 Conflict` — папка с таким именем уже существует

## DELETE `/folders/{folderId}`

Alias: `DELETE /api/folders/{folderId}`

Удаляет папку и удаляет этот `folderId` из всех user-media записей текущего пользователя.

### Response

- `200 OK` — без тела

### Ошибки

- `401 Unauthorized`
- `403 Forbidden` — папка не принадлежит пользователю
- `404 Not Found`

---

# 7. Быстрый чеклист для UI

- Сохранять JWT из `POST /auth/login` и передавать `Authorization: Bearer <token>`.
- Для пользовательской коллекции использовать `collectionStatus`, не старое имя `userMediaStatus`.
- Для глобального медиа использовать `mediaType`, не старое имя `type`.
- Для жанров использовать `genres`, не старое имя `genre`.
- `GET /user-media` не возвращает вложенное медиа, только `mediaId`.
- Для поиска использовать `GET /media/search?query=...&limit=12&offset=0`.
- Для избранного использовать `PATCH /user-media/{id}/favourite`.
- Для статуса использовать `PATCH /user-media/{id}/status`.
- Для папок использовать `PATCH /user-media/{id}/folders`; это полная замена списка.
- Не ожидать body у `POST /user-media`, `PATCH`, `DELETE`, `POST /media/admin/reindex`.
- Любое изменение API в backend должно обновлять этот файл в том же PR.
