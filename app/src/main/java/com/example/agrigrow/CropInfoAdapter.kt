package com.example.agrigrow

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

data class CropInfo(
    val cropType: String,
    val growingMethod: String,
    val amount: String,
    val state: String
)


class CropInfoAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<CropInfo>
) : ArrayAdapter<CropInfo>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View for the closed spinner
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val textView = view.findViewById<TextView>(R.id.spinner_text)
        textView.text = "PRODUCT DETAILS"
        return view
    }
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View for the dropdown list
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, recycledView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        val view = recycledView ?: LayoutInflater.from(context).inflate(resource, parent, false)


        val cropStateTextView = view.findViewById<TextView>(R.id.state)
//        val sellerNameTextView = view.findViewById<TextView>(R.id.sellerName)
        val cropTypeTextView = view.findViewById<TextView>(R.id.cropType)
        val growingMethodTextView = view.findViewById<TextView>(R.id.growingMethod)
        val amountTextView = view.findViewById<TextView>(R.id.amount)


     //   cropStateTextView.text = item?.cropState
//        sellerNameTextView.text = item?.sellerName
        cropTypeTextView.text = item?.cropType
        growingMethodTextView.text = item?.growingMethod
        amountTextView.text = item?.amount
        cropStateTextView.text = item?.state


        return view
    }
}
