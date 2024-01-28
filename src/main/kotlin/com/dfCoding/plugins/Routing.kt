package com.dfCoding.plugins

import com.dfCoding.models.TicTacToeGame
import com.dfCoding.socket
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(game: TicTacToeGame) {
    routing {
        socket(game)
//        get("/") {
//            call.respondText("Hello World!")
//        }
    }
}
