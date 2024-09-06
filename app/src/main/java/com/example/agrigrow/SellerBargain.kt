package com.example.agrigrow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class SellerBargain : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cropAdapter: CropListAdapterForSeller
    private val cropList = mutableListOf<homeFragment.CropDetail>()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_seller_bargain, container, false)

        recyclerView = view.findViewById(R.id.rvs)
        cropAdapter = CropListAdapterForSeller(cropList)
        recyclerView.adapter = cropAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Fetch crops for negotiation
        CoroutineScope(Dispatchers.Main).launch {
            fetchCropsForNegotiation()
        }

        return view
    }

    private suspend fun fetchCropsForNegotiation() = withContext(Dispatchers.IO) {
        try {
            val sellerUUID = fetchSellerUUID() ?: return@withContext
            val negotiationsRef = database.getReference("Negotiations").child(sellerUUID)

            negotiationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newCropList = mutableListOf<homeFragment.CropDetail>()

                    for (cropSnapshot in snapshot.children) {
                        val cropId = cropSnapshot.key ?: continue
                        CoroutineScope(Dispatchers.IO).launch {
                            val crop = fetchCropDetails(cropId)
                            if (crop != null) {
                                withContext(Dispatchers.Main) {
                                    newCropList.add(crop)
                                    cropList.clear()
                                    cropList.addAll(newCropList)
                                    cropAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SellerBargain", "Error fetching negotiations: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("SellerBargain", "Error fetching crops for negotiation: ", e)
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
                Log.e("SellerBargain", "Seller document not found for email: $currentUserEmail")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("SellerBargain", "Error fetching seller UUID: ", e)
            return@withContext null
        }
    }

    private suspend fun fetchCropDetails(cropId: String): homeFragment.CropDetail? = withContext(Dispatchers.IO) {
        try {
            val cropDoc = firestore.collection("Crops").document(cropId).get().await()
            if (cropDoc.exists()) {
                return@withContext homeFragment.CropDetail(
                    cropId = cropId,
                    cropName = cropDoc.getString("cropName") ?: "",
                    cropType = cropDoc.getString("cropType") ?: "",
                    amount = cropDoc.getLong("amount")?.toInt() ?: 0,
                    maxPrice = cropDoc.getDouble("maxPrice")?.toFloat() ?: 0f,
                    minPrice = cropDoc.getDouble("minPrice")?.toFloat() ?: 0f,
                    imageUrl = cropDoc.getString("imageUrl") ?: "",
                    sellerUUId = cropDoc.getString("sellerUUId") ?: ""
                )
            } else {
                Log.e("SellerBargain", "Crop document not found for ID: $cropId")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("SellerBargain", "Error fetching crop details: ", e)
            return@withContext null
        }
    }

    companion object {
        fun newInstance(): SellerBargain {
            return SellerBargain()
        }
    }
}