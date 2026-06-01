# Listly Backend

Backend-часть приложения **Listly** - сервиса для ведения личной медиатеки. Пользователь может регистрироваться, искать фильмы/сериалы/аниме/игры в общем каталоге, добавлять их в свою коллекцию, отмечать статус просмотра/прохождения, избранное, рейтинг, заметки и распределять элементы по папкам.

Проект написан на **Kotlin + Ktor** и использует **MongoDB** как основное хранилище, а **Meilisearch** - для быстрого полнотекстового поиска по глобальному каталогу медиа.

## Возможности

- Регистрация и вход по JWT: `/auth/register`, `/auth/login`
- Глобальный каталог медиа: фильмы, сериалы, аниме, игры
- Поиск по каталогу через Meilisearch
- Личная коллекция пользователя: статусы, избранное, рейтинг, заметки
- Пользовательские папки для группировки элементов коллекции
- Админские операции над глобальным каталогом
- Health-check для проверки API и поиска
- Docker Compose для запуска MongoDB, Meilisearch и backend
- Тесты сервисного слоя и HTTP routes
- Скрипты импорта CSV-датасетов в MongoDB

## Стек

| Часть | Технологии |
|---|---|
| Backend | Kotlin 2.2.21, Ktor 3.3.2 |
| HTTP server | Ktor Netty |
| Auth | JWT, BCrypt |
| Database | MongoDB 7, KMongo |
| Search | Meilisearch 1.12 |
| Serialization | kotlinx.serialization |
| Tests | JUnit 5, Ktor server tests, MockK |
| Infrastructure | Docker, Docker Compose, Gradle |

## Архитектура

Основной поток обработки запроса:

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client
    participant Ktor as Ktor Routing/Auth
    participant Route as Route Handler
    participant Service as Domain Service
    participant Repo as Repository
    participant Mongo as MongoDB
    participant Meili as Meilisearch

    Client->>Ktor: HTTP request
    Ktor->>Ktor: JWT/auth plugins
    Ktor->>Route: matched endpoint
    Route->>Service: use case
    Service->>Repo: read/write data
    Repo->>Mongo: query/update documents
    Mongo-->>Repo: result
    Repo-->>Service: domain entities
    opt catalog changes
        Service->>Meili: sync search index
    end
    Service-->>Route: result
    Route-->>Client: HTTP response
```

Ключевые слои проекта:

- `routes` - HTTP endpoints и преобразование HTTP-запросов в вызовы сервисов
- `auth`, `security`, `util` - регистрация, вход, JWT, роли, хеширование паролей
- `media` - глобальный каталог медиа
- `userMedia` - личная коллекция пользователя
- `userFolder` - пользовательские папки
- `search` - интеграция с Meilisearch
- `config` - настройка Ktor, CORS, сериализации, БД, поиска и security

## Модель данных

Основные коллекции MongoDB:

| Коллекция | Назначение |
|---|---|
| `users` | Пользователи, хеши паролей, роли |
| `globalMediaItems` | Общий каталог медиа |
| `userMediaItems` | Элементы личной коллекции пользователя |
| `userFolders` | Папки пользователя |

Важные индексы создаются при старте приложения:

- `users.login` - уникальный индекс
- `userMediaItems(userId, mediaId)` - уникальная пара, чтобы пользователь не добавлял одно медиа дважды
- `globalMediaItems.title`
- `globalMediaItems.mediaType`
- `userFolders.userId`

## Быстрый старт локально

### 1. Подготовить переменные окружения

Для Docker Compose удобно создать файл `.env` в корне проекта:

```env
MONGO_ROOT_USERNAME=listly_admin
MONGO_ROOT_PASSWORD=secret123
MONGO_DATABASE=ListlyDB
MEILI_MASTER_KEY=masterKey
```

Для локального запуска backend через Gradle также нужны переменные:

```bash
export MONGO_URI='mongodb://listly_admin:secret123@localhost:27017/ListlyDB?authSource=admin'
export MEILI_HOST='http://localhost:7700'
export MEILI_API_KEY='masterKey'
export MEILI_INDEX='media_items'
```

### 2. Поднять MongoDB и Meilisearch

```bash
docker compose up -d mongo meilisearch
```

### 3. Запустить backend

```bash
./gradlew run
```

После запуска API доступно по адресу:

```text
http://localhost:8080
```

Если используется развернутый сервер на VM, API доступно по адресу:

```text
http://158.160.251.150:8080
```

Проверка состояния:

```bash
curl http://localhost:8080/health
```

Ожидаемый ответ:

```json
{
  "status": "UP",
  "services": {
    "api": "UP",
    "search": "UP"
  }
}
```

## Полный запуск через Docker Compose

```bash
docker compose up --build
```

Сервисы:

| Сервис | Адрес |
|---|---|
| Backend | `http://localhost:8080` |
| MongoDB | внутри compose-сети: `mongo:27017` |
| Meilisearch | внутри compose-сети: `http://meilisearch:7700` |

