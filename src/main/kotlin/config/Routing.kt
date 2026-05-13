package com.example.config


import com.example.routes.AuthRouting
import com.example.routes.UserFolderRouting
import com.example.routes.GlobalMediaRoutes
import com.example.routes.GlobalMediaRouting
import com.example.routes.UserMediaRouting
import com.example.routes.searchRoutes
import com.example.search.service.SearchService
import io.ktor.server.application.Application
import io.ktor.server.routing.Route

fun Application.configureRouting(){
    AuthRouting()
    UserMediaRouting()
    UserFolderRouting()
    GlobalMediaRoutes()
    searchRoutes()
}
