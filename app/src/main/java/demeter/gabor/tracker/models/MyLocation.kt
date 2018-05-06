package demeter.gabor.tracker.models

import android.location.Location
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class MyLocation {

    var longitude: Double? = null
    var latitude: Double? = null
    var userId: String? = null

    constructor() {

    }

    constructor(loc: Location, userId: String) {
        this.latitude = loc.latitude
        this.longitude = loc.longitude
        this.userId = userId


    }

    constructor(latitude: Double?, longitude: Double?, userId: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.userId = userId

    }


    override fun toString(): String {
        return "longitude: " + this.longitude + " latitude: " + latitude + " userID: " + userId
    }
}