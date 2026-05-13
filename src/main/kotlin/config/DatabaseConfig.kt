package com.example.config

import com.example.UserFolder.model.UserFolder
import com.example.UserMedia.model.UserMediaItem
import com.example.media.model.MediaItem
import com.example.user.User
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
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

    fun users() = database.getCollection<User>()
    fun userMediaItems() = database.getCollection<UserMediaItem>()
    fun userFolders() = database.getCollection<UserFolder>()
    fun globalMediaItems() = database.getCollection<MediaItem>()

    fun close() = client.close()
}



fun Application.configureDatabase() {
    DatabaseConfig.init()

    environment.monitor.subscribe(ApplicationStopping) {
        DatabaseConfig.close()
    }

    log.info("Database connected successfully with KMongo")
}
