# Logical MongoDB Schema and Recommended Indexes

MongoDB используется как document-oriented хранилище; связи задаются через logical references, без SQL PK/FK constraints.

## Logical Collections

`users`
- `_id`
- `login` (в текущем коде используется `login`; при миграции на email поле меняется, принцип тот же)
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

## Recommended Indexes

`users`
- unique(`login`)  
  если идентификатором станет email: unique(`email`)

`user_media`
- unique compound index (`userId`, `mediaId`)

`media`
- index(`title`)
- index(`mediaType`)

`folders`
- index(`userId`)

Коротко: эти индексы закрывают основные сценарии доступа (auth, поиск медиа, user-media joins по logical references, выборки пользовательских папок).
