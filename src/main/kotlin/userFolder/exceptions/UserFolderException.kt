package com.example.UserFolder.exceptions

open class UserFolderException(
    message: String?
) : RuntimeException(message)

class UserFolderNotFoundException(folderId: String? = null) : UserFolderException(
    message = if (folderId == null) "Folder not found" else "Folder not found: $folderId"
)

class UserFolderAlreadyExistsException(name: String) : UserFolderException(
    "Folder with name '$name' already exists"
)

class InvalidUserFolderRequestException(
    message: String
) : UserFolderException(message)

class ForbiddenUserFolderAccessException(folderId: String? = null) : UserFolderException(
    message = if (folderId == null) "Access to this folder is forbidden" else "Access to folder '$folderId' is forbidden"
)
