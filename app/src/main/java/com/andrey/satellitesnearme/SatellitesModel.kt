package com.andrey.satellitesnearme

data class SatellitesModel (
            val above : List<Satellites>
        )
data class RequestInfo(
    val satcount : Int
)
data class Satellites(
    val satid : Int,
    val satname : String,
    val satlat : Double,
    val satlng : Double,
    val satalt : Double
)