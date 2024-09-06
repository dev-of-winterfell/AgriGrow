package com.example.agrigrow

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CropDataTransferFromBuyer : DialogFragment() {

    private var cropDetail: homeFragment.CropDetail? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        cropDetail = arguments?.getParcelable("CROP_DETAIL")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_crop_options, container, false)


updateUI(view)
        return view
    }

    private fun updateUI(view: View) {
        cropDetail?.let { crop ->
            view.findViewById<TextView>(R.id.cropName).text = crop.cropName
            view.findViewById<TextView>(R.id.cropType).text = crop.cropType
            view.findViewById<TextView>(R.id.growingMethod).text = crop.growingMethod
            view.findViewById<TextView>(R.id.price).text = "₹${crop.maxPrice}"
            view.findViewById<TextView>(R.id.state).text = crop.state
            view.findViewById<TextView>(R.id.amount).text = "${crop.amount} क्विंटल"

            // Load image using Glide
            val cropImageView = view.findViewById<ImageView>(R.id.cropImage1)
            Glide.with(requireContext())
                .load(crop.imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.pikaso_embed_a_middleaged_indian_farmer_wearing_a_turban_and_tr_removebg_preview)
                .apply(RequestOptions().transform(RoundedCorners(12)))
                .apply(RequestOptions().centerCrop())
                .into(cropImageView)

            view.findViewById<Button>(R.id.negotiate).setOnClickListener {
                Log.d("CropDataTransfer", "Negotiate button clicked.")
                Log.d("CropDataTransfer", "Storing negotiation data for cropId: ${crop.cropId}")
                storeNegotiationDataInFirestore(crop)
            }

        }
    }
    fun updateCropDetail(newCropDetail: homeFragment.CropDetail) {
        cropDetail = newCropDetail
        view?.let { updateUI(it) }
    }
    private fun storeNegotiationDataInFirestore(crop: homeFragment.CropDetail) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email ?: run {
                Log.e("Firestore", "User email is null.")
                Toast.makeText(context, "User email is null.", Toast.LENGTH_SHORT).show()
                return
            }
            val userDocumentRef = firestore.collection("BUYERS").document(userEmail)

            val negotiationData = hashMapOf(

                "userName" to user.displayName,
                "cropId" to crop.cropId,
                "cropName" to crop.cropName,
                "cropType" to crop.cropType,
                "growingMethod" to crop.growingMethod,
                "state" to crop.state,
                "amount" to crop.amount,
                "maxPrice" to crop.maxPrice,
                "imageUrl" to crop.imageUrl,
                "sellerUUId" to crop.sellerUUId
            )

            Log.d("Firestore", "Attempting to store data: $negotiationData")

            userDocumentRef.collection("NEGOTIATED_CROPS").document(crop.cropId).set(negotiationData)
                .addOnSuccessListener {
                    Log.d("Firestore", "Negotiation data saved successfully.for cropID:${crop.cropId} and sellerUUId: ${crop.sellerUUId}")
                    Toast.makeText(context, "Negotiation data saved successfully", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to save negotiation data", e)
                    Toast.makeText(context, "Failed to save negotiation data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("Firestore", "User not authenticated.")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
        val viewModel = ViewModelProvider(requireActivity()).get(CropViewModel::class.java)
        val currentList = viewModel.selectedCrops.value?.toMutableList() ?: mutableListOf()
        currentList.add(crop)
        viewModel.updateSelectedCrops(currentList)
    }

    companion object {
        fun newInstance(cropDetail: homeFragment.CropDetail): CropDataTransferFromBuyer {
            val fragment = CropDataTransferFromBuyer()
            val args = Bundle()
            args.putParcelable("CROP_DETAIL", cropDetail)
            fragment.arguments = args
            return fragment
        }
    }
}