# Listly Backend: UML Flow, Architecture, Use Case

Этот документ собран по актуальному коду backend и подходит для вставки в проектную документацию.

## 1) UML Flow (основной runtime-поток запроса)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client (Mobile/Web)
    participant Ktor as Ktor Framework (Routing + Plugins)
    participant Route as Route Handler
    participant Service as Domain Service
    participant Repo as Repository
    participant Mongo as MongoDB
    participant Search as Meilisearch

    Client->>Ktor: HTTP request
    Ktor->>Ktor: Authentication pipeline (JWT for protected routes)
    alt JWT missing/invalid (protected route)
        Ktor-->>Client: 401 Unauthorized
    else Pipeline passed
        Ktor->>Route: Dispatch matched endpoint
        Route->>Service: Execute use case
        Service->>Repo: Read/Write data
        Repo->>Mongo: Query/Update documents
        Mongo-->>Repo: DB result
        Repo-->>Service: Domain entities
        opt Media create/update/delete
            Service->>Search: Sync search index
            Search-->>Service: Indexing result
        end
        Service-->>Route: Result / domain response
        Route-->>Ktor: HTTP response payload
        Ktor-->>Client: HTTP response (2xx/4xx/5xx)
    end
```

## 2) Архитектурная схема (компоненты)

```mermaid
flowchart LR
    Client[Mobile App / API Client] --> App[Ktor Application]
    App --> Pipeline[Ktor Pipeline\nRouting + Authentication + Plugins]
    Pipeline --> Routes[Route Handlers\nAuth, GlobalMedia, UserMedia, UserFolder, Search]

    Routes --> AuthService[AuthService]
    Routes --> MediaService[MediaCatalogService]
    Routes --> UserMediaService[UserMediaService]
    Routes --> UserFolderService[UserFolderService]
    Routes --> SearchService[MeiliMediaSearchServiceImpl]

    AuthService --> UserRepo[UserRepository]

    MediaService --> MediaRepo[MediaCatalogRepository]
    MediaService --> IndexService[SearchIndexService]

    UserMediaService --> UserMediaRepo[UserMediaRepository]
    UserMediaService --> MediaRepo
    UserMediaService --> FolderRepo[UserFolderRepository]

    UserFolderService --> FolderRepo
    UserFolderService --> UserMediaRepo

    SearchService --> SearchRepo[MeiliMediaSearchRepository]
    SearchService --> MediaRepo

    IndexService --> SearchRepo
    IndexService --> MediaRepo

    UserRepo --> Mongo[(MongoDB)]
    MediaRepo --> Mongo
    UserMediaRepo --> Mongo
    FolderRepo --> Mongo

    SearchRepo --> Meili[(Meilisearch)]
```

На high-level уровне зависимости между доменными сервисами сведены к минимуму; ключевые связи показаны через repositories.

## 3) Use Case Diagram

```mermaid
flowchart LR
    Guest((Guest))
    User((User))
    Admin((Admin))

    Register([Register])
    Login([Login])
    BrowseCatalog([Browse Global Catalog])
    SearchMedia([Search Media])

    ManageOwnMedia([Manage Own User Media\ncreate/update/delete/status/favourite/folders])
    ManageOwnFolders([Manage Own Folders\ncreate/rename/delete])

    CreateGlobal([Create Global Media Item])
    UpdateGlobal([Update Global Media Item])
    DeleteGlobal([Delete Global Media Item])
    Reindex([Reindex Search Index])

    Guest --> Register
    Guest --> Login

    User --> BrowseCatalog
    User --> SearchMedia
    User --> ManageOwnMedia
    User --> ManageOwnFolders

    Admin -.->|generalization| User
    Admin --> CreateGlobal
    Admin --> UpdateGlobal
    Admin --> DeleteGlobal
    Admin --> Reindex
```

## 4) Logical MongoDB Schema

MongoDB используется как document-oriented хранилище; связи моделируются как logical references между документами, а не как SQL PK/FK constraints.

`users`
- `_id`
- `login` (в проекте используется login; при миграции на email индексная стратегия сохраняется)
- `passwordHash`
- `role`

`media`
- `id`
- `title`
- `description`
- `mediaType`
- `mediaStatus`
- `genres`

`user_media`
- `id`
- `userId` -> logical reference to `users._id`
- `mediaId` -> logical reference to `media.id`
- `collectionStatus`
- `isFavourite`
- `folderIds[]` -> logical references to folder identifiers
- `userRating`
- `note`

`folders`
- `id`
- `userId` -> logical reference to `users._id`
- `name`

## 5) п Indexes

`users`
- unique(`login`)  
  если используется email-идентификатор: unique(`email`)

`user_media`
- unique compound index (`userId`, `mediaId`)

`media`
- index(`title`)
- index(`mediaType`)

`folders`
- index(`userId`)

## Замечание по ролям

- `Create Global Media Item` (`POST /media` и alias `POST /mediaCatalog`) должен выполняться только ролью `ADMIN`.
- В коде это зафиксировано через `authenticate("auth-jwt") + requireAdmin(...)`.
