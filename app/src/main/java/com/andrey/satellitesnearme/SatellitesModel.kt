package com.andrey.satellitesnearme

data class SatellitesModel (
            val above : List<Satellites>
        )
data class Satellites(
    val satid : Int,
    val satname : String,
    val satlat : Double,
    val satlng : Double,
    val satalt : Double
)

data class SatellitesResults(
    val satid : Int,
    val satname : String,
    val satlat : Double,
    val satlng : Double,
    val satalt : Double,
    var azimuth: Double,
    var altitude: Double
)