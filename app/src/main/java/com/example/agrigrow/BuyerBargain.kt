package com.example.agrigrow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class BuyerBargain : Fragment() {

    private var cropDetail: homeFragment.CropDetail? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cropDetail = arguments?.getParcelable("CROP_DETAIL")
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view= inflater.inflate(R.layout.fragment_connect, container, false)

        cropDetail?.let { crop ->
            // Display crop data in UI elements
            view.findViewById<TextView>(R.id.cropName).text = crop.name
            view.findViewById<TextView>(R.id.cropType).text = crop.type
            view.findViewById<TextView>(R.id.growingMethod).text = crop.growingMethod
            view.findViewById<TextView>(R.id.price).text = "₹${crop.maxPrice}"
            view.findViewById<TextView>(R.id.state).text = crop.state
            view.findViewById<TextView>(R.id.amount).text = "${crop.amount} क्विंटल"
        }

        return view
    }

    companion object {
        fun newInstance(cropDetail: homeFragment.CropDetail?): BuyerBargain {
            return BuyerBargain().apply {
                arguments = Bundle().apply {
                    putParcelable("CROP_DETAIL", cropDetail)
                }
            }
        }
    }
}
