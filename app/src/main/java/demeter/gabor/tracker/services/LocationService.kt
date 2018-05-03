package demeter.gabor.tracker.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import demeter.gabor.tracker.Util.Constants
import demeter.gabor.tracker.models.MyLocation

class LocationService : Service() {

    private var listener: LocationListener? = null
    private var locationManager: LocationManager? = null
    private var mAuth: FirebaseAuth? = null
    private var currentUser: FirebaseUser? = null
    private var database: FirebaseDatabase? = null
    private var locationsReference: DatabaseReference? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {

        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth!!.currentUser

        database = FirebaseDatabase.getInstance()
        locationsReference = database!!.getReference(Constants.LOCATIONS_REF)

        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {


                if (currentUser != null) {
                    writeLocatoinToFirebase(location)
                }
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {

            }

            override fun onProviderEnabled(s: String) {

            }

            override fun onProviderDisabled(s: String) {
                val i = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            }
        }

        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager


        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 200f, listener)

    }


    override fun onDestroy() {
        super.onDestroy()
        if (locationManager != null) {

            locationManager!!.removeUpdates(listener)
        }
    }

    private fun writeLocatoinToFirebase(location: Location) {
        locationsReference!!.push().setValue(MyLocation(location, currentUser!!.uid))
    }
}
