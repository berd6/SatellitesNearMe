package com.andrey.satellitesnearme

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*

class Network (lat: Double,  lon: Double) {
    private var searchRad = 90 //область поиска над устройством. 0 - над ним, 90 - вплоть до горизонта
    private val BASE_URL = "https://api.n2yo.com/rest/v1/satellite/"
    private val ABOVE_URL = "$BASE_URL/above/${lat}/${lon}/10/$searchRad/1/&apiKey=PMBGAU-SVL5P6-SY7D7M-50MN"

    private val httpClient= HttpClient(OkHttp){
        install(ContentNegotiation){
            gson()
        }
    }

    suspend fun getSattelites(): SatellitesModel {
        val response = httpClient.request(ABOVE_URL) { //отправляем запрос на API
            method = HttpMethod.Get
            contentType(ContentType.Application.Json)
        }
        return response.body()

    }



}