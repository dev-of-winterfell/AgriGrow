package com.example.agrigrow

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cropDetail = arguments?.getParcelable("CROP_DETAIL")
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

            Glide.with(requireContext())
                .load(crop.imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_error_24)
                .apply(RequestOptions().transform(RoundedCorners(12)))
                .into(view.findViewById(R.id.cropImage))
        }

        view.findViewById<Button>(R.id.negotiate).setOnClickListener {
            cropDetail?.let { crop ->
                storeNegotiationDataInFirestore(crop) // Store the data in Firestore

                val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
                sharedViewModel.setMaxPrice(crop.maxPrice)

                val cropDetailList = listOf(crop)
                val buyerBargainFragment = BuyerBargain.newInstance(cropDetailList, crop.maxPrice)

                (activity as? MainActivity)?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragment_container, buyerBargainFragment)
                    ?.addToBackStack(null)
                    ?.commit()

                dismiss()
                (activity as? OnNegotiateClickListener)?.onNegotiateClick(crop)
                dismiss()
            } ?: run {
                // Handle the case where cropDetail is null
            }
        }

        view.findViewById<Button>(R.id.addtocart).setOnClickListener {
            // Handle add to cart button click
        }

        return view
    }

    private fun storeNegotiationDataInFirestore(crop: homeFragment.CropDetail) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val buyerDocRef = firestore.collection("BUYERS").document(user.uid)
            val cropData = hashMapOf(
                "cropId" to crop.cropId,
                "name" to crop.name,
                "type" to crop.type,
                "growingMethod" to crop.growingMethod,
                "state" to crop.state,
                "amount" to crop.amount,
                "imageUrl" to crop.imageUrl,
                "negotiatedPrice" to crop.maxPrice // or some other price field if different
            )

            buyerDocRef.collection("Negotiations").add(cropData)
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(context, "Negotiation data saved", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(context, "Failed to save negotiation data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }



    companion object {
        fun newInstance(cropDetail: homeFragment.CropDetail): CropDataTransferFromBuyer {
            return CropDataTransferFromBuyer().apply {
                arguments = Bundle().apply {
                    putParcelable("CROP_DETAIL", cropDetail)
                }
            }
        }
    }

    interface OnNegotiateClickListener {
        fun onNegotiateClick(cropDetail: homeFragment.CropDetail?)
    }
}
