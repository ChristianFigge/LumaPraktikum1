package com.example.lumapraktikum1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.lumapraktikum1.ui.composables.system.MyNavModal
import com.example.lumapraktikum1.ui.theme.LumaPraktikum1Theme


class MainActivity : ComponentActivity() {

    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>

    private val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val locationPermissionsGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initLocationPermissionRequest()

        /* Erteilte Permissions checken und ggf. mit dem Launcher requesten: */
// TODO shouldShowRequestPermissionRationale(permission) abfragen?
// s. https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code
        if (!hasAllLocationPermissions(this, LOCATION_PERMISSIONS)) {
            Log.i("LocPermissions", "Check: Location Permissions denied.")
            locationPermissionRequest.launch(LOCATION_PERMISSIONS)
        } else {
            Log.i("LocPermissions", "Check: Location Permissions granted.")
        }

        setContent {
            LumaPraktikum1Theme {
                PermissionComposible()
            }
        }
    }

    fun initLocationPermissionRequest() {
        /* Request Launcher definieren: */
        locationPermissionRequest =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { isGranted: Map<String, @JvmSuppressWildcards Boolean> ->
                if (isGranted.containsValue(true)) {
                    // Mindestens eine location permission erteilt (coarse oder fine)
                    locationPermissionsGranted.value = true
                    Log.i("LocPermissions", "Request: Location Permissions granted: $isGranted")
                } else {
                    // Keine permission erteilt
                    locationPermissionsGranted.value = false
                    //locationListenersAndData.forEach { (_, listenerAndData) ->
                    //    listenerAndData.second.value =
                    //        "Standortzugriff verweigert\n(Änderbar in Einstellungen)"
                    //}
                    Log.i("LocPermissions", "Request: Location Permissions denied.")
                }
            }
    }

    @Composable
    fun PermissionComposible() {
        //var permissionGranted by remember { mutableStateOf(locationPermissionsGranted) }

        //LaunchedEffect(locationPermissionsGranted) {
        //    permissionGranted = locationPermissionsGranted
        //    println("permissionGranted " + permissionGranted)
        //}

        locationPermissionsGranted.value = hasAllLocationPermissions(this, LOCATION_PERMISSIONS)

        if (locationPermissionsGranted.value) {
            MyNavModal()
        } else {
            Column(
                modifier = Modifier
                    .padding(30.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Für diese App werden Lokalisierungs Berechtigungen benötigt.",
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                        with(intent) {
                            data = Uri.fromParts("package", getPackageName(), null)
                            addCategory(CATEGORY_DEFAULT)
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                            addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                        startActivity(intent)
                    },
                ) { Text("Einstellungen öffnen") }
            }
        }

    }

}


private fun hasAllLocationPermissions(ctx: Context, LOCATION_PERMISSIONS: Array<String>): Boolean {
    LOCATION_PERMISSIONS.forEach {
        if (ActivityCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_DENIED) {
            return false
        }
    }
    return true
}


