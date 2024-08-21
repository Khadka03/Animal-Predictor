package com.example.animalpredictor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var contentImageView: ImageView
    private lateinit var contentTextView: TextView
    private lateinit var tflite: Interpreter
    private lateinit var auth: FirebaseAuth
    private val SELECT_IMAGE_REQUEST = 1
    private val CAPTURE_IMAGE_REQUEST = 2
    private val PERMISSION_REQUEST_CODE = 1
    private var selectedImageBitmap: Bitmap? = null
    private lateinit var currentPhotoPath: String
    private lateinit var classLabels: List<String> // Load from asset file

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase Auth and Analytics
        auth = Firebase.auth

        // Bind views with updated IDs
        contentImageView = findViewById(R.id.contentImageView)
        contentTextView = findViewById(R.id.contentTextView)
        val selectImageButton = findViewById<Button>(R.id.selectImageButton)
        val takePictureButton = findViewById<Button>(R.id.takePictureButton)
        val recognizeButton = findViewById<Button>(R.id.recognizeButton)

        // Initialize the BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Set listener for item selection
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Handle Home action
                    // Example: startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.navigation_learn -> {
                    // Handle Learn action
                    startActivity(Intent(this, CreatureLearn::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    // Handle Profile action
                    startActivity(Intent(this, Profile::class.java))
                    true
                }
                else -> false
            }
        }



        findViewById<ImageView>(R.id.logoutButton).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.clearButton).setOnClickListener {
            resetToInitialState()  // Reset to the initial layout
        }


        // Load labels from asset file
        loadClassLabels()

        // Check and request permission
        if (isPermissionsGranted()) {
            initializeModel()
        } else {
            requestPermissions()
        }

        selectImageButton.setOnClickListener {
            selectImage()
        }

        takePictureButton.setOnClickListener {
            captureImage()
        }

        recognizeButton.setOnClickListener {
            selectedImageBitmap?.let {
                recognizeImage(it)
            } ?: run {
                Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun isPermissionsGranted(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        return writePermission && cameraPermission
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.v("HomeActivity", "All permissions granted")
                    initializeModel()
                } else {
                    Log.v("HomeActivity", "Permissions denied")
                    Toast.makeText(this, "Permissions denied. Cannot use the camera or save images.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initializeModel() {
        try {
            val modelFile = loadModelFile()
            tflite = Interpreter(modelFile)
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error initializing TensorFlow Lite model", e)
        }
    }
    private fun resetToInitialState() {
        contentImageView.setImageDrawable(null)  // Clear the image view
        contentTextView.text = ""                // Clear the text view
        selectedImageBitmap = null               // Reset the selected image
        findViewById<Button>(R.id.recognizeButton).visibility = View.GONE  // Hide the recognize button
        findViewById<Button>(R.id.clearButton).visibility = View.GONE      // Hide the clear button
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = assets.openFd("mobilenet_v1_1.0_224_quant.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    private fun loadClassLabels() {
        classLabels = assets.open("labels_mobilenet_quant_v1_224.txt").bufferedReader().use { it.readLines() }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, SELECT_IMAGE_REQUEST)
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
        when (requestCode) {
            SELECT_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val imageUri = data.data
                    imageUri?.let {
                        Glide.with(this)
                            .load(it)
                            .into(contentImageView)

                        selectedImageBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it))
                        showRecognizeButton()
                        Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            CAPTURE_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    Glide.with(this)
                        .load(currentPhotoPath)
                        .into(contentImageView)

                    selectedImageBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    showRecognizeButton()
                    Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show()
                }


            }
        }
    }
    private fun recognizeImage(bitmap: Bitmap) {
        if (!::tflite.isInitialized) {
            Toast.makeText(this, "Model is not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val inputBuffer = ByteBuffer.allocateDirect(224 * 224 * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixelValues = IntArray(224 * 224)
        resizedBitmap.getPixels(pixelValues, 0, 224, 0, 0, 224, 224)

        for (pixel in pixelValues) {
            val red = ((pixel shr 16) and 0xFF).toByte()
            val green = ((pixel shr 8) and 0xFF).toByte()
            val blue = (pixel and 0xFF).toByte()
            inputBuffer.put(red)
            inputBuffer.put(green)
            inputBuffer.put(blue)
        }

        val output = ByteBuffer.allocateDirect(classLabels.size * 1)
        output.order(ByteOrder.nativeOrder())
        tflite.run(inputBuffer, output)

        output.rewind()
        val outputArray = ByteArray(classLabels.size)
        output.get(outputArray)

        val floatOutput = FloatArray(classLabels.size)
        for (i in outputArray.indices) {
            floatOutput[i] = outputArray[i].toFloat() / 255.0f
        }

        val maxIndex = floatOutput.indexOfMax()
        val confidence = floatOutput[maxIndex]
        val predictedClassName = classLabels[maxIndex]

        contentTextView.text = "Predicted class: $predictedClassName\nConfidence: ${confidence * 100}%"
        contentTextView.setTextColor(ContextCompat.getColor(this, R.color.headerTextColor)) // Set text color to red
        Toast.makeText(this, "Image recognized successfully", Toast.LENGTH_SHORT).show()

        showClearButton()

        val bundle = Bundle().apply {
            putString("predicted_class", predictedClassName)
            putFloat("confidence", confidence)
        }
    }


    private fun showClearButton() {
        findViewById<Button>(R.id.clearButton).visibility = View.VISIBLE


    }

    private fun FloatArray.indexOfMax(): Int {
        return this.indices.maxByOrNull { this[it] } ?: -1
    }

    private fun showRecognizeButton() {
        findViewById<Button>(R.id.recognizeButton).visibility = View.VISIBLE
    }

    // Firebase authentication state check
    override fun onStart() {
        super.onStart()
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser == null) {
            // No user is signed in; redirect to login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
