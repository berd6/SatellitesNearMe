package com.andrey.satellitesnearme

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andrey.satellitesnearme.ui.theme.SatellitesNearMeTheme
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.ui.IconGenerator
import kotlinx.coroutines.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SatellitesNearMeTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val deviceLatLng =LatLng(59.944841330703866, 30.35195106677977) //местоположение устройства
                    val allSatellitesInfo = MutableList(0) {SatellitesResults(0,   "",0.0,0.0,0.0,0.0,0.0)}
                    val mutableSatellitesData = remember { mutableStateOf(allSatellitesInfo) }

                    Scaffold(
                        topBar = { } ,
                        bottomBar = {},
                        floatingActionButtonPosition = FabPosition.End,
                        floatingActionButton = {


                            FloatingActionButton(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        withContext(Dispatchers.Main) {
                                            try {
                                                val myPosition = Calculations.Point(latitude = deviceLatLng.latitude, longitude = deviceLatLng.longitude, elevation = 5.0, radius=0.0)
                                                val network = Network(59.944841330703866, 30.35195106677977)
                                                val satellitesList : SatellitesModel = network.getSattelites()
                                                val calculations = Calculations(satellitesList = satellitesList, myPosition = myPosition)
                                                mutableSatellitesData.value = calculations.completeSatelliteslist()
                                                cancel()
                                            }catch(err: Exception){
                                                withContext(Dispatchers.Main){
                                                    Toast.makeText(applicationContext,"Ошибка сетевого запроса!", Toast.LENGTH_LONG).show()

                                                }
                                            }
                                        }
                                    }
                                },
                                backgroundColor = Color(0xFFC08020),
                                modifier = Modifier.padding(4.dp)

                            ) {
                                Row {
                                    Text(mutableSatellitesData.value.size.toString())
                                    Icon(Icons.Filled.Refresh,null)
                                }
                            }
                        }
                        , content = {it
                            val cameraPositionState = rememberCameraPositionState {
                                position =CameraPosition.fromLatLngZoom(deviceLatLng,1f)
                            }
                            GoogleMap(
                                modifier  = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true, mapToolbarEnabled = false)
                            ) {
                                Marker(state = MarkerState(deviceLatLng))   //маркер местоположения
                                for (oneSatellite in mutableSatellitesData.value){  //перебираем список спутников, создавая для каждого из них маркер
                                        SatelliteMarker(
                                            LatLng(oneSatellite.satlat, oneSatellite.satlng),
                                            oneSatellite.satname,
                                            oneSatellite.satid.toString(),
                                            String.format("%.2f", oneSatellite.altitude),
                                            String.format("%.2f", oneSatellite.azimuth))
                                }
                            }
                        })



                }

            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        finish()
        startActivity(intent)
    }

@Composable
fun SatelliteMarker(position: LatLng, title: String = "", snippet: String = "", altitude: String = "", azimuth: String = "", context: Context = applicationContext
){          //создаём модифицированный маркер для спутников
    val iconFactory = IconGenerator(context)
    Marker(
        state = rememberMarkerState(position = position),
        title = title,
        snippet = "NORAD id спутника: $snippet",

        icon = BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("alt: $altitude.t.° az: $azimuth°")),

    )
}


}

