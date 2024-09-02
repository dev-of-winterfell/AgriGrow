package com.example.agrigrow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.agrigrow.SharedViewModel

import android.widget.TextView
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
            view.findViewById<TextView>(R.id.price).text=("₹${crop.maxPrice}")
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

                val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

                sharedViewModel.setMaxPrice(crop.maxPrice)
                val cropDetailList = listOf(crop) // Create a list with the single non-null crop detail
                val buyerBargainFragment = BuyerBargain.newInstance(cropDetailList,crop.maxPrice)

                (activity as? MainActivity)?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragment_container, buyerBargainFragment)
                    ?.addToBackStack(null)
                    ?.commit()

                dismiss()
                (activity as? OnNegotiateClickListener)?.onNegotiateClick(crop)
                dismiss()
            } ?: run {
                // Handle the case where cropDetail is null, e.g., show an error message or do nothing
            }
        }

        view.findViewById<Button>(R.id.addtocart).setOnClickListener {
            // Handle add to cart button click
        }

        return view
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
