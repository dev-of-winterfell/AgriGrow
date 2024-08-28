package com.example.agrigrow

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
import java.util.*

class SellerHomePage : Fragment() {
private lateinit var infoButton: ImageButton
    private lateinit var cropRecyclerView: RecyclerView
    private lateinit var addCropButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressDialog: AlertDialog
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private lateinit var cropImageView: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var cropAdapter: CropAdapter
    private val cropList = mutableListOf<Crop>()
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
        val view = inflater.inflate(R.layout.fragment_seller_home, container, false)

        cropRecyclerView = view.findViewById(R.id.cropRecyclerView)
        addCropButton = view.findViewById(R.id.cropadbutton)

        cropAdapter = CropAdapter(cropList)
        cropRecyclerView.layoutManager = LinearLayoutManager(context)
        cropRecyclerView.adapter = cropAdapter

        addCropButton.setOnClickListener {
            showAddCropDialog()
        }
        lifecycleScope.launch {
            fetchMspCrops()
            fetchCrops() // Ensure this is called within a coroutine scope
        }


        val progressDialogView = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        progressBar = progressDialogView.findViewById(R.id.progressBar)
        progressDialog = AlertDialog.Builder(requireContext())
            .setView(progressDialogView)
            .setCancelable(false)
            .create()

