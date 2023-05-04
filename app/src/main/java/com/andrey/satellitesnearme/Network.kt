package com.andrey.satellitesnearme

import com.google.android.gms.maps.model.LatLng
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

//position либо вводить в поле, которое потом добавлю, либо по GPS (через Fused Location не получилось реализовать)
//searchRad пока не планируется менять, API KEY желательно, как и с картами, получать по ссылке, а не напрямую
object Network {
    var position = LatLng(22.0, 22.0)
    var searchRad = 70
    val BASE_URL = "https://api.n2yo.com/rest/v1/satellite/"
    val ABOVE_URL = "$BASE_URL/above/${position.latitude}/${position.longitude}/10/$searchRad/1/&apiKey=PMBGAU-SVL5P6-SY7D7M-50MN"

    val httpClient= HttpClient(OkHttp){
        install(ContentNegotiation){
            gson()
        }

    }
}