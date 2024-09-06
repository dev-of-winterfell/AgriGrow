package com.example.agrigrow

import android.content.ContentValues.TAG
import android.text.Editable
import android.text.TextWatcher
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

        private var mspPrice: Float = 0f

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

            // Fetch the CROP_PRICE from Firestore
            CoroutineScope(Dispatchers.Main).launch {
                mspPrice = getCropPriceFromFirestore(crop.cropName)
            }

            // Fetch buyer UID and set up listeners
            fetchBuyerUid { buyerUid ->
                if (buyerUid != null) {
                    fetchNegotiatedPrice(crop.sellerUUId, buyerUid, crop.cropId)
                    // Fetch seller's name


                    // Set up TextWatcher to handle price changes
                    cropPrice.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            val newPriceString = s.toString().removePrefix("₹")
                            val newPrice = newPriceString.toFloatOrNull() ?: return

                            if (newPrice < mspPrice) {
                                sendNewPrice.isEnabled = false
                                addToCart.isEnabled = false
                                cropPrice.error = "कीमत एम.एस.पी. से कम नहीं हो सकती"
                            } else {
                                sendNewPrice.isEnabled = true
                                addToCart.isEnabled = true
                                cropPrice.error = null
                            }
                        }

                        override fun afterTextChanged(s: Editable?) {}
                    })

                    // Set listener to update the price
                    sendNewPrice.setOnClickListener {

                        val newPriceString = cropPrice.text.toString().removePrefix("₹")
                        val newPrice = newPriceString.toFloatOrNull() ?: run {
                            Toast.makeText(itemView.context, "अमान्य मूल्य दर्ज किया गया", Toast.LENGTH_SHORT)
                                .show()
                            return@setOnClickListener
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            updateMutualPriceInRealtimeDB(crop.sellerUUId, buyerUid, crop.cropId, newPrice)
                            Toast.makeText(
                                itemView.context,
                                "विक्रेता को नई कीमत भेज दी गई है",
                                Toast.LENGTH_SHORT
                            ).show()
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

            // Attach a ValueEventListener to listen for real-time changes
            negotiationRef.child("negotiatedPrice").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val price = snapshot.getValue(Float::class.java)
                        if (price != null) {
                            cropPrice.setText("₹$price")
                        } else {
                            cropPrice.setText("₹${cropList[adapterPosition].maxPrice}") // Fallback to max price
                        }
                    } catch (e: Exception) {
                        Log.e("fetchNegotiatedPrice", "Error processing negotiation data: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("fetchNegotiatedPrice", "Error fetching negotiated price: ${error.message}")
                }
            })
        }


        private suspend fun getCropPriceFromFirestore(cropName: String): Float = withContext(Dispatchers.IO) {
            return@withContext try {
                // Query the Firestore collection for a document where the crop name matches
                val querySnapshot = db.collection("MSP_CROPS")
                    .whereEqualTo("CROP_NAME", cropName)
                    .get()
                    .await()

                if (querySnapshot.documents.isNotEmpty()) {
                    // Assuming only one document will match the cropName
                    val document = querySnapshot.documents[0]
                    document.getDouble("CROP_PRICE")?.toFloat() ?: 0f
                } else {
                    Log.d("CropListAdapter", "No document found for cropName: $cropName")
                    0f
                }
            } catch (e: Exception) {
                Log.e("CropListAdapter", "Error fetching MSP price: ${e.message}")
                0f
            }
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
