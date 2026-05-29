package com.example.media

import com.example.config.DatabaseConfig
import com.example.media.dto.UpdateMediaRequest
import com.example.media.model.MediaItem
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Sorts
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.`in`
import org.litote.kmongo.limit
import org.litote.kmongo.skip

class MediaCatalogRepository {

    private val collection = DatabaseConfig.globalMediaItems()

    fun findAllByTitle(title: String): List<MediaItem> {
        return collection.find(MediaItem::title eq title).toList()
    }

    fun findByIds(ids: List<String>): List<MediaItem> {
        if (ids.isEmpty()) return emptyList()

        val requestedIds = ids.distinct()
        val items = collection.find(MediaItem::id `in` requestedIds).toList()
        val itemsById = items.associateBy { it.id }

        return ids.mapNotNull { itemsById[it] }
    }

    fun findPage(limit: Int, offset: Int): List<MediaItem> {
        if (limit <= 0 || offset < 0) return emptyList()
        return collection.find()
            .skip(offset)
            .limit(limit)
            .toList()
    }

    fun findNewestPage(limit: Int, offset: Int): List<MediaItem> {
        if (limit <= 0 || offset < 0) return emptyList()
        return collection.find()
            .sort(Sorts.descending("createdAt"))
            .skip(offset)
            .limit(limit)
            .toList()
    }

    fun save(mediaItem: MediaItem){
        collection.insertOne(mediaItem)
    }

    fun findById(mediaId: String): MediaItem? {
        val byId = collection.findOne(MediaItem::id eq mediaId)
        if (byId != null) return byId

        if (!ObjectId.isValid(mediaId)) return null

        val objectId = ObjectId(mediaId)
        val byObjectId = collection.findOne(Filters.eq("_id", objectId)) ?: return null

        if (byObjectId.id != mediaId) {
            collection.updateOne(
                Filters.eq("_id", objectId),
                Updates.set("id", mediaId)
            )
        }

        return byObjectId
    }

    fun update(
        mediaId: String,
        request: UpdateMediaRequest
    ) {
        val updates = mutableListOf<org.bson.conversions.Bson>()

        request.title?.let {
            updates.add(Updates.set("title", it.trim()))
        }

        request.description?.let {
            updates.add(Updates.set("description", it.trim()))
        }

        request.posterUrl?.let {
            updates.add(Updates.set("posterUrl", it.trim()))
        }

        request.genres?.let {
            updates.add(Updates.set("genres", it.map(String::trim).filter(String::isNotBlank)))
        }

        request.mediaStatus?.let {
            updates.add(Updates.set("mediaStatus", it))
        }

        if (updates.isEmpty()) return

        updates.add(Updates.set("updatedAt", System.currentTimeMillis()))

        collection.updateOne(
            MediaItem::id eq mediaId,
            Updates.combine(updates)
        )
    }

    fun delete(mediaId: String) {
        collection.deleteOne(MediaItem::id eq mediaId)
    }

    fun adjustUserRating(mediaId: String, ratingDelta: Double, countDelta: Int) {
        val updates = mutableListOf<org.bson.conversions.Bson>()

        if (ratingDelta != 0.0) {
            updates.add(Updates.inc("userRatingSum", ratingDelta))
        }

        if (countDelta != 0) {
            updates.add(Updates.inc("userRatingCount", countDelta))
        }

        if (updates.isEmpty()) return

        updates.add(Updates.set("updatedAt", System.currentTimeMillis()))

        collection.updateOne(
            MediaItem::id eq mediaId,
            Updates.combine(updates)
        )
    }
}
