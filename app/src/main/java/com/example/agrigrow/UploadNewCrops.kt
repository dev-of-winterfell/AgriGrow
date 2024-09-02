package com.example.agrigrow

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AIFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UploadNewCrops : Fragment() {
    private lateinit var cropImageView: ImageView
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
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private val mspCrops = mutableMapOf<String, Float>()
    private var selectedImageUri: Uri? = null
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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_a_i, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        cropImageView = view.findViewById(R.id.cropImageView)
        cropNameEditText = view.findViewById(R.id.cropNameEditText)
        cropTypeEditText = view.findViewById(R.id.cropTypeEditText)
        growingMethodSpinner = view.findViewById(R.id.growingMethodEditText)
        minPriceEditText = view.findViewById(R.id.minPriceEditText)
        maxPriceEditText = view.findViewById(R.id.maxPriceEditText)
        stateEditText = view.findViewById(R.id.stateEditText)
        amountEditText = view.findViewById(R.id.amountEditText)
        saveButton = view.findViewById(R.id.saveButton)
//        progressBar = view.findViewById(R.id.progressBar)
        fetchMspCrops()
        setupSpinner()
        setupImagePicker()
        setupSaveButton()
        val layout=view.findViewById<LinearLayout>(R.id.linearLayout22)
        val tv=layout.findViewById<TextView>(R.id.tv2)
        // Retrieve the current logged-in user
        val userId = FirebaseAuth.getInstance().currentUser?.email

        if (userId != null) {
            // Fetch user data from Firestore
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
        val progressDialogView = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        progressBar = progressDialogView.findViewById(R.id.progressBar)
        progressDialog = AlertDialog.Builder(requireContext())
            .setView(progressDialogView)
            .setCancelable(false)
            .create()

        val infoButton=view.findViewById<ImageButton>(R.id.imagebtn)
        infoButton.setOnClickListener {
            showInfoDialog()
        }

        cropImageView.setOnClickListener {
            openGallery()
        }

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
        return view


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
                Toast.makeText(context, "एमएसपी फसलें लाने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AIFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UploadNewCrops().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
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

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(if (position == 0) android.graphics.Color.GRAY else android.graphics.Color.BLACK)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        growingMethodSpinner.adapter = adapter
        growingMethodSpinner.setSelection(0)
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
                Toast.makeText(context, "कृपया फसल उगाने की विधि चुनें", Toast.LENGTH_SHORT).show()
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
            selectedImageUri == null -> {
                Toast.makeText(context, "कृपया एक छवि चुनें", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }


    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }


    private fun setupImagePicker() {
        cropImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            Glide.with(this)
                .load(selectedImageUri)
                .apply(RequestOptions().override(300, 300).centerCrop())
                .into(cropImageView)
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (validateInputs()) {
                progressDialog.show()
                lifecycleScope.launch {
                    try {
                        val imageUrl = uploadImage()
                        val cropData = createCropData(imageUrl)
                        saveCropToFirestore(cropData)

                        Toast.makeText(context, "फसल डेटा सफलतापूर्वक अपलोड किया गया", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "अपलोड विफल: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }



    private fun uploadCropData() {
        progressBar.visibility = View.VISIBLE
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "उपयोगकर्ता लॉग इन नहीं है", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }
        if (selectedImageUri == null) {
            Toast.makeText(context, "कृपया एक छवि चुनें", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val imageUrl = uploadImage()
                val cropData = createCropData(imageUrl)
                saveCropToFirestore(cropData)

                Toast.makeText(context, "फसल डेटा सफलतापूर्वक अपलोड किया गया", Toast.LENGTH_SHORT)
                    .show()
                progressBar.visibility = View.GONE
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "अपलोड विफल: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun uploadImage(): String = withContext(Dispatchers.IO) {
        val imageRef = storage.reference.child("crop_images/${UUID.randomUUID()}")
        val uploadTask = imageRef.putFile(selectedImageUri!!)
        uploadTask.await()
        return@withContext imageRef.downloadUrl.await().toString()
    }

    private fun createCropData(imageUrl: String): Map<String, Any> {
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
            "imageUrl" to imageUrl
        )

    }

    private suspend fun saveCropToFirestore(cropData: Map<String, Any>) =
        withContext(Dispatchers.IO) {
            val email = auth.currentUser?.email ?: throw Exception("User email not found")
            val cropId = cropData["cropId"] as String
            firestore.collection("SELLERS").document(email)
                .update("crops", FieldValue.arrayUnion(cropData))
                .await()

        }
    private fun showInfoDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.info_dialog_box, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val infoTextView = dialogView.findViewById<TextView>(R.id.infoText)
        // Set any additional properties or text to infoTextView here if needed
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)
        // Remove this line as it's not needed and might cause issues
        // val view2 = dialogView.findViewById<Button>(R.id.imageView5)
        closeButton.setOnClickListener {
            alertDialog.dismiss() // This will close the dialog
        }
        alertDialog.show()
    }


}




//
//    private fun showAddCropDialog() {
//        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_box, null)
//        val alertDialog = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .create()
//
//        infoButton = dialogView.findViewById(R.id.imagebtn)
//        infoButton.setOnClickListener {
//            showInfoDialog()
//        }
//
//        cropImageView = dialogView.findViewById(R.id.cropImageView)
//        val cropNameEditText = dialogView.findViewById<EditText>(R.id.cropNameEditText)
//        val cropTypeEditText = dialogView.findViewById<EditText>(R.id.cropTypeEditText)
//        val growingMethodSpinner = dialogView.findViewById<Spinner>(R.id.growingMethodEditText)
//        val minPriceEditText = dialogView.findViewById<EditText>(R.id.minPriceEditText)
//        val maxPriceEditText = dialogView.findViewById<EditText>(R.id.maxPriceEditText)
//        val stateEditText = dialogView.findViewById<EditText>(R.id.stateEditText)
//        val amountEditText = dialogView.findViewById<EditText>(R.id.amountEditText)
//        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
//        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
//
//        // Set up the Spinner
//        val growingMethods = arrayOf("फसल उगाने की विधि", "ऑर्गेनिक", "इनऑर्गेनिक")
//        val growingMethodsAdapter = object : ArrayAdapter<String>(
//            requireContext(),
//            android.R.layout.simple_spinner_item,
//            growingMethods
//        ) {
//            override fun isEnabled(position: Int): Boolean {
//                return position != 0
//            }
//
//            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
//                val view = super.getDropDownView(position, convertView, parent)
//                val textView = view as TextView
//                if (position == 0) {
//                    textView.setTextColor(android.graphics.Color.GRAY)
//                } else {
//                    textView.setTextColor(android.graphics.Color.BLACK)
//                }
//                return view
//            }
//        }
//        cropImageView.setOnClickListener {
//            openGallery()
//        }
//        growingMethodsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        growingMethodSpinner.adapter = growingMethodsAdapter
//        growingMethodSpinner.setSelection(0)
//
//        cropNameEditText.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                val cropName = s.toString()
//                if (mspCrops.containsKey(cropName)) {
//                    minPriceEditText.setText(mspCrops[cropName].toString())
//                    minPriceEditText.isEnabled = false
//                } else {
//                    minPriceEditText.setText("")
//                    minPriceEditText.isEnabled = true
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })
//
//        saveButton.setOnClickListener {
//            lifecycleScope.launch {
//                validateAndSaveCropData(
//                    cropNameEditText,
//                    cropTypeEditText,
//                    growingMethodSpinner,
//                    minPriceEditText,
//                    maxPriceEditText,
//                    stateEditText,
//                    amountEditText,
//                    alertDialog
//                )
//            }
//        }
//
//        cancelButton.setOnClickListener {
//            alertDialog.dismiss()
//        }
//
//        alertDialog.show()
//    }
//    private fun openGallery() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == android.app.Activity.RESULT_OK && data != null && data.data != null) {
//            selectedImageUri = data.data
//            // Use Glide to load and display the selected image
//            Glide.with(this)
//                .load(selectedImageUri)
//                .apply(RequestOptions().override(300, 300).centerCrop())
//                .into(cropImageView)
//        }
//    }
//
//    private suspend fun validateAndSaveCropData(
//        cropNameEditText: EditText,
//        cropTypeEditText: EditText,
//        growingMethodSpinner: Spinner,
//        minPriceEditText: EditText,
//        maxPriceEditText: EditText,
//        stateEditText: EditText,
//        amountEditText: EditText,
//        alertDialog: AlertDialog
//    ) {
//        val cropName = cropNameEditText.text.toString()
//        val cropType = cropTypeEditText.text.toString()
//        val growingMethod = growingMethodSpinner.selectedItem.toString()
//        val minPrice = minPriceEditText.text.toString().toFloatOrNull() ?: return
//        val maxPrice = maxPriceEditText.text.toString().toFloatOrNull() ?: return
//        val state = stateEditText.text.toString()
//        val amount = amountEditText.text.toString().toIntOrNull() ?: return
//
//        if (cropName.isBlank() || cropType.isBlank() || growingMethod == "फसल उगाने की विधि" || minPrice <= 0 || maxPrice <= 0 || state.isBlank() || amount <= 0) {
//            Toast.makeText(context, "कृपया सभी फ़ील्ड सही ढंग से भरें", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Upload image and crop data
//        uploadImageAndData(
//            cropName,
//            cropType,
//            growingMethod,
//            minPrice,
//            maxPrice,
//            state,
//            amount,
//            alertDialog
//        )
//    }
//
//
//
//
//
//    private suspend fun uploadImageAndData(
//        cropName: String,
//        cropType: String,
//        growingMethod: String,
//        minPrice: Float,
//        maxPrice: Float,
//        state: String,
//        amount: Int,
//        alertDialog: AlertDialog
//    ) = withContext(Dispatchers.IO) {
//        val currentUser = auth.currentUser
//        if (currentUser == null) {
//            withContext(Dispatchers.Main) {
//                hideProgressDialog()
//                Toast.makeText(context, "उपयोगकर्ता लॉग इन नहीं है", Toast.LENGTH_SHORT).show()
//            }
//            return@withContext
//        }
//
//        val email = currentUser.email ?: return@withContext
//
//        try {
//            withContext(Dispatchers.Main) {
//                showProgressDialog()  // Show progress dialog before starting the upload
//            }
//
//            val imageRef = storage.reference.child("crop_images/${UUID.randomUUID()}")
//            val uploadTask = imageRef.putFile(selectedImageUri!!)
//            uploadTask.addOnProgressListener { taskSnapshot ->
//                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
//                CoroutineScope(Dispatchers.Main).launch {
//                    progressBar.progress = progress
//                }
//            }
//            val downloadUrl = uploadTask.continueWithTask { task ->
//                if (!task.isSuccessful) {
//                    task.exception?.let { throw it }
//                }
//                imageRef.downloadUrl
//            }.await().toString()
//
//            val cropData = mapOf(
//                "cropName" to cropName,
//                "cropType" to cropType,
//                "growingMethod" to growingMethod,
//                "minPrice" to minPrice,
//                "maxPrice" to maxPrice,
//                "state" to state,
//                "amount" to amount,
//                "imageUrl" to downloadUrl
//            )
//
//            firestore.collection("SELLERS").document(email)
//                .update("crops", FieldValue.arrayUnion(cropData))
//                .await()
//
//            withContext(Dispatchers.Main) {
//                hideProgressDialog()
//                Toast.makeText(context, "फसल डेटा सफलतापूर्वक अपलोड किया गया", Toast.LENGTH_SHORT).show()
//                alertDialog.dismiss()
//                // Refresh crops after upload
//            }
//        } catch (e: Exception) {
//            withContext(Dispatchers.Main) {
//                hideProgressDialog()
//                Toast.makeText(context, "डेटा अपलोड करते समय त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//    private fun showProgressDialog() {
//        progressDialog.show()
//    }
//
//    private fun hideProgressDialog() {
//        progressDialog.dismiss()
//    }
//    private fun showInfoDialog() {
//        val dialogView = LayoutInflater.from(context).inflate(R.layout.info_dialog_box, null)
//        val alertDialog = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .setCancelable(true)
//            .create()
//
//        val infoTextView = dialogView.findViewById<TextView>(R.id.infoText)
//        // Set any additional properties or text to infoTextView here if needed
//        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)
//        // Remove this line as it's not needed and might cause issues
//        // val view2 = dialogView.findViewById<Button>(R.id.imageView5)
//        closeButton.setOnClickListener {
//            alertDialog.dismiss() // This will close the dialog
//        }
//        alertDialog.show()
//    }