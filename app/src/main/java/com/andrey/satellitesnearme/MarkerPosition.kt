package com.andrey.satellitesnearme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.android.gms.maps.model.LatLng

data class MarkerPosition (
    var positionOne : MutableState<LatLng?> = mutableStateOf(null),
    val position2 : MutableState<LatLng?>,
    val position3 : MutableState<LatLng?>
)


