# Use Case Diagram

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

Диаграмма разделяет три актора: `Guest`, `User` и `Admin`. `Guest` ограничен регистрацией и входом, после аутентификации сценарии переходят в пользовательский контур: просмотр каталога, поиск, управление личной коллекцией и папками.

Связь generalization (`Admin` -> `User`) фиксирует, что администратор включает все пользовательские сценарии и дополнительно выполняет операции уровня системы: создание/обновление/удаление глобальных media-записей и переиндексацию поиска. Такое разделение напрямую поддерживает role-based access policy backend (`USER` vs `ADMIN`).
