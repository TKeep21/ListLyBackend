package com.example.config

import com.example.UserFolder.model.UserFolder
import com.example.UserMedia.model.UserMediaItem
import com.example.media.model.MediaItem
import com.example.user.User
import org.litote.kmongo.KMongo
import com.mongodb.MongoCommandException
import org.litote.kmongo.getCollection
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log

object DatabaseConfig {

    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase

    fun init() {
        val mongoUri = System.getenv("MONGO_URI")
            ?: error("MONGO_URI is not set")

        println(">>> Mongo URI: $mongoUri")

        client = KMongo.createClient(mongoUri)
        database = client.getDatabase("ListlyDB")
    }

    fun users() = database.getCollection<User>("users")
    fun userMediaItems() = database.getCollection<UserMediaItem>("userMediaItems")
    fun userFolders() = database.getCollection<UserFolder>("userFolders")
    fun globalMediaItems() = database.getCollection<MediaItem>("globalMediaItems")

    fun ensureIndexes() {
        ensureIndex(
            collectionName = "users",
            indexName = "ux_users_login",
            indexDefinition = Indexes.ascending("login"),
            options = IndexOptions().unique(true).name("ux_users_login")
        )

        ensureIndex(
            collectionName = "userMediaItems",
            indexName = "ux_userMedia_userId_mediaId",
            indexDefinition = Indexes.compoundIndex(
                Indexes.ascending("userId"),
                Indexes.ascending("mediaId")
            ),
            options = IndexOptions().unique(true).name("ux_userMedia_userId_mediaId")
        )

        ensureIndex(
            collectionName = "globalMediaItems",
            indexName = "ix_media_title",
            indexDefinition = Indexes.ascending("title"),
            options = IndexOptions().name("ix_media_title")
        )

        ensureIndex(
            collectionName = "globalMediaItems",
            indexName = "ix_media_mediaType",
            indexDefinition = Indexes.ascending("mediaType"),
            options = IndexOptions().name("ix_media_mediaType")
        )

        ensureIndex(
            collectionName = "userFolders",
            indexName = "ix_userFolders_userId",
            indexDefinition = Indexes.ascending("userId"),
            options = IndexOptions().name("ix_userFolders_userId")
        )
    }

    private fun ensureIndex(
        collectionName: String,
        indexName: String,
        indexDefinition: org.bson.conversions.Bson,
        options: IndexOptions
    ) {
        try {
            database.getCollection<org.bson.Document>(collectionName)
                .createIndex(indexDefinition, options)
        } catch (ex: MongoCommandException) {
            // Keep app startup resilient if existing data violates a newly-added unique index.
            println(">>> Failed to create index '$indexName' on '$collectionName': ${ex.errorMessage}")
        }
    }

    fun close() = client.close()
}



fun Application.configureDatabase() {
    DatabaseConfig.init()
    DatabaseConfig.ensureIndexes()

    environment.monitor.subscribe(ApplicationStopping) {
        DatabaseConfig.close()
    }

    log.info("Database connected successfully with KMongo, indexes ensured")
}