## Сервер на VM

Проект также развернут на виртуальной машине:

```text
http://158.160.251.150:8080
```

Подключение к серверу по SSH:

```bash
ssh -i ~/.ssh/vm_resources viviten@158.160.251.150
```

На защите можно показать, что backend доступен не только локально:

```bash
curl http://158.160.251.150:8080/health
```

## Конфигурация

Приложение читает настройки из переменных окружения и `src/main/resources/application.yaml`.

| Переменная | Назначение | Пример |
|---|---|---|
| `MONGO_URI` | Строка подключения к MongoDB | `mongodb://listly_admin:secret123@localhost:27017/ListlyDB?authSource=admin` |
| `MEILI_HOST` | Адрес Meilisearch | `http://localhost:7700` |
| `MEILI_API_KEY` | Master/API key Meilisearch | `masterKey` |
| `MEILI_INDEX` | Индекс для поиска медиа | `media_items` |

Важно: в текущем коде рабочая база MongoDB выбирается как `ListlyDB`.

## Основные API endpoints

### Auth

| Метод | Endpoint | Описание |
|---|---|---|
| `POST` | `/auth/register` | Создать пользователя |
| `POST` | `/auth/login` | Получить JWT |

Пример регистрации:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"mike","password":"strongpass123"}'
```

Пример входа:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"mike","password":"strongpass123"}'
```

Ответ:

```json
{
  "token": "<jwt>"
}
```

Защищенные endpoints требуют заголовок:

```http
Authorization: Bearer <jwt>
```

### Global Media Catalog

| Метод | Endpoint | Доступ | Описание |
|---|---|---|---|
| `GET` | `/media/{mediaId}` | public | Получить медиа по id |
| `GET` | `/media/items/{title}` | public | Найти медиа по названию |
| `GET` | `/media/discover?limit=12&offset=0` | public | Получить подборку каталога |
| `POST` | `/media` | `ADMIN` | Создать элемент каталога |
| `PATCH` | `/media/admin/{mediaId}` | `ADMIN` | Обновить элемент каталога |
| `DELETE` | `/media/{mediaId}` | `ADMIN` | Удалить элемент каталога |
| `POST` | `/media/admin/reindex` | `ADMIN` | Переиндексировать поиск |

Для совместимости также есть alias `/mediaCatalog`.

Типы медиа:

```text
MOVIE, SERIES, ANIME, GAME
```

Статусы медиа:

```text
FINISHED, ONGOING, ANNOUNCED
```

### Search

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/media/search?query=interstellar&limit=12&offset=0` | Поиск медиа |

Поиск выполняется через Meilisearch, после чего backend загружает найденные элементы из MongoDB по id.

### User Media

Endpoints личной коллекции требуют JWT.

| Метод | Endpoint | Описание |
|---|---|---|
| `POST` | `/user-media` | Добавить медиа в коллекцию |
| `GET` | `/user-media` | Получить коллекцию с фильтрами |
| `GET` | `/user-media/{userMediaId}` | Получить элемент коллекции |
| `PATCH` | `/user-media/{userMediaId}` | Обновить рейтинг/заметку |
| `PATCH` | `/user-media/{userMediaId}/status` | Изменить статус |
| `PATCH` | `/user-media/{userMediaId}/favourite` | Изменить избранное |
| `PATCH` | `/user-media/{userMediaId}/folders` | Изменить папки |
| `DELETE` | `/user-media/{userMediaId}` | Удалить из коллекции |

Фильтры `GET /user-media`:

| Query param | Значения |
|---|---|
| `status` | `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `DROPPED` |
| `favourite` | `true`, `false` |
| `folderId` | id папки |
| `mediaType` | `MOVIE`, `SERIES`, `ANIME`, `GAME` |
| `sortBy` | `added_date`, `title` |
| `sortDir` | `asc`, `desc` |

Для части методов также есть alias `/api/user-media`.

### User Folders

Endpoints папок требуют JWT.

