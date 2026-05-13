package com.example.user

import com.example.config.DatabaseConfig
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

class UserRepository {

    private val collection = DatabaseConfig.users()

    fun findByLogin( login:String):User?{
        return collection.findOne(User::login eq login)
    }

    fun save(user:User){
         collection.insertOne(user)
    }






}