        return view
    }


    private suspend fun fetchCrops() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "उपयोगकर्ता लॉग इन नहीं है", Toast.LENGTH_SHORT).show()
            }
            return@withContext
        }

        val email = currentUser.email ?: return@withContext

        try {
            val document = firestore.collection("SELLERS").document(email).get().await()
            if (document != null && document.exists()) {
                val crops = document.get("crops") as? List<Map<String, Any>>
                if (crops != null) {
                    cropList.clear()
                    for (cropData in crops) {
                        val crop = Crop(
                            name = cropData["cropName"] as String,
                            type = cropData["cropType"] as String,
                            growingMethod = cropData["growingMethod"] as String,
                            minPrice = (cropData["minPrice"] as Number).toFloat(),
                            maxPrice = (cropData["maxPrice"] as Number).toFloat(),
                            state = cropData["state"] as String,
                            amount = (cropData["amount"] as Number).toInt(),
                            imageUrl = cropData["imageUrl"] as String
                        )
                        cropList.add(crop)
                    }
                    withContext(Dispatchers.Main) {
                        cropAdapter.notifyDataSetChanged()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "कोई फसल नहीं मिली:", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "फसल लाने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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


    private fun showAddCropDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_box, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        infoButton = dialogView.findViewById(R.id.imagebtn)
        infoButton.setOnClickListener {
            showInfoDialog()
        }

        cropImageView = dialogView.findViewById(R.id.cropImageView)
        val cropNameEditText = dialogView.findViewById<EditText>(R.id.cropNameEditText)
        val cropTypeEditText = dialogView.findViewById<EditText>(R.id.cropTypeEditText)
        val growingMethodSpinner = dialogView.findViewById<Spinner>(R.id.growingMethodEditText)
        val minPriceEditText = dialogView.findViewById<EditText>(R.id.minPriceEditText)
        val maxPriceEditText = dialogView.findViewById<EditText>(R.id.maxPriceEditText)
        val stateEditText = dialogView.findViewById<EditText>(R.id.stateEditText)
        val amountEditText = dialogView.findViewById<EditText>(R.id.amountEditText)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Set up the Spinner
        val growingMethods = arrayOf("फसल उगाने की विधि", "ऑर्गेनिक", "इनऑर्गेनिक")
        val growingMethodsAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            growingMethods
        ) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                if (position == 0) {
                    textView.setTextColor(android.graphics.Color.GRAY)
                } else {
                    textView.setTextColor(android.graphics.Color.BLACK)
                }
                return view
            }
        }
        cropImageView.setOnClickListener {
            openGallery()
        }
        growingMethodsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        growingMethodSpinner.adapter = growingMethodsAdapter
        growingMethodSpinner.setSelection(0)

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

        saveButton.setOnClickListener {
           lifecycleScope.launch {
                validateAndSaveCropData(
                    cropNameEditText,
                    cropTypeEditText,
                    growingMethodSpinner,
                    minPriceEditText,
                    maxPriceEditText,
                    stateEditText,
                    amountEditText,
                    alertDialog
                )
            }
        }

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == android.app.Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            // Use Glide to load and display the selected image
            Glide.with(this)
                .load(selectedImageUri)
                .apply(RequestOptions().override(300, 300).centerCrop())
                .into(cropImageView)
        }
    }

    private suspend fun validateAndSaveCropData(
        cropNameEditText: EditText,
        cropTypeEditText: EditText,
        growingMethodSpinner: Spinner,
        minPriceEditText: EditText,
        maxPriceEditText: EditText,
        stateEditText: EditText,
        amountEditText: EditText,
        alertDialog: AlertDialog
    ) {
        val cropName = cropNameEditText.text.toString()
        val cropType = cropTypeEditText.text.toString()
        val growingMethod = growingMethodSpinner.selectedItem.toString()
        val minPrice = minPriceEditText.text.toString().toFloatOrNull() ?: return
        val maxPrice = maxPriceEditText.text.toString().toFloatOrNull() ?: return
        val state = stateEditText.text.toString()
        val amount = amountEditText.text.toString().toIntOrNull() ?: return

        if (cropName.isBlank() || cropType.isBlank() || growingMethod == "फसल उगाने की विधि" || minPrice <= 0 || maxPrice <= 0 || state.isBlank() || amount <= 0) {
            Toast.makeText(context, "कृपया सभी फ़ील्ड सही ढंग से भरें", Toast.LENGTH_SHORT).show()
            return
        }

        // Upload image and crop data
        uploadImageAndData(
            cropName,
            cropType,
            growingMethod,
            minPrice,
            maxPrice,
            state,
            amount,
            alertDialog
        )
    }





    private suspend fun uploadImageAndData(
        cropName: String,
        cropType: String,
        growingMethod: String,
        minPrice: Float,
        maxPrice: Float,
        state: String,
        amount: Int,
        alertDialog: AlertDialog
    ) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            withContext(Dispatchers.Main) {
                hideProgressDialog()
                Toast.makeText(context, "उपयोगकर्ता लॉग इन नहीं है", Toast.LENGTH_SHORT).show()
            }
            return@withContext
        }

        val email = currentUser.email ?: return@withContext

        try {
            withContext(Dispatchers.Main) {
                showProgressDialog()  // Show progress dialog before starting the upload
            }

            val imageRef = storage.reference.child("crop_images/${UUID.randomUUID()}")
            val uploadTask = imageRef.putFile(selectedImageUri!!)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                CoroutineScope(Dispatchers.Main).launch {
                    progressBar.progress = progress
                }
            }
            val downloadUrl = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageRef.downloadUrl
            }.await().toString()

            val cropData = mapOf(
                "cropName" to cropName,
                "cropType" to cropType,
                "growingMethod" to growingMethod,
                "minPrice" to minPrice,
                "maxPrice" to maxPrice,
                "state" to state,
                "amount" to amount,
                "imageUrl" to downloadUrl
            )

            firestore.collection("SELLERS").document(email)
                .update("crops", FieldValue.arrayUnion(cropData))
                .await()

            withContext(Dispatchers.Main) {
                hideProgressDialog()
                Toast.makeText(context, "फसल डेटा सफलतापूर्वक अपलोड किया गया", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
                fetchCrops() // Refresh crops after upload
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                hideProgressDialog()
                Toast.makeText(context, "डेटा अपलोड करते समय त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    data class Crop(
        val name: String,
        val type: String,
        val growingMethod: String,
        val minPrice: Float,
        val maxPrice: Float,
        val state: String,
        val amount: Int,
        val imageUrl: String
    )

    inner class CropAdapter(private val crops: List<Crop>) : RecyclerView.Adapter<CropAdapter.CropViewHolder>() {

        inner class CropViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cropImage: ImageView = itemView.findViewById(R.id.cropImage)
            val cropName: TextView = itemView.findViewById(R.id.cropName)
            val cropType: TextView = itemView.findViewById(R.id.cropType)
            val growingMethod: TextView = itemView.findViewById(R.id.growingMethod)
            val price: TextView = itemView.findViewById(R.id.price)
            val state: TextView = itemView.findViewById(R.id.state)
            val amount: TextView = itemView.findViewById(R.id.amount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.crop_item, parent, false)
            return CropViewHolder(view)
        }

        override fun onBindViewHolder(holder: CropViewHolder, position: Int) {
            val crop = crops[position]
            holder.cropName.text = crop.name
            holder.cropType.text = crop.type
            holder.growingMethod.text = crop.growingMethod
            holder.price.text = "₹${crop.minPrice} - ₹${crop.maxPrice}"
            holder.state.text = crop.state
            holder.amount.text = "${crop.amount} क्विंटल"

            // Use Glide to load and display the crop image
            Glide.with(holder.itemView.context)
                .load(crop.imageUrl)
                .apply(RequestOptions()
                    .override(300, 300)
                    .centerCrop()
                    .placeholder(R.drawable.baseline_image_24)
                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(holder.cropImage)
        }

        override fun getItemCount() = crops.size
    }

    private fun showProgressDialog() {
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        progressDialog.dismiss()
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

    companion object {
        @JvmStatic
        fun newInstance() = SellerHomePage()
    }
}