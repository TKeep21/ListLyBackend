package com.example.user

import org.bson.types.ObjectId

class User(  val _id: ObjectId = ObjectId(),
             val login: String,
             val passwordHash: String,
             val role: UserRole = UserRole.USER)
