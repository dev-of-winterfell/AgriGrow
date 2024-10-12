package com.example.agrigrow

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UploadNewCrops : Fragment() {

    private lateinit var mediaSelectorView: ImageView
    private lateinit var cropNameEditText: EditText
    private lateinit var cropTypeEditText: EditText
    private lateinit var growingMethodSpinner: Spinner
    private lateinit var minPriceEditText: EditText
    private lateinit var maxPriceEditText: EditText
    private lateinit var stateEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressDialog: AlertDialog
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private val REQUEST_CODE_CAMERA_IMAGE = 1
    private val REQUEST_CODE_GALLERY_IMAGE = 2
    private val REQUEST_CODE_CAMERA_VIDEO = 3
    private val REQUEST_CODE_GALLERY_VIDEO = 4

    private val capturedImageUris: MutableList<Uri> = mutableListOf()
    private var capturedVideoUri: Uri? = null
    private var imageCaptureCount = 0

    private val mspCrops = mutableMapOf<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_a_i, container, false)

        // Initialize views
        mediaSelectorView = view.findViewById(R.id.cropImageView)
        cropNameEditText = view.findViewById(R.id.cropNameEditText)
        cropTypeEditText = view.findViewById(R.id.cropTypeEditText)
        growingMethodSpinner = view.findViewById(R.id.growingMethodEditText)
        minPriceEditText = view.findViewById(R.id.minPriceEditText)
        maxPriceEditText = view.findViewById(R.id.maxPriceEditText)
        stateEditText = view.findViewById(R.id.stateEditText)
        amountEditText = view.findViewById(R.id.amountEditText)
        saveButton = view.findViewById(R.id.saveButton)

        // Set up progress dialog
        val progressDialogView = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        progressBar = progressDialogView.findViewById(R.id.progressBar)
        progressDialog = AlertDialog.Builder(requireContext())
            .setView(progressDialogView)
            .setCancelable(false)
            .create()

        // Set up listeners
        mediaSelectorView.setOnClickListener {
            showMediaSelectionDialog()
        }

        saveButton.setOnClickListener {
            setupSaveButton()
        }

        setupSpinner()
        fetchMspCrops()
        setupTextWatchers()
        displayUserName(view)
        setupInfoButton(view)

        return view
    }

    private fun setupTextWatchers() {
        cropNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val cropName = s.toString()
                if (mspCrops.containsKey(cropName)) {
                    minPriceEditText.setText(mspCrops[cropName].toString())
                    minPriceEditText.isEnabled = false
                } else {
                    minPriceEditText.setText("")
                    minPriceEditText.isEnabled = true
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun displayUserName(view: View) {
        val layout = view.findViewById<LinearLayout>(R.id.linearLayout22)
        val tv = layout.findViewById<TextView>(R.id.tv2)
        val userId = FirebaseAuth.getInstance().currentUser?.email

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("SELLERS").document(userId)

            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("Name") ?: "No Name Available"
                    tv.text = userName
                } else {
                    tv.text = "No user data available"
                }
            }.addOnFailureListener {
                tv.text = "Failed to load user data"
            }
        } else {
            tv.text = "User not logged in"
        }
    }

    private fun setupInfoButton(view: View) {
        val infoButton = view.findViewById<ImageButton>(R.id.imagebtn)
        infoButton.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun showMediaSelectionDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Media Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startCameraProcess()
                    1 -> startGalleryProcess()
                }
            }
            .show()
    }

    private fun startCameraProcess() {
        capturedImageUris.clear()
        imageCaptureCount = 0
        captureNextImage()
    }

    private fun captureNextImage() {
        val imageFile = createImageFile()
        val imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, REQUEST_CODE_CAMERA_IMAGE)
        // Store the URI to save later in onActivityResult
        currentImageUri = imageUri
    }

    private var currentImageUri: Uri? = null

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun recordVideo() {
        val videoFile = createVideoFile()
        val videoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            videoFile
        )
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // Limit to 1 minute
        startActivityForResult(intent, REQUEST_CODE_CAMERA_VIDEO)
        capturedVideoUri = videoUri
    }

    private fun createVideoFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile(
            "VIDEO_${timeStamp}_",
            ".mp4",
            storageDir
        )
    }

    private fun startGalleryProcess() {
        // First, select images
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(intent, "Select Images"),
            REQUEST_CODE_GALLERY_IMAGE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_CAMERA_IMAGE -> {
                    // Image captured from camera
                    currentImageUri?.let {
                        capturedImageUris.add(it)
                        // Allow editing if necessary
                        // You can call editImage(it) here if you have an editor
                        imageCaptureCount++
                        if (imageCaptureCount < 3) {
                            captureNextImage()
                        } else {
                            // After 3 images, record video
                            recordVideo()
                        }
                    }
                }
                REQUEST_CODE_CAMERA_VIDEO -> {
                    // Video captured from camera
                    // Validate video duration
                    validateVideoDuration(capturedVideoUri)
                }
                REQUEST_CODE_GALLERY_IMAGE -> {
                    // Images selected from gallery
                    if (data?.clipData != null) {
                        val count = data.clipData!!.itemCount
                        if (count != 3) {
                            Toast.makeText(
                                context,
                                "Please select exactly 3 images",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                        capturedImageUris.clear()
                        for (i in 0 until count) {
                            val imageUri = data.clipData!!.getItemAt(i).uri
                            capturedImageUris.add(imageUri)
                        }
                        // Proceed to select video
                        val videoIntent = Intent(Intent.ACTION_GET_CONTENT)
                        videoIntent.type = "video/*"
                        startActivityForResult(
                            Intent.createChooser(videoIntent, "Select Video"),
                            REQUEST_CODE_GALLERY_VIDEO
                        )
                    } else {
                        Toast.makeText(
                            context,
                            "Please select exactly 3 images",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                REQUEST_CODE_GALLERY_VIDEO -> {
                    // Video selected from gallery
                    capturedVideoUri = data?.data
                    validateVideoDuration(capturedVideoUri)
                }
            }
        }
    }

    private fun validateVideoDuration(videoUri: Uri?) {
        if (videoUri == null) {
            Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show()
            return
        }
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val time =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillisec = time?.toLong() ?: 0
        retriever.release()

        val maxDuration = 60 * 1000 // 1 minute in milliseconds
        if (timeInMillisec > maxDuration) {
            Toast.makeText(
                context,
                "Please select a video less than or equal to 1 minute",
                Toast.LENGTH_SHORT
            ).show()
            capturedVideoUri = null
        } else {
            // Update UI to show that media has been selected
            mediaSelectorView.setImageResource(R.drawable.baseline_image_24)
        }
    }

    private fun setupSaveButton() {
        if (validateInputs()) {
            progressDialog.show()
            lifecycleScope.launch {
                try {
                    val imageUrls = uploadImages()
                    val videoUrl = uploadVideo()
                    val cropData = createCropData(imageUrls, videoUrl)
                    saveCropToFirestore(cropData)

                    Toast.makeText(
                        context,
                        "Crop data uploaded successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Upload failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        return when {
            cropNameEditText.text.toString().isEmpty() -> {
                cropNameEditText.error = "कृपया फसल का नाम दर्ज करें"
                false
            }
            cropTypeEditText.text.toString().isEmpty() -> {
                cropTypeEditText.error = "कृपया फसल के प्रकार का उल्लेख करें"
                false
            }
            growingMethodSpinner.selectedItemPosition == 0 -> {
                Toast.makeText(
                    context,
                    "कृपया फसल उगाने की विधि चुनें",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
            minPriceEditText.text.toString().isEmpty() -> {
                minPriceEditText.error = "कृपया न्यूनतम मूल्य दर्ज करें"
                false
            }
            maxPriceEditText.text.toString().isEmpty() -> {
                maxPriceEditText.error = "कृपया अधिकतम मूल्य दर्ज करें"
                false
            }
            stateEditText.text.toString().isEmpty() -> {
                stateEditText.error = "कृपया राज्य का नाम दर्ज करें"
                false
            }
            amountEditText.text.toString().isEmpty() -> {
                amountEditText.error = "कृपया मात्रा दर्ज करें"
                false
            }
            capturedImageUris.size != 3 -> {
                Toast.makeText(
                    context,
                    "Please capture/select 3 images",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
            capturedVideoUri == null -> {
                Toast.makeText(
                    context,
                    "Please record/select a video",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
            else -> true
        }
    }

    private suspend fun uploadImages(): List<String> = withContext(Dispatchers.IO) {
        val imageUrls = mutableListOf<String>()
        for (uri in capturedImageUris) {
            val imageRef = storage.reference.child("crop_images/${UUID.randomUUID()}")
            val uploadTask = imageRef.putFile(uri)
            uploadTask.await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            imageUrls.add(downloadUrl)
        }
        return@withContext imageUrls
    }

    private suspend fun uploadVideo(): String = withContext(Dispatchers.IO) {
        val videoRef = storage.reference.child("crop_videos/${UUID.randomUUID()}")
        val uploadTask = videoRef.putFile(capturedVideoUri!!)
        uploadTask.await()
        return@withContext videoRef.downloadUrl.await().toString()
    }

    private fun createCropData(imageUrls: List<String>, videoUrl: String): Map<String, Any> {
        val uniqueCropId = UUID.randomUUID().toString()
        return mapOf(
            "cropId" to uniqueCropId,
            "cropName" to cropNameEditText.text.toString(),
            "cropType" to cropTypeEditText.text.toString(),
            "growingMethod" to growingMethodSpinner.selectedItem.toString(),
            "minPrice" to minPriceEditText.text.toString().toFloat(),
            "maxPrice" to maxPriceEditText.text.toString().toFloat(),
            "state" to stateEditText.text.toString(),
            "amount" to amountEditText.text.toString().toInt(),
            "imageUrls" to imageUrls,
            "videoUrl" to videoUrl
        )
    }

    private suspend fun saveCropToFirestore(cropData: Map<String, Any>) =
        withContext(Dispatchers.IO) {
            val email = auth.currentUser?.email ?: throw Exception("User email not found")
            firestore.collection("SELLERS").document(email)
                .update("crops", FieldValue.arrayUnion(cropData))
                .await()
        }

    private fun setupSpinner() {
        val growingMethods = arrayOf("फसल उगाने की विधि", "ऑर्गेनिक", "इनऑर्गेनिक")
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            growingMethods
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(
                    if (position == 0) android.graphics.Color.GRAY
                    else android.graphics.Color.BLACK
                )
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        growingMethodSpinner.adapter = adapter
        growingMethodSpinner.setSelection(0)
    }

    private fun fetchMspCrops() {
        firestore.collection("MSP_CROPS").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val cropName = document.getString("CROP_NAME")
                    val cropPrice = document.get("CROP_PRICE") as? Number
                    if (cropName != null && cropPrice != null) {
                        mspCrops[cropName] = cropPrice.toFloat()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "एमएसपी फसलें लाने में त्रुटि: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showInfoDialog() {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.info_dialog_box, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)
        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
}
