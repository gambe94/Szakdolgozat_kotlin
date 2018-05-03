package demeter.gabor.tracker.Util

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream

open class BaseActivity : AppCompatActivity() {

    private var mProgressDialog: ProgressDialog? = null


    protected var mStorageRef: StorageReference? = null
    protected var mImagesRefecence: StorageReference? = null


    protected var mDatabase: DatabaseReference? = null
    protected var mImages: DatabaseReference? = null

    protected val uid: String
        get() = FirebaseAuth.getInstance().currentUser!!.uid


    protected fun showProgressDialog(message: String) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog!!.setCancelable(false)
            mProgressDialog!!.setMessage(message)
        }

        mProgressDialog!!.show()
    }

    fun showProgressDialogPercentage(percentage: Double) {
        if (mProgressDialog == null) {
            mProgressDialog!!.setCancelable(false)
            mProgressDialog!!.setMessage("Uploaded" + percentage.toInt() + "%")
        }

        mProgressDialog!!.show()
    }

    protected fun hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
    }


    protected fun selectImageFromGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), Constants.PICK_IMAGE_REQUEST)
    }

    protected fun uploadImagetoFireBase(bitmapToUpload: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmapToUpload.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = mImagesRefecence!!.child(uid).child(Constants.PROFILE_IMG).putBytes(data)
        uploadTask.addOnFailureListener { exception ->
            // Handle unsuccessful uploads
            Log.e(TAG, exception.message)
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
            val downloadUrl = taskSnapshot.downloadUrl
            Toast.makeText(applicationContext, "Image Uploaded Successfully ", Toast.LENGTH_LONG).show()

            mImages!!.child(uid).setValue(downloadUrl!!.toString())
        }

    }

    companion object {

        private val TAG = BaseActivity::class.java.name
    }


}