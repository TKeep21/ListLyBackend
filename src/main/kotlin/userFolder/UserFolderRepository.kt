package com.example.UserFolder

import com.example.UserFolder.model.UserFolder
import com.example.config.DatabaseConfig
import com.mongodb.client.model.Updates
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

class UserFolderRepository {
    private val collection = DatabaseConfig.userFolders()

    fun save(folder: UserFolder) {
        collection.insertOne(folder)
    }

    fun findAllByUserId(userId: String): List<UserFolder> {
        return collection.find(UserFolder::userId eq userId).toList()
    }

    fun findById(folderId: String): UserFolder? {
        return collection.findOne(UserFolder::id eq folderId)
    }

    fun findByIdAndUserId(userId: String, folderId: String): UserFolder? {
        return collection.findOne(
            and(
                UserFolder::id eq folderId,
                UserFolder::userId eq userId
            )
        )
    }

    fun findByIdsAndUserId(userId: String, folderIds: List<String>): List<UserFolder> {
        if (folderIds.isEmpty()) return emptyList()
        val all = findAllByUserId(userId)
        val folderIdsSet = folderIds.toSet()
        return all.filter { it.id in folderIdsSet }
    }

    fun updateName(userId: String, folderId: String, name: String) {
        collection.updateOne(
            and(
                UserFolder::id eq folderId,
                UserFolder::userId eq userId
            ),
            Updates.combine(
                Updates.set("name", name),
                Updates.set("updatedAt", System.currentTimeMillis())
            )
        )
    }

    fun delete(userId: String, folderId: String) {
        collection.deleteOne(
            and(
                UserFolder::id eq folderId,
                UserFolder::userId eq userId
            )
        )
    }
}
