package demeter.gabor.tracker

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import demeter.gabor.tracker.R.id.loginAs
import demeter.gabor.tracker.Util.BaseActivity
import demeter.gabor.tracker.Util.Constants
import demeter.gabor.tracker.adapters.UserAdapter
import demeter.gabor.tracker.models.MyLocation
import demeter.gabor.tracker.models.User
import demeter.gabor.tracker.services.LocationService
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class MainActivity : BaseActivity() {



    private lateinit var  recyclerViewUsers: RecyclerView
    private lateinit var usersAdapter: UserAdapter


    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button

    //Database reference manage Users
    private lateinit var mUsersDatabase: DatabaseReference

    //Database reference manage Location
    private lateinit var mLocationReference: DatabaseReference
    private lateinit var mLastKnownLocation: DatabaseReference
    private lateinit var mLastLocationQuery: Query


    //EVENT Listenres
    private lateinit var locationListener: ValueEventListener
    private lateinit var userListener: ChildEventListener
    private lateinit var loadLastknownLocation: ValueEventListener
    private lateinit var imagesListener: ChildEventListener

    private var isSaveLastData: Boolean = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //DATABASES REFERENCE
        mUsersDatabase = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        mLocationReference = FirebaseDatabase.getInstance().getReference(Constants.LOCATIONS_REF)
        mLastKnownLocation = FirebaseDatabase.getInstance().getReference(Constants.LAST_KNOWN_LOCATIONS_REF)

        mImages = FirebaseDatabase.getInstance().getReference(Constants.IMAGES_REF) //filed declared in baseActivity


        mLastLocationQuery = mLocationReference.orderByKey().limitToLast(1)



        //CLOUD MESSAGING

        FirebaseMessaging.getInstance().subscribeToTopic(Constants.PUSH_NOTIFICATIONS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW))
        }


        //STORRAGE REFERENCE
        mStorageRef = FirebaseStorage.getInstance().reference //filed declared in baseActivity
        mImagesRefecence = mStorageRef.child(Constants.IMAGES_STORAGR_REF) //filed declared in baseActivity

        //Create Listeners
        createUserListener()
        createLocationListener()
        createImageListener()

        //SET VIEWS


        startBtn = findViewById<Button>(R.id.startService)
        stopBtn = findViewById<Button>(R.id.stopService)

        val viewManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        usersAdapter = UserAdapter(applicationContext)
        recyclerViewUsers = findViewById<RecyclerView>(R.id.recyclerViewUsers).apply {
            layoutManager = viewManager
            adapter = usersAdapter
        }





        //INIT DATABASES CHANGES LISTENER
        mUsersDatabase.addChildEventListener(userListener)


        //INIT variables

        //loginAs.text = FirebaseAuth.getInstance().currentUser!!.email

        //CHECK PERMISSONS
        if (!runtime_permissions())
            enable_buttons()

    }


    private fun createImageListener() {

        imagesListener = object : ChildEventListener {
            override fun onCancelled(error: DatabaseError?) {
                Log.d(TAG, "image Listener onCancelled")
            }

            override fun onChildMoved(p0: DataSnapshot?, p1: String?) {
                Log.d(TAG, "image Listener onChildMoved")
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot?, p1: String?) {
                Log.d(TAG, "imagelistener change: " + dataSnapshot.toString())
                val imageURL = dataSnapshot!!.getValue(String::class.java)
                usersAdapter.updateProfileImage(dataSnapshot.getKey(), imageURL!!)
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot?, p1: String?) {
                val imageURL = dataSnapshot!!.getValue(String::class.java)
                usersAdapter.updateProfileImage(dataSnapshot.getKey(), imageURL!!)
            }

            override fun onChildRemoved(p0: DataSnapshot?) {
                Log.d(TAG, "image Listener onChildRemoved")
            }

        }

    }

    private fun createUserListener() {
        userListener = object : ChildEventListener {
            override fun onCancelled(dataSnapshot: DatabaseError?) {
                Log.d(TAG, "userListener onCancelled" + dataSnapshot.toString())
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot?, p1: String?) {
                Log.d(TAG, "userListener onChildMoved" + dataSnapshot.toString())
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot?, p1: String?) {
                Log.d(TAG, "userListener onChildChanged" + dataSnapshot.toString())
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot?, previousChildName: String?) {
                val newUser = dataSnapshot?.getValue(User::class.java)
                Log.d(TAG, "userListener :" + newUser!!.toString() + " key: " + dataSnapshot.key)
                usersAdapter.addUser(newUser, dataSnapshot.key)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot?) {
                Log.d(TAG, "userListener onChildRemoved" + dataSnapshot.toString())
            }

        }
    }

    private fun createLocationListener() {
        locationListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var loc: MyLocation?
                for (data in dataSnapshot.children) {
                    Log.d(TAG, "Location Query" + dataSnapshot.toString())
                    loc = data.getValue(MyLocation::class.java)
                    usersAdapter.updateLastLocation(loc)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }

        loadLastknownLocation = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "LastKnownLoc out " + dataSnapshot.toString())

                val locations = ArrayList<MyLocation>()
                //
                //                Log.d(TAG, "LastKnownLoc: "+ dataSnapshot);
                //                Log.d(TAG, "LastKnownLoc  value: "+ dataSnapshot.getValue(MyLocation.class));
                //                locations.add(dataSnapshot.getValue(MyLocation.class));

                for (data in dataSnapshot.children) {


                    locations.add(data.getValue(MyLocation::class.java)!!)
                }
                if (!locations.isEmpty()) {
                    Log.d(TAG, "updateLastLocation: $locations")
                    usersAdapter!!.updateLastLocation(locations)
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
    }


    override fun onStart() {
        super.onStart()
        Log.d(TAG, "eletciklus  ONSART")

        //ADD DATABASES CHANGES LISTENER
        mLastKnownLocation.addListenerForSingleValueEvent(loadLastknownLocation)
        mLastLocationQuery.addValueEventListener(locationListener)
        mImages.addChildEventListener(imagesListener)


    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "eletciklus ONRESUME")

        isSaveLastData = false
    }

    override fun onStop() {
        super.onStop()

        mLocationReference.removeEventListener(locationListener)
        mLastKnownLocation.removeEventListener(loadLastknownLocation)


        saveLastLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        mImages.removeEventListener(imagesListener!!)
        mUsersDatabase.removeEventListener(userListener)

        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.PUSH_NOTIFICATIONS)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i = item.itemId
        if (i == R.id.action_logout) {
            saveLastLocation()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return true
        } else if (i == R.id.uploadProfileImg) {
            selectImageFromGallery()

            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private fun enable_buttons() {




        startBtn.setOnClickListener {
            val i = Intent(applicationContext, LocationService::class.java)
            startService(i)
            sendFCMNotificationToOthers()
        }

        stopBtn.setOnClickListener {
            val i = Intent(applicationContext, LocationService::class.java)
            stopService(i)
        }

    }

    private fun sendFCMNotificationToOthers() {
        val thread = Thread(Runnable {
            var conn: HttpURLConnection? = null
            try {
                Log.d(TAG, "MY Post thread started")
                Thread.sleep(300)
                val url = URL("https://gcm-http.googleapis.com/gcm/send")

                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", getString(R.string.authorization_key))
                conn.doOutput = true
                conn.doInput = true

                val jsonObject = JSONObject()
                jsonObject.put("to", getString(R.string.notificatoin_path))

                val dataChield = JSONObject()
                dataChield.put(Constants.CURRENTUSER_UID, uid)
                val currentUser = usersAdapter.getUserbyId(uid)
                dataChield.put(Constants.USERNAME, currentUser?.username)
                dataChield.put(Constants.LATITUDE, currentUser?.lastLocation!!.latitude)
                dataChield.put(Constants.LONGITUDE, currentUser?.lastLocation!!.longitude)

                jsonObject.put("data", dataChield)


                val notificationChield = JSONObject()

                notificationChield.put("title", "Help for, " + currentUser.username!!)
                notificationChield.put("text", "View his position on Map")

                jsonObject.put("notification", notificationChield)

                Log.i("JSON", jsonObject.toString())

                val os = DataOutputStream(conn.outputStream)
                //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                os.writeBytes(jsonObject.toString())

                os.flush()
                os.close()

                Log.i("STATUS", conn.responseCode.toString())

                Log.i("MSG", conn.responseMessage)

                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (conn != null) {
                    conn.disconnect()
                }
            }
        })

        thread.start()
    }

    private fun runtime_permissions(): Boolean {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 100)

            return true
        }
        return false
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enable_buttons()
            } else {
                runtime_permissions()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {

            var bitmap: Bitmap? = null
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            uploadImagetoFireBase(bitmap!!)

        }
    }


    private fun saveLastLocation() {
        Log.d(TAG, "saveLastLocation: $isSaveLastData")
        if (!isSaveLastData) {
            mLastKnownLocation.child(uid).setValue(usersAdapter.getUserbyId(uid)!!.lastLocation)
            isSaveLastData = true
        }

    }

    companion object {

        private val TAG = MainActivity::class.java.name
    }


}

