package com.example.agrigrow

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

    private var maxPrice: Float = 0f
    private var cropDetail: homeFragment.CropDetail? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    interface OnNegotiateClickListener {
        fun onNegotiateClick(crop: homeFragment.CropDetail)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        cropDetail = arguments?.getParcelable("CROP_DETAIL")
        maxPrice = arguments?.getFloat("MAX_PRICE") ?: 0f
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_crop_options, container, false)

        cropDetail?.let { crop ->
            view.findViewById<TextView>(R.id.cropName).text = crop.name
            view.findViewById<TextView>(R.id.cropType).text = crop.type
            view.findViewById<TextView>(R.id.growingMethod).text = crop.growingMethod
            view.findViewById<TextView>(R.id.price).text = "₹${crop.maxPrice}"
            view.findViewById<TextView>(R.id.state).text = crop.state
            view.findViewById<TextView>(R.id.amount).text = "${crop.amount} क्विंटल"

            getContextSafely()?.let { context ->
                Glide.with(context)
                    .load(crop.imageUrl)
                    .placeholder(R.drawable.baseline_image_24)
                    .error(R.drawable.baseline_error_24)
                    .apply(RequestOptions().transform(RoundedCorners(12)))
                    .into(view.findViewById(R.id.cropImage))
            }
        }

        view.findViewById<Button>(R.id.negotiate).setOnClickListener {
            cropDetail?.let { crop ->
                storeNegotiationDataInFirestore(crop)
            } ?: run {
                getContextSafely()?.let { context ->
                    Toast.makeText(context, "Crop details are missing.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun getContextSafely(): Context? {
        return if (isAdded) context else null
    }

//    private fun storeNegotiationDataInFirestore(crop: homeFragment.CropDetail) {
//        val currentUser = auth.currentUser
//        currentUser?.let { user ->
//            val buyerDocRef = firestore.collection("BUYERS").document(user.uid)
//            val cropData = hashMapOf(
//                "cropId" to crop.cropId,
//                "name" to crop.name,
//                "type" to crop.type,
//                "growingMethod" to crop.growingMethod,
//                "state" to crop.state,
//                "amount" to crop.amount,
//                "imageUrl" to crop.imageUrl,
//                "negotiatedPrice" to crop.maxPrice // or some other price field if different
//            )
//
//            buyerDocRef.collection("NEGOTIATED_CROPS").add(cropData)
//                .addOnSuccessListener {
//                    if (isAdded) {
//                        Toast.makeText(context, "Negotiation data saved", Toast.LENGTH_SHORT).show()
//                        dismiss()
//                    }
//                }
//                    // Update SharedViewModel with the max price
//                    activity?.let { fragmentActivity ->
//                        val sharedViewModel = ViewModelProvider(fragmentActivity)[SharedViewModel::class.java]
//                        sharedViewModel.setMaxPrice(crop.maxPrice)
//                    }
//
//                    // Prepare to navigate to the BuyerBargain fragment
////                    val cropDetailList = listOf(crop)
////                    val buyerBargainFragment = BuyerBargain.newInstance(cropDetailList.toString(), crop.maxPrice)
////
////                    // Navigate to the BuyerBargain fragment using the SupportFragmentManager
////                    (activity as? MainActivity)?.supportFragmentManager?.beginTransaction()
////                        ?.replace(R.id.fragment_container, buyerBargainFragment)
////                        ?.addToBackStack(null)
////                        ?.commit()
////
////                    // Notify the activity about the negotiation click
////                    (activity as? OnNegotiateClickListener)?.onNegotiateClick(crop)
////
////                    dismiss() // Close the dialog after successful operation
////                }?.addOnFailureListener { e ->
////            getContextSafely()?.let { context ->
////                Toast.makeText(
////                    context,
////                    "Failed to save negotiation data: ${e.message}",
////                    Toast.LENGTH_SHORT
////                ).show()
////            }
////
////        }else run {
////            getContextSafely()?.let { context ->
////                Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
////            }
//     }
//    }
private fun storeNegotiationDataInFirestore(crop: homeFragment.CropDetail) {
    val user = auth.currentUser
    if (user != null) {
        // Reference to the user's document using their UID
        val userDocumentRef = user.email?.let { firestore.collection("BUYERS").document(it) }

        // Define the negotiation data to be stored
        val negotiationData = hashMapOf(
            "userId" to user.uid,
            "userName" to user.displayName,
            "cropId" to crop.cropId,
            "cropName" to crop.name,
            "cropType" to crop.type,
            "growingMethod" to crop.growingMethod,
            "state" to crop.state,
            "amount" to crop.amount,
            "maxPrice" to crop.maxPrice,
            "imageUrl" to crop.imageUrl
        )

        // Access or create a subcollection "NEGOTIATED_CROPS" inside the user's document
        userDocumentRef?.collection("NEGOTIATED_CROPS")?.add(negotiationData)?.addOnSuccessListener {
            if (isAdded) {
                context?.let {
                    Toast.makeText(it, "Negotiation data saved successfully", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }?.addOnFailureListener { e ->
            if (isAdded) {
                context?.let {
                    Toast.makeText(it, "Failed to save negotiation data: ${e.message}", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    } else {
        if (isAdded) {
            context?.let {
                Toast.makeText(it, "User not authenticated", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }
}

    companion object {
        fun newInstance(crop: homeFragment.CropDetail, maxPrice: Float): CropDataTransferFromBuyer {
            val fragment = CropDataTransferFromBuyer()
            val args = Bundle().apply {
                putParcelable("CROP_DETAIL", crop)
                putFloat("MAX_PRICE", maxPrice)
            }
            fragment.arguments = args
            return fragment
        }
    }
}