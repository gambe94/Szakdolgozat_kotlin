package demeter.gabor.tracker.models

import android.location.Location
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MyLocation(val userId: String?, val longitude: Double?, val latitude: Double?) {

    constructor() : this(null, null, null)

    constructor(loc: Location, userId: String) : this(null, loc.longitude, loc.latitude)

    constructor(latitude: Double?, longitude: Double?, userId: String) : this(userId, longitude, latitude)


}