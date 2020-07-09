package com.techprimers.vertx

import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    println("Hello, World")

    val vertx = Vertx.vertx()
    val httpServer = vertx.createHttpServer()

    val router = Router.router(vertx)

    var count = 0

    var options = CircuitBreakerOptions()
    options.maxFailures = 5
    options.timeout = 60000
    options.isFallbackOnFailure = true
    options.resetTimeout = 60000

    val breaker = CircuitBreaker.create("my-circuit-breaker", vertx, options)

    router.get("/")
            .handler { routingContext ->
                val response = routingContext.response()
                println("Fetching game")
                breaker
                        .execute<Any> { future ->
                            if (count < 100) {
                                println("HTTP")
                                future.fail("Error occurred")
                            } else {
                                future.complete(count)
                            }
                        }
                        .onSuccess() { ar ->
                            response.putHeader("content-type", "application/json")
                                    .setChunked(true)
                                    .write(Json.encodePrettily(ResponseObj(ar.toString())))
                                    .end()
                        }
                        .onFailure() { throwable ->
                            response.putHeader("content-type", "application/json")
                                    .setChunked(true)
                                    .setStatusCode(500)
                                    .write(Json.encodePrettily(mapOf("res" to throwable.message)))
                                    .end()
                        }


            }

    httpServer
            .requestHandler(router::accept)
            .listen(8091)
}

data class ResponseObj(var name: String = "")
