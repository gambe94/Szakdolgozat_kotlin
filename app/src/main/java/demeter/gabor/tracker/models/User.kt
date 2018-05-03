package demeter.gabor.tracker.models

import java.util.*


class User {


    private var uId: String? = null
    var username: String? = null
    var email: String? = null
    private var locations: Stack<MyLocation>? = Stack<MyLocation>()
    var profileImageURL: String? = null

    val lastLocation: MyLocation?
        get() = if (!this.locations!!.empty()) {
            this.locations!!.peek()
        } else null

    var lastLocations: MyLocation?
        get() {
            return if (this.locations != null && !this.locations!!.isEmpty()) {
                locations!!.peek()
            } else {
                null
            }
        }
        set(loc) {
            if (this.locations == null) {
                locations = Stack<MyLocation>()
                locations!!.push(loc)
            } else {
                this.locations!!.push(loc)
            }
        }

    constructor() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    constructor(username: String, email: String, uId: String) { // for login/sign in
        this.username = username
        this.email = email
        this.uId = uId

    }

    constructor(username: String, email: String, loc: MyLocation) {
        this.username = username
        this.email = email
        this.locations!!.push(loc)

    }

    fun getuId(): String? {
        return uId
    }

    fun setuId(uId: String) {
        this.uId = uId
    }

//    fun toMap(): Map<String, Any> {
//        val result = HashMap<String, Any>()
//        result["username"] = this.username
//        result["email"] = this.email
//        result["lastLocation"] = lastLocation
//
//
//        return result
//    }

    override fun toString(): String {
        return this.username + " " + this.lastLocation
    }
}