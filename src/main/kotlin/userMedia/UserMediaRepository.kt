package com.example.UserMedia

import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.model.UserCollectionStatus
import com.example.UserMedia.model.SortDirection
import com.example.UserMedia.model.UserMediaSortBy
import com.example.UserMedia.model.UserMediaItem
import com.example.config.DatabaseConfig
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import org.litote.kmongo.and
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.pull

class UserMediaRepository {
    val collection = DatabaseConfig.userMediaItems()


    fun findAllByUser(
        userId: String,
        status: UserCollectionStatus? = null,
        favourite: Boolean? = null,
        folderId: String? = null,
        sortBy: UserMediaSortBy = UserMediaSortBy.CREATED_AT,
        sortDirection: SortDirection = SortDirection.DESC
    ): List<UserMediaItem> {
        val filters = mutableListOf<org.bson.conversions.Bson>()
        filters.add(UserMediaItem::userId eq userId)

        status?.let {
            filters.add(UserMediaItem::collectionStatus eq it)
        }

        favourite?.let {
            filters.add(UserMediaItem::isFavourite eq it)
        }

        folderId?.let {
            filters.add(UserMediaItem::folderIds contains it)
        }

        val sortField = when (sortBy) {
            UserMediaSortBy.CREATED_AT -> "createdAt"
            UserMediaSortBy.TITLE -> "createdAt"
        }
        val sort = when (sortDirection) {
            SortDirection.ASC -> Sorts.ascending(sortField)
            SortDirection.DESC -> Sorts.descending(sortField)
        }

        return collection.find(and(*filters.toTypedArray()))
            .sort(sort)
            .toList()
    }

    fun findById(userId:String,userMediaId:String): UserMediaItem?{
        return collection.findOne(
            and(
                UserMediaItem::userId eq userId,
                UserMediaItem::id eq userMediaId
            )
        )
    }

    fun save( userMediaItem: UserMediaItem){
        collection.insertOne(userMediaItem)
    }

    fun update(
        userId: String,
        userMediaId: String,
        request: UpdateUserMediaRequest
    ) {
        val updates = mutableListOf<org.bson.conversions.Bson>()

        request.userRating?.let {
            updates.add(Updates.set("userRating", it))
        }
        request.note?.let {
            updates.add(Updates.set("note", it))
        }

        if (updates.isEmpty()) return

        updates.add(
            Updates.set("updatedAt", System.currentTimeMillis())
        )

        collection.updateOne(
            and(
                UserMediaItem::id eq userMediaId,
                UserMediaItem::userId eq userId
            ),
            Updates.combine(updates)
        )
    }

    fun updateStatus(
        userId: String,
        userMediaId: String,
        status: UserCollectionStatus
    ) {
        collection.updateOne(
            and(
                UserMediaItem::id eq userMediaId,
                UserMediaItem::userId eq userId
            ),
            Updates.combine(
                Updates.set("collectionStatus", status),
                Updates.set("updatedAt", System.currentTimeMillis())
            )
        )
    }

    fun updateFavourite(
        userId: String,
        userMediaId: String,
        isFavourite: Boolean
    ) {
        collection.updateOne(
            and(
                UserMediaItem::id eq userMediaId,
                UserMediaItem::userId eq userId
            ),
            Updates.combine(
                Updates.set("isFavourite", isFavourite),
                Updates.set("updatedAt", System.currentTimeMillis())
            )
        )
    }

    fun updateFolders(
        userId: String,
        userMediaId: String,
        folderIds: List<String>
    ) {
        collection.updateOne(
            and(
                UserMediaItem::id eq userMediaId,
                UserMediaItem::userId eq userId
            ),
            Updates.combine(
                Updates.set("folderIds", folderIds),
                Updates.set("updatedAt", System.currentTimeMillis())
            )
        )
    }

    fun delete(userId:String,userMediaId:String){
        collection.deleteOne(
            and(
                UserMediaItem::userId eq userId,
                UserMediaItem::id eq userMediaId
            )
        )
    }

    fun findByMediaIdAndUserId(userId: String, mediaId: String): UserMediaItem? {
        return collection.findOne(
            and(
                UserMediaItem::userId eq userId,
                UserMediaItem::mediaId eq mediaId
            )
        )
    }

    fun removeFolderIdFromAllUserMedia(userId: String, folderId: String) {
        collection.updateMany(
            UserMediaItem::userId eq userId,
            Updates.combine(
                pull(UserMediaItem::folderIds, folderId),
                Updates.set("updatedAt", System.currentTimeMillis())
            )
        )
    }
}
