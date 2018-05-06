package demeter.gabor.tracker

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import demeter.gabor.tracker.Util.Constants
import demeter.gabor.tracker.models.MyLocation
import java.util.*

class UserMapActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap


    private lateinit var mLocationReference: DatabaseReference
    private lateinit var mLastKnownLocation: DatabaseReference

    private  lateinit var mLastLocationQuery: Query

    private lateinit var locationLisener: ValueEventListener

    private lateinit var uId: String
    private lateinit var username: String
    private  var currentLongitude: Double? = null
    private  var currentLatitude: Double? = null

    private var position = 0
    private lateinit var markers: Stack<MarkerOptions>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "eletciklus ONCREATE")
        setContentView(R.layout.activity_user_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        //DATABASES REFERENCE

        mLocationReference = FirebaseDatabase.getInstance().getReference(Constants.LOCATIONS_REF)
        mLastKnownLocation = FirebaseDatabase.getInstance().getReference(Constants.LAST_KNOWN_LOCATIONS_REF)

        mLastLocationQuery = mLocationReference.orderByKey().limitToLast(1)


        markers = Stack()

        //Create Listeners
        createLocationListerner()
        mLastLocationQuery.addValueEventListener(locationLisener)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "eletciklus ONSTART")

        //GET DATA FROM INENT
        currentLongitude = intent.getDoubleExtra(Constants.LONGITUDE, 0.0)
        currentLatitude = intent.getDoubleExtra(Constants.LATITUDE, 0.0)
        username = intent.getStringExtra(Constants.USERNAME)
        uId = intent.getStringExtra(Constants.CURRENTUSER_UID)
    }

    private fun createLocationListerner() {
        locationLisener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {


                for (data in dataSnapshot.children) {
                    Log.d(TAG, "Location Query" + dataSnapshot.toString())
                    val loc = data.getValue(MyLocation::class.java)

                    Log.d(TAG, "Location loc" + loc.toString())
                    Log.d(TAG, "uid " + uId.toString())

                    if (loc != null && uId == loc.userId) {
                        mMap.clear()
                        animateMarker(position++, LatLng(currentLatitude!!, currentLongitude!!), LatLng(loc.latitude!!, loc.longitude!!), false)
                        currentLongitude = loc.longitude
                        currentLatitude = loc.latitude
                    }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val currentUserPosition = LatLng(currentLatitude!!, currentLongitude!!)

        mMap.addMarker(MarkerOptions().position(currentUserPosition).title(username))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentUserPosition))

    }


    //This methos is used to move the marker of each car smoothly when there are any updates of their position
    private fun animateMarker(position: Int, startPosition: LatLng, toPosition: LatLng,  hideMarker: Boolean) {


        val myMarker = MarkerOptions()
                .position(startPosition)
                .title(username)


        if (!markers.isEmpty()) {
            markers.pop().visible(false)
        }
        markers.push(myMarker)
        mMap.clear()
        val marker = mMap.addMarker(myMarker)


        val handler = Handler()
        val start = SystemClock.uptimeMillis()

        val duration: Long = 1000
        val interpolator = LinearInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val lng = t * toPosition.longitude + (1 - t) * startPosition.longitude
                val lat = t * toPosition.latitude + (1 - t) * startPosition.latitude
                val newPosition = LatLng(lat, lng)
                marker.position = newPosition
                mMap.moveCamera(CameraUpdateFactory.newLatLng(newPosition))
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                } else {
                    marker.isVisible = !hideMarker
                }
            }
        })
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "eletciklus ONPAUSE")
        mLastLocationQuery.removeEventListener(locationLisener)
        mLastKnownLocation.child(uId).setValue(MyLocation(this.currentLatitude, this.currentLongitude, uId))
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "eletciklus ONSTOP")

    }

    companion object {

        private val TAG = UserMapActivity::class.java.name
    }

}
