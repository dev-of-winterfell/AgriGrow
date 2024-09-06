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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CropListAdapter(private val cropList: MutableList<homeFragment.CropDetail>) :
    RecyclerView.Adapter<CropListAdapter.CropViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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

    inner class CropViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropName: TextView = itemView.findViewById(R.id.ropName)
        private val cropType: TextView = itemView.findViewById(R.id.ropType)
        private val cropPrice: EditText = itemView.findViewById(R.id.ropPrice)
        private val cropImage: ImageView = itemView.findViewById(R.id.ropImage)
        private val cropAmount: TextView = itemView.findViewById(R.id.ropquantity)
        private val sendNewPrice: Button = itemView.findViewById(R.id.sendnewPRICEtosellerfROMbUYER)
        private val addToCart: Button = itemView.findViewById(R.id.addtocart)

        fun bind(crop: homeFragment.CropDetail) {
            cropName.text = crop.cropName
            cropType.text = crop.cropType
            cropAmount.text = crop.amount.toString()
            Glide.with(itemView.context)
                .load(crop.imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_error_24)
                .apply(RequestOptions().transform(RoundedCorners(12)))
                .into(cropImage)

            // Fetch buyer UID from Firestore and update the price
            fetchBuyerUid { buyerUid ->
                if (buyerUid != null) {
                    fetchNegotiatedPrice(crop.sellerUUId, buyerUid, crop.cropId)

                    // Set listener to update the price
                    sendNewPrice.setOnClickListener {
                        val newPriceString = cropPrice.text.toString().removePrefix("₹")
                        val newPrice = newPriceString.toFloatOrNull() ?: run {
                            Toast.makeText(itemView.context, "Invalid price entered", Toast.LENGTH_SHORT)
                                .show()
                            return@setOnClickListener
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            updateMutualPriceInRealtimeDB(crop.sellerUUId, buyerUid, crop.cropId, newPrice)
                        }
                    }
                }
            }
        }

        private fun fetchBuyerUid(callback: (String?) -> Unit) {
            val email = auth.currentUser?.email
            if (email != null) {
                db.collection("BUYERS")
                    .document(email)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val buyerUid = document.getString("uuid")
                            callback(buyerUid)
                        } else {
                            Log.d("FetchBuyerUID", "No such document")
                            callback(null)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("FetchBuyerUID", "Error getting document: ", exception)
                        callback(null)
                    }
            } else {
                Log.d("FetchBuyerUID", "Current user email is null")
                callback(null)
            }
        }

        private fun fetchNegotiatedPrice(sellerUUID: String, buyerUUID: String, cropId: String) {
            val database = FirebaseDatabase.getInstance()
            val negotiationRef = database.getReference("Negotiations")
                .child(sellerUUID).child(cropId).child(buyerUUID)

            negotiationRef.child("negotiatedPrice").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val price = snapshot.getValue(Float::class.java)
                    if (price != null) {
                        cropPrice.setText("₹$price")
                    } else {
                        cropPrice.setText("₹${cropList[adapterPosition].maxPrice}") // Fallback to max price
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CropListAdapter", "Error fetching negotiated price: ${error.message}")
                }
            })
        }

        private suspend fun updateMutualPriceInRealtimeDB(
            sellerUUID: String,
            buyerUUID: String,
            cropId: String,
            newPrice: Float
        ) = withContext(Dispatchers.IO) {
            try {
                val database = FirebaseDatabase.getInstance()

                // Update price under seller's UUID
                val sellerNegotiationRef = database.getReference("Negotiations")
                    .child(sellerUUID).child(cropId).child(buyerUUID)
                sellerNegotiationRef.child("negotiatedPrice").setValue(newPrice).await()

                // Update price under buyer's UUID
                val buyerNegotiationRef = database.getReference("Negotiations")
                    .child(buyerUUID).child(cropId).child(sellerUUID)
                buyerNegotiationRef.child("negotiatedPrice").setValue(newPrice).await()

                Log.d("CropListAdapter", "Price updated for buyer $buyerUUID and seller $sellerUUID")
            } catch (e: Exception) {
                Log.e("CropListAdapter", "Error updating price: ${e.message}")
            }
        }
    }
}
