package com.example.agrigrow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        val sellerName=view.findViewById<TextView>(R.id.tv)
        setupSellerName(sellerName)
        Log.d("SellerBargain", "Initializing RecyclerView and Adapter")

        CoroutineScope(Dispatchers.Main).launch {
            fetchCropsForNegotiation()
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val activity = activity as? SellerLandingPage
                if (dy > 0) {
                    // User is scrolling up -> Hide Bottom Navigation
                    Log.d("homeFragment", "User scrolling up, hiding BottomNav")
                    activity?.hideBottomNavBar()
                } else if (dy < 0) {
                    // User is scrolling down -> Show Bottom Navigation
                    Log.d("homeFragment", "User scrolling down, showing BottomNav")
                    activity?.showBottomNavBar()
                }
            }
        })



        return view
    }

    private suspend fun fetchCropsForNegotiation() = withContext(Dispatchers.IO) {
        Log.d("SellerBargain", "Starting to fetch crops for negotiation")

        try {
            val sellerEmail = auth.currentUser?.email ?: run {
                Log.e("SellerBargain", "User email is null, exiting fetch")
                return@withContext
            }

            val sellerUUID = fetchSellerUUID(sellerEmail) ?: run {
                Log.e("SellerBargain", "Failed to fetch seller UUID, exiting fetch")
                return@withContext
            }

            Log.d("SellerBargain", "Current user email: $sellerEmail, UUID: $sellerUUID")

            val negotiationsRef = database.getReference("Negotiations").child(sellerUUID)

            negotiationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("SellerBargain", "Received negotiations data snapshot: ${snapshot.childrenCount} children found")

                    CoroutineScope(Dispatchers.IO).launch {
                        val newCropSet = mutableSetOf<homeFragment.CropDetail>()

                        for (cropSnapshot in snapshot.children) {
                            val cropId = cropSnapshot.key ?: continue
                            Log.d("SellerBargain", "Fetching crop details for crop ID: $cropId")

                            val crop = fetchCropDetails(sellerEmail, cropId)
                            if (crop != null) {
                                Log.d("SellerBargain", "Successfully fetched crop: $crop")
                                newCropSet.add(crop)
                            } else {
                                Log.e("SellerBargain", "Crop details are null for crop ID: $cropId")
                            }
                        }

                        withContext(Dispatchers.Main) {
                            cropAdapter.updateCrops(newCropSet.toList())
                            Log.d("SellerBargain", "Updated crop list and notified adapter")
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

    private suspend fun fetchSellerUUID(email: String): String? = withContext(Dispatchers.IO) {
        try {
            val sellerDoc = firestore.collection("SELLERS").document(email).get().await()
            return@withContext sellerDoc.getString("uuid")
        } catch (e: Exception) {
            Log.e("SellerBargain", "Error fetching seller UUID: ${e.message}")
            return@withContext null
        }
    }
    private suspend fun fetchCropDetails(sellerEmail: String, cropId: String): homeFragment.CropDetail? = withContext(Dispatchers.IO) {
        Log.d("SellerBargain", "Fetching crop details for seller: $sellerEmail and crop ID: $cropId")

        try {
            val userId = auth.currentUser?.email ?: throw Exception("User not authenticated")
            val sellerDoc = firestore.collection("SELLERS").document(userId).get().await()
            if (sellerDoc.exists()) {
                Log.d("SellerBargain", "Seller document found for email: $sellerEmail")

                val crops = sellerDoc.get("crops") as? List<Map<String, Any>> ?: run {
                    Log.e("SellerBargain", "No negotiated crops found for seller: $sellerEmail")
                    return@withContext null
                }

                val matchingCrop = crops.find { it["cropId"] as? String == cropId }

                if (matchingCrop != null) {
                    Log.d("SellerBargain", "Matching crop found for cropId: $cropId")

                    return@withContext homeFragment.CropDetail(
                        cropId = cropId,
                        cropName = matchingCrop["cropName"] as? String ?: "",
                        cropType = matchingCrop["cropType"] as? String ?: "",
                        amount = (matchingCrop["amount"] as? Long)?.toInt() ?: 0,
                        maxPrice = (matchingCrop["maxPrice"] as? Double)?.toFloat() ?: 0f,
                        minPrice = (matchingCrop["minPrice"] as? Double)?.toFloat() ?: 0f,
                        imageUrl = matchingCrop["imageUrl"] as? String ?: "",
                        sellerUUId = sellerDoc.getString("uuid") ?: ""
                    )
                } else {
                    Log.e("SellerBargain", "Crop not found in seller's crops: $cropId")
                    return@withContext null
                }
            } else {
                Log.e("SellerBargain", "Seller document not found for email: $sellerEmail")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("SellerBargain", "Error fetching crop details: ", e)
            return@withContext null
        }
    }
    private fun setupSellerName(tvBuyer: TextView) {
        val userId = FirebaseAuth.getInstance().currentUser?.email
        if (userId != null) {
            firestore.collection("SELLERS").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("Name") ?: "No Name Available"
                        tvBuyer.text = userName
                    } else {
                        tvBuyer.text = "No user data available"
                    }
                }.addOnFailureListener {
                    tvBuyer.text = "Failed to load user data"
                }
        } else {
            tvBuyer.text = "User not logged in"
        }
    }

    companion object {
        fun newInstance(): SellerBargain {
            return SellerBargain()
        }
    }
}