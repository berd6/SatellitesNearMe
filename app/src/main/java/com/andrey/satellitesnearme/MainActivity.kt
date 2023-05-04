package com.andrey.satellitesnearme

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.andrey.satellitesnearme.ui.theme.SatellitesNearMeTheme
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.maps.android.compose.*
import com.google.maps.android.ui.IconGenerator
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*


class MainActivity : ComponentActivity() {

    var positionOne : MutableState<LatLng?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var position1 : MutableState<LatLng?> = mutableStateOf(null)

        //


        var deviceLatLng =LatLng(22.00,22.00)




        setContent {
            SatellitesNearMeTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    Scaffold(topBar = { } ,
                        bottomBar = {},
                        floatingActionButtonPosition = FabPosition.End,
                        floatingActionButton = {
                            FloatingActionButton(


                                onClick = {

                                    CoroutineScope(Dispatchers.IO).launch {
                                        withContext(Dispatchers.Main) {
                                            try {
                                                val response = Network.httpClient.request {
                                                    url(Network.ABOVE_URL)
                                                    method = HttpMethod.Get
                                                    contentType(ContentType.Application.Json)
                                                }
                                                val builder = GsonBuilder()
                                                val gson = builder.create()

                                                Log.i("SatInfJSON","!!!" + response.body<String>().toString() )
                                                val data  : SatellitesModel = response.body()
                                                Log.i("SatInfJSON", "!!" + data.above[0].satname)
                                                Log.i("SatInfJSON", "!" + data.toString())

                                                    /* Отсюда и далее Coroutine не выполняется
                                                    * !!!!!!
                                                    * !!!!!
                                                    * !!!!
                                                    * !!!
                                                    * */

                                                val markerPosition = gson.fromJson(data.above.toString(),SatellitesModel::class.java)
                                                positionOne.value = LatLng(markerPosition.above[0].satlat,markerPosition.above[0].satlng)
                                                Log.i("PositionOne", positionOne.value.toString())

                                                Log.i("PositionOne", "positionOne.value.toString()")
                                                position1.value = LatLng(markerPosition.above[0].satlat,markerPosition.above[0].satlng)






                                                //val data  : List<SatellitesModel> = gson.fromJson(response.body(), SatellitesModel::javaClass)
                                                //val data: List<SatellitesModel> =gson.fromJson(response.body(),SatellitesModel::class.java)
                                                //Log.i("SAT", )

                                                positionOne.value = LatLng(22.00 ,25.00)
                                                Log.i("POS", positionOne.value!!.longitude.toString())

                                                cancel()
                                            }catch(err: Exception){
                                                withContext(Dispatchers.Main){
                                                    // Toast.makeText(requireContext(this@),"Ошибка сервера!", Toast.LENGTH_LONG).show()

                                                }

                                            }
                                        }
                                    }

                                }
                            ) {
                                Icon(Icons.Filled.Refresh,null)
                            }
                        }
                        , content = { it


                            val cameraPositionState = rememberCameraPositionState {
                                position =CameraPosition.fromLatLngZoom(deviceLatLng,6f)

                            }

                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
                            ) {
                                Marker(state = MarkerState(deviceLatLng))
                                /*
                                SatelliteMarker(LatLng(21.0,22.0), "St", "Sn", "30.8646", "74.6425")
                                SatelliteMarker(LatLng(23.4,25.0), "SAR LUPE 2", "31797", "73.3268","253.715")
                                SatelliteMarker(LatLng(26.0,22.3), "St", "Sn", "47.2395", "1354.3743")
                                SatelliteMarker(LatLng(23.6,23.0), "S2t", "n_sas", "79.7457","253.615")
                                SatelliteMarker(LatLng(27.0,28.0), "St", "Sn", "84.3486", "74.9538")

                                 */

                                var satOne =SatelliteMarker( position = positionOne)
                                Log.i("Posit", satOne.toString())
                                var satTwo =SatelliteMarker(position =remember{mutableStateOf(null)}  )
                                var satthree =SatelliteMarker(position =remember{mutableStateOf(null)} )
                                var satFour =SatelliteMarker(position =remember{mutableStateOf(null)} )
                                var satFive =SatelliteMarker(position =remember{mutableStateOf(null)} )

                            }

                        })


                    Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                         {
                            
                        }
                    }


                }

            }
        }
    }



@Composable
fun SatelliteMarker(position: MutableState<LatLng?> = remember{ mutableStateOf(null)}, title: String = "", snippet: String = "", altitude: String = "", azimuth: String = "", context: Context = applicationContext
){
    val iconFactory = IconGenerator(context)


    if(position!=null){
    Marker(
        state = rememberMarkerState(position = position.value?: LatLng(0.0,0.0)),
        title = title,
        snippet = "Satellite NORAD id: $snippet",
        icon = BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("alt: $altitude° az: $azimuth°")),

    )
    }
}

}

