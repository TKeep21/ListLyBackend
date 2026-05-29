# Listly Backend (Ktor + MongoDB + Meilisearch)

Backend для мобильного приложения **Listly**.

## Что уже есть
- JWT авторизация (`/auth/register`, `/auth/login`)
- Коллекция пользователя (`/user-media`)
- MongoDB + Meilisearch + Docker Compose
- Unit/integration tests

## Документация
- API контракт для backend и Android: `docs/API_V1.md`
- Процесс поддержки документации: `docs/DOCS_PROCESS.md`
- UML/архитектура/use-cases: `docs/UML_ARCHITECTURE_USECASES.md`
- Разделенные UML/Schema материалы: `docs/uml/README.md`

## Быстрый старт (локально)

### 1) Поднять MongoDB и Meilisearch в Docker
```bash
docker compose up -d mongo meilisearch
```

### 2) Экспортировать переменные
```bash
export MONGO_URI='mongodb://listly_admin:secret123@localhost:27017/listlydb?authSource=admin'
export MEILI_HOST='http://localhost:7700'
export MEILI_API_KEY='masterKey'
export MEILI_INDEX='media_items'
```

### 3) Запустить backend
```bash
./gradlew run
```

Сервер: `http://localhost:8080`

## Полный запуск через Docker Compose
```bash
docker compose up --build
```

Сервисы:
- `app` -> `http://localhost:8080`
- `mongo` -> `localhost:27017`
- `meilisearch` -> `http://localhost:7700`

## Полезные команды
```bash
./gradlew test
./gradlew build
```

## Импорт датасетов (Kaggle CSV)

Файлы для фильмов:
- `input/movies_metadata.csv` — используется для импорта в `globalMediaItems`
- `input/credits.csv` и `input/keywords.csv` — пока не используются (в текущей модели нет полей под cast/crew/keywords)

Файлы для новых датасетов:
- `input/anilist_anime_data_complete.csv` — аниме из AniList, импортируется как `ANIME`
- `input/TMDB_tv_dataset_v3.csv` — сериалы из TMDB, импортируется как `SERIES`

Команды импорта:
```bash
./scripts/import-tmdb-movies-to-mongo.sh input/movies_metadata.csv
./scripts/import-anilist-anime-to-mongo.sh input/anilist_anime_data_complete.csv
./scripts/import-tmdb-series-to-mongo.sh input/TMDB_tv_dataset_v3.csv
```

Что делает скрипт:
1. Python-скрипт маппит CSV в NDJSON `MediaItem`
2. `scripts/import-media-ndjson-to-mongo.sh` заливает NDJSON в Mongo (`ListlyDB.globalMediaItems`) через upsert по полю `id`

По умолчанию импорт аниме и сериалов берет топ-5000 по полю `popularity`. Если датасет меньше, импортируются все доступные строки. Лимит можно переопределить третьим аргументом shell-скрипта или `--limit` у Python-конвертера; `0` означает без лимита.

Быстрый тест на небольшой выборке:
```bash
python3 scripts/tmdb_movies_to_media_ndjson.py --input input/movies_metadata.csv --output build/import/sample_media.ndjson --limit 100
python3 scripts/anilist_anime_to_media_ndjson.py --input input/anilist_anime_data_complete.csv --output build/import/sample_anime.ndjson --limit 100
python3 scripts/tmdb_series_to_media_ndjson.py --input input/TMDB_tv_dataset_v3.csv --output build/import/sample_series.ndjson --limit 100
```

## Как синхронизироваться с Android-клиентом
1. Любое изменение API сначала/сразу фиксируйте в `docs/API_V1.md`.
2. Backend и Android сверяют DTO/enum/error-коды только по этому файлу.
3. В каждом PR с API-изменением добавляйте секцию `API changes`.

## Текущая целевая модель данных
- `MediaItem` — глобальная сущность (видят все пользователи)
- `UserMediaItem` — пользовательская сущность со ссылкой `mediaId` на `MediaItem`

Это целевая модель для следующего этапа рефакторинга backend.
