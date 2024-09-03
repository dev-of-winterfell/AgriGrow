package com.example.agrigrow

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CropListAdapter(private val cropList: MutableList<homeFragment.CropDetail>, private var maxPrice: Float) :
    RecyclerView.Adapter<CropListAdapter.CropViewHolder>() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crop, parent, false)
        return CropViewHolder(view)
    }

    override fun onBindViewHolder(holder: CropViewHolder, position: Int) {
        val crop = cropList[position]
        holder.bind(crop)
    }

    override fun getItemCount(): Int = cropList.size

    fun setMaxPrice(price: Float) {
        maxPrice = price
        notifyDataSetChanged()
    }

    inner class CropViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropName: TextView = itemView.findViewById(R.id.ropName)
        private val cropType: TextView = itemView.findViewById(R.id.ropType)
        private val cropPrice: EditText = itemView.findViewById(R.id.ropPrice)
        private val cropImage: ImageView = itemView.findViewById(R.id.ropImage)
        private val cropAmount: TextView = itemView.findViewById(R.id.ropquantity)
        private val sendNewPrice: Button = itemView.findViewById(R.id.sendnewPRICEtosellerfROMbUYER)
        private val addToCart: Button = itemView.findViewById(R.id.addtocart)

        fun bind(crop: homeFragment.CropDetail) {
            cropName.text = crop.name
            cropType.text = crop.type
            cropPrice.setText("₹${crop.maxPrice}")
            cropAmount.text = crop.amount.toString()
            Glide.with(itemView.context)
                .load(crop.imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_error_24)
                .apply(RequestOptions().transform(RoundedCorners(12)))
                .into(cropImage)

            fetchPriceFromRealtimeDB(crop.cropId)

            sendNewPrice.setOnClickListener {
                val newPriceString = cropPrice.text.toString().removePrefix("₹")
                val newPrice = newPriceString.toFloatOrNull() ?: run {
                    Toast.makeText(itemView.context, "Invalid price entered", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("CropListAdapter", "Invalid new price entered: $newPriceString")
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    val mspPrice = fetchMSPPrice(crop.name)
                    if (newPrice < mspPrice) {
                        Toast.makeText(itemView.context, "Price cannot be lower than MSP ₹$mspPrice", Toast.LENGTH_SHORT).show()
                        sendNewPrice.isEnabled = false
                        sendNewPrice.setBackgroundColor(itemView.context.getColor(R.color.translucent))
                    } else if (newPrice < maxPrice) {
                        Log.d("CropListAdapter", "New price is valid and less than max price.")
                        updatePriceInRealtimeDB(crop.cropId, newPrice)
                        cropPrice.setText("₹$newPrice") // Update the price in the EditText
                        sendNewPrice.isEnabled = true
                        sendNewPrice.setBackgroundColor(itemView.context.getColor(R.color.olive))
                    } else {
                        Toast.makeText(itemView.context, "Price should be less than the max price", Toast.LENGTH_SHORT).show()
                        Log.e("CropListAdapter", "New price $newPrice is greater than or equal to max price $maxPrice.")
                    }
                }
            }
        }

        private fun fetchPriceFromRealtimeDB(cropId: String) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("Crops").child(cropId).child("updatedPrice")
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val price = snapshot.getValue(Float::class.java) ?: cropList[adapterPosition].maxPrice
                    cropPrice.setText("₹$price")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CropListAdapter", "Error fetching price from Realtime Database: ${error.message}")
                    cropPrice.setText("₹${cropList[adapterPosition].maxPrice}")
                }
            })
        }


        private suspend fun updatePriceInRealtimeDB(cropId: String, newPrice: Float) = withContext(Dispatchers.IO) {
            try {
                if (newPrice <= maxPrice) {
                    val realtimeDbRef = FirebaseDatabase.getInstance().getReference("Crops").child(cropId)
                    realtimeDbRef.child("updatedPrice").setValue(newPrice).await()
                    Log.d("Firebase", "Price updated successfully")
                } else {
                    Log.e("CropListAdapter", "New price $newPrice is greater than the allowed max price $maxPrice for cropId: $cropId")
                }
            } catch (e: Exception) {
                Log.e("CropListAdapter", "Error updating price: ${e.message}")
            }
        }

        private suspend fun fetchMSPPrice(cropName: String): Float = withContext(Dispatchers.IO) {
            try {
                val mspRef = FirebaseFirestore.getInstance().collection("MSP_CROPS").document(cropName)
                val snapshot = mspRef.get().await()
                snapshot.getDouble("CROP_PRICE")?.toFloat() ?: 0f
            } catch (e: Exception) {
                Log.e("CropListAdapter", "Error fetching MSP price: ${e.message}")
                0f
            }
        }
    }
}
