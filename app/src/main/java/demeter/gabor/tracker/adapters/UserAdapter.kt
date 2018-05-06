package demeter.gabor.tracker.adapters

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import demeter.gabor.tracker.R
import demeter.gabor.tracker.UserMapActivity
import demeter.gabor.tracker.Util.Constants
import demeter.gabor.tracker.models.MyLocation
import demeter.gabor.tracker.models.User
import kotlinx.android.synthetic.main.user_item.view.*
import java.util.*

class UserAdapter(private val context: Context) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    private val userList: MutableList<User>
    private val userMap: MutableMap<String, User>
    private var lastPosition = -1

    init {
        this.userList = ArrayList()
        this.userMap = HashMap()
    }

    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val myView = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.user_item, viewGroup, false)
        return ViewHolder(myView)

    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val tempUser = userList[position]
        viewHolder.itemView.tvUserName.text = tempUser.username
        viewHolder.itemView.tvUserEmail.text = tempUser.email

        if (tempUser.lastLocation == null) {
            viewHolder.itemView.tvLongitude.text = context.getString(R.string.unknown)
            viewHolder.itemView.tvLatitude.text = context.getString(R.string.unknown)
            viewHolder.itemView.tvAddress.text = context.getString(R.string.unknown)
        } else {
            viewHolder.itemView.tvLongitude.text = tempUser.lastLocation!!.longitude.toString()
            viewHolder.itemView.tvLatitude.text = tempUser.lastLocation!!.latitude.toString()
            viewHolder.itemView.tvAddress.text = getAddressFromMyLocation(tempUser)
        }

        viewHolder.itemView.setOnClickListener {
            if (context != null) {
                val showUserdata = Intent(context, UserMapActivity::class.java)
                showUserdata.putExtra(Constants.LONGITUDE, tempUser.lastLocation!!.longitude)
                showUserdata.putExtra(Constants.LATITUDE, tempUser.lastLocation!!.latitude)
                showUserdata.putExtra(Constants.USERNAME, tempUser.username)
                showUserdata.putExtra(Constants.CURRENTUSER_UID, tempUser.getuId())

                context.startActivity(showUserdata)
            }
        }

        //SET PROFILE IMAGE
        if (!TextUtils.isEmpty(tempUser.profileImageURL)) {
            Glide.with(context).load(tempUser.profileImageURL).into(viewHolder.itemView.useProfileImageView)
            viewHolder.itemView.useProfileImageView.visibility = View.VISIBLE
        } else {
            viewHolder.itemView.useProfileImageView.visibility = View.GONE
        }

        //setAnimation(viewHolder.itemView, position);

    }

    override fun getItemCount(): Int {
        return userList.size
    }

    fun addUser(user: User, key: String) {
        userList.add(user)
        userMap[key] = user
        notifyDataSetChanged()
        Log.d(TAG, "addUser " + userMap.toString())
    }

    fun updateLastLocation(myLocation: MyLocation?) {

        Log.d(TAG, "myLocation: " + myLocation.toString())
        Log.d(TAG, "UserMAP: " + userMap.toString())


        if (myLocation != null && userMap.containsKey(myLocation.userId)) {
            userMap[myLocation.userId]?.lastLocations = myLocation
            notifyDataSetChanged()
        }

    }

    fun updateLastLocation(locations: List<MyLocation>) {
        for (loc in locations) {
            if (loc != null && userMap.containsKey(loc.userId)) {
                userMap[loc.userId]?.lastLocations = loc

            }
        }
        Log.d(TAG, "updateLastLocation " + userMap.toString())
        notifyDataSetChanged()
    }


    fun getUserbyId(uId: String): User? {
        return userMap[uId]
    }


    fun updateProfileImage(uId: String, profileImgURL: String) {
        if (userMap.containsKey(uId)) {
            userMap[uId]?.profileImageURL = profileImgURL
        }
        notifyDataSetChanged()
    }

    private fun getAddressFromMyLocation(tempUser: User): String {
        var strAdd = ""
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(tempUser.lastLocation!!.latitude!!, tempUser.lastLocation!!.longitude!!, 1)
            if (addresses != null && !addresses.isEmpty()) {
                val returnedAddress = addresses[0]
                val sb = StringBuilder("")
                for (i in 0..returnedAddress.maxAddressLineIndex) {
                    sb.append(returnedAddress.getAddressLine(i)).append("\n")
                }
                strAdd = sb.toString()

            } else {

            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.stackTrace.toString())
        }

        return strAdd
    }


    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context,
                    android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    companion object {

        private val TAG = UserAdapter::class.java.name
    }


}
