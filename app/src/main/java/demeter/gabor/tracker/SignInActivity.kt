package demeter.gabor.tracker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import demeter.gabor.tracker.Util.BaseActivity
import demeter.gabor.tracker.Util.Constants
import demeter.gabor.tracker.models.User
import kotlinx.android.synthetic.main.activity_login.*
import java.io.IOException

class SignInActivity : BaseActivity(), View.OnClickListener {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mEmailField: EditText
    private lateinit var mPasswordField: EditText
    private lateinit var mSignInButton: Button
    private lateinit var mSignUpButton: Button


    private lateinit var userProfileImage: ImageView
    private lateinit var userProfileImagePath: Uri

    private var isSelectedImage = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //INIT FITEBASE REFERENCE
        mDatabase = FirebaseDatabase.getInstance().reference //filed declared in baseActivity
        mAuth = FirebaseAuth.getInstance()
        mStorageRef = FirebaseStorage.getInstance().reference //filed declared in baseActivity
        mImagesRefecence = mStorageRef.child(Constants.IMAGES_STORAGR_REF) //filed declared in baseActivity

        // Views
        mEmailField = findViewById<EditText>(R.id.field_email)
        mPasswordField = findViewById<EditText>(R.id.field_password)
        mSignInButton = findViewById(R.id.button_sign_in)
        mSignUpButton = findViewById(R.id.button_sign_up)
        userProfileImage = findViewById(R.id.userProfileImage)

        // Click listeners
        mSignInButton.setOnClickListener(this)
        mSignUpButton.setOnClickListener(this)
        userProfileImage.setOnClickListener(this)
    }

    public override fun onStart() {
        super.onStart()

        // Check auth on Activity start
        if (mAuth.currentUser != null) {
            onAuthSuccess(mAuth.currentUser!!)
        }
    }

    private fun signIn() {
        Log.d(TAG, "signIn")
        if (!validateForm()) {
            return
        }

        showProgressDialog("Sign In ...")
        val email = field_email?.text.toString()
        val password = field_password?.text.toString()

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    Log.d(TAG, "signIn:onComplete:" + task.isSuccessful)
                    hideProgressDialog()


                    if (task.isSuccessful) {
                        onAuthSuccess(task.result.user)
                    } else {
                        Toast.makeText(this@SignInActivity, "Sign In Failed",Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun signUp() {
        Log.d(TAG, "signUp")
        if (!validateForm()) {
            return
        }

        showProgressDialog("Sign Up...")
        val email = field_email?.text.toString()
        val password = field_password?.text.toString()

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    Log.d(TAG, "createUser:onComplete:" + task.isSuccessful)
                    hideProgressDialog()
                    if (task.isSuccessful) {
                        if (isSelectedImage) {
                            uploadImagetoFireBase(userProfileImage.drawingCache)
                            onAuthSuccess(task.result.user)
                        }
                    } else {
                        Toast.makeText(this@SignInActivity, "Sign Up Failed",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun onAuthSuccess(user: FirebaseUser) {
        val username = usernameFromEmail(user.email!!)

        // Write new user
        writeNewUser(user.uid, username, user.email)

        // Go to MainActivity
        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        finish()
    }

    private fun usernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            email
        }
    }

    private fun validateForm(): Boolean {
        var result = true
        if (TextUtils.isEmpty(mEmailField.text.toString())) {
            mEmailField.error = "Required"
            result = false
        } else {
            mEmailField.error = null
        }

        if (TextUtils.isEmpty(mPasswordField.text.toString())) {
            mPasswordField.error = "Required"
            result = false
        } else {
            mPasswordField.error = null
        }

        return result
    }


    private fun writeNewUser(userId: String, name: String, email: String?) {

        val user = User(name, email!!, userId)
        mDatabase.child(Constants.USERS_REF).child(userId).setValue(user)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.data != null) {
            userProfileImagePath = data.data

            var bitmap: Bitmap? = null
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, userProfileImagePath)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            userProfileImage.setImageBitmap(bitmap)

            userProfileImage.isDrawingCacheEnabled = true
            userProfileImage.buildDrawingCache()
            isSelectedImage = true
        }
    }


    override fun onClick(v: View) {
        val i = v.id
        if (i == R.id.button_sign_in) {
            signIn()
        } else if (i == R.id.button_sign_up) {
            signUp()
        } else if (i == R.id.userProfileImage) {
            selectImageFromGallery() //Base Activity
        }
    }

    companion object {

        private val TAG = "SignInActivity"
    }
}