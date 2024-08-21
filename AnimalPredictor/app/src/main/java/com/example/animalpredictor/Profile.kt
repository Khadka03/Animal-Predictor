package com.example.animalpredictor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var profileImage: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var editUserName: EditText
    private lateinit var editUserEmail: EditText
    private lateinit var selectImageButton: Button
    private lateinit var editProfileButton: Button
    private lateinit var saveProfileButton: Button
    private lateinit var cancelEditButton: Button
    private lateinit var logoutButton: Button
    private var imageUri: Uri? = null
    private lateinit var currentPhotoPath: String

    private val IMAGE_PICK_CODE = 1000
    private val CAPTURE_IMAGE_REQUEST = 1001
    private val PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users").child(auth.currentUser!!.uid)
        storage = FirebaseStorage.getInstance()

        // Bind views
        profileImage = findViewById(R.id.profileImage)
        userNameTextView = findViewById(R.id.userNameTextView)
        userEmailTextView = findViewById(R.id.userEmailTextView)
        editUserName = findViewById(R.id.editUserName)
        editUserEmail = findViewById(R.id.editUserEmail)
        selectImageButton = findViewById(R.id.selectImageButton)
        editProfileButton = findViewById(R.id.editProfileButton)
        saveProfileButton = findViewById(R.id.saveProfileButton)
        cancelEditButton = findViewById(R.id.cancelEditButton)
        logoutButton = findViewById(R.id.logoutButton)


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }

                R.id.navigation_learn -> {
                    startActivity(Intent(this, CreatureLearn::class.java))
                    true
                }
                R.id.navigation_profile -> true


                else -> false
            }
        }

        // Fetch and display user data
        loadUserData()

        // Edit profile functionality
        editProfileButton.setOnClickListener { toggleEditProfile(true) }

        // Save profile changes
        saveProfileButton.setOnClickListener { saveProfileChanges() }

        // Cancel editing
        cancelEditButton.setOnClickListener { toggleEditProfile(false) }

        // Logout functionality
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Image selection functionality
        selectImageButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun loadUserData() {
        database.get().addOnSuccessListener { dataSnapshot ->
            val userName = dataSnapshot.child("name").value as? String
            val userEmail = dataSnapshot.child("email").value as? String
            val profileImageUrl = dataSnapshot.child("profileImageUrl").value as? String

            userNameTextView.text = userName
            userEmailTextView.text = userEmail
            editUserName.setText(userName)
            editUserEmail.setText(userEmail)

            if (!profileImageUrl.isNullOrEmpty()) {
                Glide.with(this).load(profileImageUrl).into(profileImage)
            }
        }
    }

    private fun toggleEditProfile(enableEdit: Boolean) {
        if (enableEdit) {
            userNameTextView.visibility = View.GONE
            userEmailTextView.visibility = View.GONE
            editUserName.visibility = View.VISIBLE
            editUserEmail.visibility = View.VISIBLE
            logoutButton.visibility = View.GONE
            selectImageButton.visibility = View.VISIBLE
            saveProfileButton.visibility = View.VISIBLE
            cancelEditButton.visibility = View.VISIBLE
            editProfileButton.visibility = View.GONE
        } else {
            userNameTextView.visibility = View.VISIBLE
            userEmailTextView.visibility = View.VISIBLE
            editUserName.visibility = View.GONE
            editUserEmail.visibility = View.GONE
            selectImageButton.visibility = View.GONE
            saveProfileButton.visibility = View.GONE
            cancelEditButton.visibility = View.GONE
            editProfileButton.visibility = View.VISIBLE
        }
    }

    private fun saveProfileChanges() {
        val newUserName = editUserName.text.toString()
        val newUserEmail = editUserEmail.text.toString()

        val updatedUser = hashMapOf(
            "name" to newUserName,
            "email" to newUserEmail
        )

        if (imageUri != null) {
            uploadImageAndSaveProfile(updatedUser)
        } else {
            saveProfileToDatabase(updatedUser)
        }
    }

    private fun uploadImageAndSaveProfile(updatedUser: HashMap<String, String>) {
        val storageRef = storage.reference.child("profileImages/${auth.currentUser?.uid}.jpg")
        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val profileImageUrl = uri.toString()
                    updatedUser["profileImageUrl"] = profileImageUrl
                    saveProfileToDatabase(updatedUser)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Image upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveProfileToDatabase(updatedUser: HashMap<String, String>) {
        database.updateChildren(updatedUser as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                toggleEditProfile(false)
                loadUserData() // Refresh the displayed user data
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating profile: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun captureImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)?.also {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, CAPTURE_IMAGE_REQUEST)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(null)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    imageUri = data?.data
                    profileImage.setImageURI(imageUri)
                }
                CAPTURE_IMAGE_REQUEST -> {
                    imageUri = Uri.fromFile(File(currentPhotoPath))
                    profileImage.setImageURI(imageUri)
                }
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    openImagePicker()
                } else {
                    Toast.makeText(this, "Permissions denied. Cannot use the camera or save images.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
