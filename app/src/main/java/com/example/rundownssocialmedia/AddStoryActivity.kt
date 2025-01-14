package com.example.rundownssocialmedia

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.rundownssocialmedia.databinding.ActivityAddStoryBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import java.util.*
import kotlin.collections.HashMap

class AddStoryActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAddStoryBinding
    private lateinit var storage : FirebaseStorage
    private var imageUri : Uri? = null
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            imageUri = result.uriContent
            uploadStory()
        } else {
            val exception = result.error
            print(exception)
        }
    }
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private var myUrl : String = ""
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        registerLauncher()
        storage = Firebase.storage

        if(ContextCompat.checkSelfPermission(this@AddStoryActivity,android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_DENIED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@AddStoryActivity,android.Manifest.permission.READ_MEDIA_IMAGES)){
                Snackbar.make(view,"Permission needed for Gallery !!", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }.show()
            }else{
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }else{
            startCrop()
        }
    }
    private fun startCrop(){
        cropImage.launch(
            CropImageContractOptions(
                uri = imageUri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    outputCompressFormat = Bitmap.CompressFormat.PNG,
                    aspectRatioX = 9,
                    aspectRatioY = 16
                )
            )
        )
    }
    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result){
                //permission granted
                startCrop()
            }else{
                //permission denied
                Toast.makeText(this@AddStoryActivity,"Permission Needed ! ", Toast.LENGTH_SHORT).show()
            }

        }
}

    @Suppress("DEPRECATION")
    private fun uploadStory() {
        when{
            imageUri == null -> Toast.makeText(this,"Please select image first",Toast.LENGTH_SHORT).show()
            else ->{
                val progressDialog = ProgressDialog(this@AddStoryActivity)
                progressDialog.setTitle("Adding Story")
                progressDialog.setMessage("Please wait, we are adding your story...")
                progressDialog.show()
                val uuid = UUID.randomUUID()
                val imageName = "${uuid}.jpg"

                val reference = storage.reference
                val imageReference = reference.child("Story").child(imageName)

                imageReference.putFile(imageUri!!).addOnSuccessListener {
                    val uploadPictureReference = storage.reference.child("Story").child(imageName)
                    uploadPictureReference.downloadUrl.addOnSuccessListener {url ->

                        val getDownloadUrl = url.toString()
                        myUrl = getDownloadUrl

                        val ref = FirebaseDatabase.getInstance().reference.child("Story").child(FirebaseAuth.getInstance().currentUser!!.uid)
                        val storyId = (ref.push().key).toString()

                        val timeEnd = System.currentTimeMillis() + 86400000 //one day

                        val storyMap = HashMap<String, Any>()
                        storyMap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
                        storyMap["timestart"] = ServerValue.TIMESTAMP
                        storyMap["timeend"] = timeEnd
                        storyMap["imageurl"] = myUrl
                        storyMap["storyid"] = storyId

                        ref.child(storyId).updateChildren(storyMap)

                            Toast.makeText(this@AddStoryActivity,"Story has been uploaded successfully",Toast.LENGTH_SHORT).show()

                        val goMainIntent = Intent(this@AddStoryActivity, MainActivity::class.java)
                        startActivity(goMainIntent)
                        finish()

                        progressDialog.dismiss()

                    }.addOnFailureListener { e ->

                        Toast.makeText(this@AddStoryActivity,e.localizedMessage,Toast.LENGTH_SHORT).show()

                        progressDialog.dismiss()

                    }
                }.addOnFailureListener{
                    Toast.makeText(this@AddStoryActivity,it.localizedMessage,Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
