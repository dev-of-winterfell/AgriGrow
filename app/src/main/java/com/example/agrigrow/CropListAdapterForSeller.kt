package com.example.agrigrow

import android.content.ContentValues.TAG
import android.provider.ContactsContract.CommonDataKinds.Email
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

class CropListAdapterForSeller(private var cropList: MutableList<homeFragment.CropDetail>) :
    RecyclerView.Adapter<CropListAdapterForSeller.CropViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crop_seller, parent, false)
        return CropViewHolder(view)
    }

    override fun onBindViewHolder(holder: CropViewHolder, position: Int) {
        val crop = cropList[position]
        holder.bind(crop)
    }

    override fun getItemCount(): Int = cropList.size

    fun updateCrops(newCropList: List<homeFragment.CropDetail>) {
        cropList = newCropList.distinctBy { it.cropId }.toMutableList()
        notifyDataSetChanged()
    }

    inner class CropViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropName: TextView = itemView.findViewById(R.id.CCropName)
        private val cropType: TextView = itemView.findViewById(R.id.CCropType)
        private val cropPrice: EditText = itemView.findViewById(R.id.CCropPrice)
        private val cropImage: ImageView = itemView.findViewById(R.id.CCropImage)
        private val cropAmount: TextView = itemView.findViewById(R.id.CCropquantity)
        private val sendNewPrice: Button = itemView.findViewById(R.id.sendnewPRICEtoseller)
        private val acceptbtn: Button = itemView.findViewById(R.id.acceptbtn)
        private val buyerName: TextView = itemView.findViewById(R.id.BuyerName)

        fun bind(crop: homeFragment.CropDetail) {

            cropName.text = crop.cropName
            cropType.text = crop.cropType
            cropAmount.text = "${crop.amount} क्विंटल"

            Glide.with(itemView.context)
                .load(crop.imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_error_24)
                .apply(RequestOptions().transform(RoundedCorners(12)))
                .into(cropImage)

            CoroutineScope(Dispatchers.Main).launch {
                fetchNegotiationData(crop)
            }

            sendNewPrice.setOnClickListener {
                val newPriceString = cropPrice.text.toString().removePrefix("₹")
                val newPrice = newPriceString.toFloatOrNull()
                if (newPrice == null) {
                    Toast.makeText(itemView.context, "Invalid price entered", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    updatePriceInDatabase(crop, newPrice)
                }
            }


//            acceptbtn.setOnClickListener {
//                CoroutineScope(Dispatchers.Main).launch {
//                    sendNotificationToBuyer(crop,crop.maxPrice)
//                }
//            }

        }

        private suspend fun sendNotificationToBuyer(crop: homeFragment.CropDetail, newPrice: Float) {
            val sellerUUID = fetchSellerUUID() ?: return
            val notificationsRef = database.getReference("Notifications")
            val newNotificationRef = notificationsRef.push()

            // Create a notification object
            val notification = mapOf(
                "type" to "price_update",
                "cropId" to crop.cropId,
                "newPrice" to newPrice,
                "timestamp" to System.currentTimeMillis()
            )

            newNotificationRef.setValue(notification)
                .addOnSuccessListener {
                    Log.d("sendNotificationToBuyer", "Notification sent successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("sendNotificationToBuyer", "Error sending notification: ${e.message}")
                }
        }

        private suspend fun fetchNegotiationData(crop: homeFragment.CropDetail) {
            try {
                val sellerUUID = fetchSellerUUID() ?: return
                val negotiationsRef =
                    database.getReference("Negotiations").child(sellerUUID).child(crop.cropId)

                negotiationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            for (buyerSnapshot in snapshot.children) {
                                val buyerUUID = buyerSnapshot.key
                                val negotiatedPrice = buyerSnapshot.child("negotiatedPrice")
                                    .getValue(Float::class.java)

                                if (buyerUUID != null && negotiatedPrice != null) {
                                    cropPrice.setText("₹$negotiatedPrice")
                                    fetchBuyerNameFromFirestore(buyerUUID)
                                    break  // Assuming we're dealing with one buyer at a time
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "fetchNegotiationData",
                                "Error processing negotiation data: ${e.message}"
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "fetchNegotiationData",
                            "Error fetching negotiation data: ${error.message}"
                        )
                    }
                })
            } catch (e: Exception) {
                Log.e("fetchNegotiationData", "Error fetching seller UUID: ${e.message}")
            }
        }


        private fun fetchBuyerNameFromFirestore(buyerUUID: String) {
            Log.d(TAG, "fetchBuyerNameFromFirestore: Fetching name for buyer UUID: $buyerUUID")
            firestore.collection("BUYERS")
                .whereEqualTo("uuid", buyerUUID)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val name = document.getString("Name")
                        val email = document.id  // The document ID is the email
                        Log.d(
                            TAG,
                            "fetchBuyerNameFromFirestore: Retrieved name for buyer $buyerUUID: $name, Email: $email"
                        )
                        buyerName.text = name ?: "Unknown Buyer"
                    } else {
                        Log.w(
                            TAG,
                            "fetchBuyerNameFromFirestore: No document found for buyer UUID: $buyerUUID"
                        )
                        buyerName.text = "Unknown Buyer"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching buyer name for UUID $buyerUUID: ${e.message}", e)
                    buyerName.text = "Error Fetching Name"
                }
        }

        private suspend fun fetchSellerUUID(): String? = withContext(Dispatchers.IO) {
            try {
                val currentUserEmail = auth.currentUser?.email ?: return@withContext null
                val sellerDoc = firestore.collection("SELLERS")
                    .whereEqualTo("Email", currentUserEmail)
                    .get()
                    .await()

                if (!sellerDoc.isEmpty) {
                    return@withContext sellerDoc.documents[0].getString("uuid")
                } else {
                    Log.e(
                        "CropListAdapterForSeller",
                        "Seller document not found for email: $currentUserEmail"
                    )
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("CropListAdapterForSeller", "Error fetching seller UUID: ", e)
                return@withContext null
            }
        }

        private suspend fun updatePriceInDatabase(crop: homeFragment.CropDetail, newPrice: Float) =
            withContext(Dispatchers.IO) {
                try {
                    val sellerUUID = fetchSellerUUID() ?: return@withContext
                    val negotiationsRef = database.getReference("Negotiations")

                    // Update under seller's node
                    negotiationsRef.child(sellerUUID).child(crop.cropId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (buyerSnapshot in snapshot.children) {
                                    val buyerUUID = buyerSnapshot.key
                                    if (buyerUUID != null) {
                                        // Update price for seller
                                        negotiationsRef.child(sellerUUID).child(crop.cropId)
                                            .child(buyerUUID)
                                            .child("negotiatedPrice").setValue(newPrice)

                                        // Update price for buyer
                                        negotiationsRef.child(buyerUUID).child(crop.cropId)
                                            .child(sellerUUID)
                                            .child("negotiatedPrice").setValue(newPrice)

//                                        // Send notification to buyer
//                                        val notificationRef =
//                                            database.getReference("Users").child(buyerUUID)
//                                                .child("notifications")
//                                        notificationRef.push().setValue(
//                                            mapOf(
//                                                "type" to "price_update",
//                                                "cropId" to crop.cropId,
//                                                "newPrice" to newPrice
//                                            )
//                                        )

                                        break  // Assuming we're dealing with one buyer at a time
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(
                                    "com.example.agrigrow.CropListAdapterForSeller",
                                    "Error updating price: ${error.message}"
                                )
                            }
                        })

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            itemView.context,
                            "Price updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(
                        "com.example.agrigrow.CropListAdapterForSeller",
                        "Error updating price: ",
                        e
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            itemView.context,
                            "Failed to update price",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }
}