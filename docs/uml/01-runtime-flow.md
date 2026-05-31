# Runtime Flow (Ktor + JWT)

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

JWT-проверка выполняется в рамках Ktor pipeline до передачи управления в route handler. Поэтому отказ по авторизации (`401`) формируется на уровне framework-пайплайна, а не в доменном сервисе.

После успешной аутентификации Ktor маршрутизирует запрос в конкретный endpoint, где уже запускается бизнес-сценарий через service-layer. Это отделяет технические проверки доступа от прикладной логики use-case.

Repository-слой инкапсулирует операции чтения/записи в MongoDB и возвращает доменные данные сервисам. За счёт этого маршруты и сервисы не завязаны на детали хранения документов.

Для операций изменения глобального каталога (`create/update/delete`) дополнительно выполняется синхронизация поискового индекса в Meilisearch. Ошибки поиска и ошибки БД в таком потоке диагностируются независимо.
