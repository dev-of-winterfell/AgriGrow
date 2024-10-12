package com.example.agrigrow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.PropertyName

data class CartItem(
    @get:PropertyName("cropId") @set:PropertyName("cropId") var cropId: String = "",
    @get:PropertyName("cropName") @set:PropertyName("cropName") var cropName: String = "",
    @get:PropertyName("cropType") @set:PropertyName("cropType") var cropType: String = "",
    @get:PropertyName("amount") @set:PropertyName("amount") var amount: Int = 0,
    @get:PropertyName("newPrice") @set:PropertyName("newPrice") var newPrice: Float = 0f,
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String = ""
)

class CartAdapter(private val cartItems: List<CartItem>) :
    RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropName: TextView = itemView.findViewById(R.id.CropName)
        private val cropType: TextView = itemView.findViewById(R.id.CropType)
        private val cropAmount: TextView = itemView.findViewById(R.id.Cropquantity)
        private val cropPrice: TextView = itemView.findViewById(R.id.CropPrice)
        private val cropImage: ImageView = itemView.findViewById(R.id.CropImage)

        fun bind(cartItem: CartItem) {
            cropName.text = cartItem.cropName
            cropType.text = cartItem.cropType
            cropAmount.text = "${cartItem.amount} क्विंटल"
            cropPrice.text = "₹${cartItem.newPrice}"

            // Load image using Glide
            Glide.with(itemView.context)
                .load(cartItem.imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_error_24)
                .into(cropImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cart_items, parent, false)  // Assuming you have a layout for the cart item
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size
}