| Метод | Endpoint | Описание |
|---|---|---|
| `POST` | `/folders` | Создать папку |
| `GET` | `/folders` | Получить папки пользователя |
| `PATCH` | `/folders/{folderId}` | Переименовать папку |
| `DELETE` | `/folders/{folderId}` | Удалить папку |

Также есть alias `/api/folders`.

## Роли

| Возможность | `USER` | `ADMIN` |
|---|---:|---:|
| Регистрация и вход | да | да |
| Просмотр каталога | да | да |
| Поиск медиа | да | да |
| Управление своей коллекцией | да | да |
| Управление своими папками | да | да |
| Создание/обновление/удаление глобального каталога | нет | да |
| Переиндексация поиска | нет | да |

## Тесты и сборка

Запустить все тесты:

```bash
./gradlew test
```

Собрать проект:

```bash
./gradlew build
```

Отдельно в проекте есть task:

```bash
./gradlew testAll
```

## Импорт датасетов

Проект содержит скрипты для импорта CSV-датасетов в MongoDB. Конвертация происходит в NDJSON, затем данные загружаются в коллекцию `globalMediaItems` через upsert по `id`.

Поддерживаемые датасеты:

| Файл | Тип медиа | Команда |
|---|---|---|
| `input/movies_DB.csv` | `MOVIE` | `./scripts/import-movies-db-to-mongo.sh input/movies_DB.csv` |
| `input/anilist_anime_data_complete.csv` | `ANIME` | `./scripts/import-anilist-anime-to-mongo.sh input/anilist_anime_data_complete.csv` |
| `input/TMDB_tv_dataset_v3.csv` | `SERIES` | `./scripts/import-tmdb-series-to-mongo.sh input/TMDB_tv_dataset_v3.csv` |
| `input/Ultimate_Games_Dataset.csv` | `GAME` | `./scripts/import-ultimate-games-to-mongo.sh input/Ultimate_Games_Dataset.csv` |

Быстрая проверка конвертации на небольшой выборке:

```bash
python3 scripts/movies_db_to_media_ndjson.py \
  --input input/movies_DB.csv \
  --output build/import/sample_movies.ndjson \
  --limit 100
```

После импорта можно вызвать админский endpoint:

```bash
curl -X POST http://localhost:8080/media/admin/reindex \
  -H "Authorization: Bearer <admin-jwt>"
```

## Документация

| Файл | Что внутри |
|---|---|
| `docs/API.md` | Полный API contract на английском |
| `docs/listly-backend-api-ru.md` | API contract на русском |
| `docs/UML_ARCHITECTURE_USECASES.md` | Архитектура, runtime flow, use cases, схема данных |
| `docs/uml/README.md` | Отдельные UML-диаграммы и схемы |
| `docs/DOCS_PROCESS.md` | Правила поддержки документации |

## Что можно рассказать на защите

- Backend разделен на routes, services и repositories, поэтому HTTP-слой отделен от бизнес-логики и доступа к данным.
- JWT используется для защиты пользовательских endpoints, а роль `ADMIN` ограничивает операции с глобальным каталогом.
- MongoDB хранит основные сущности приложения, а Meilisearch вынесен отдельно, чтобы поиск не зависел от тяжелых запросов к базе.
- При изменении глобального каталога backend синхронизирует поисковый индекс.
- Для пользовательской коллекции есть уникальный индекс `(userId, mediaId)`, который защищает от дублей.
- Health endpoint показывает состояние API и поискового сервиса.
- Docker Compose позволяет поднять окружение одной командой.
- Тесты покрывают auth, каталог, пользовательскую коллекцию, папки, поиск и health route.

## Структура проекта

```text
.
├── src/main/kotlin
│   ├── Application.kt
│   ├── auth
│   ├── config
│   ├── media
│   ├── routes
│   ├── search
│   ├── security
│   ├── user
│   ├── userFolder
│   └── userMedia
├── src/test/kotlin
├── docs
├── scripts
├── input
├── docker-compose.yml
├── Dockerfile
└── build.gradle.kts
```

## Короткий сценарий демо

1. Поднять окружение: `docker compose up --build`
2. Проверить API: `GET /health`
3. Зарегистрировать пользователя: `POST /auth/register`
4. Войти и получить JWT: `POST /auth/login`
5. Найти медиа: `GET /media/search?query=...`
6. Добавить медиа в коллекцию: `POST /user-media`
7. Изменить статус или избранное: `PATCH /user-media/{id}/status`, `PATCH /user-media/{id}/favourite`
8. Создать папку: `POST /folders`
9. Показать фильтрацию коллекции: `GET /user-media?status=PLANNED&sortBy=title&sortDir=asc`
