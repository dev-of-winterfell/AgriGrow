package com.example.agrigrow

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class SellerHomePage : Fragment() {
    private lateinit var cropRecyclerView: RecyclerView
    private lateinit var cropAdapter: CropAdapter
    private val cropList = mutableListOf<Crop>()
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_seller_home, container, false)
val layout=view.findViewById<LinearLayout>(R.id.linearLayout2)
        val tv=layout.findViewById<TextView>(R.id.tv)
        // Retrieve the current logged-in user
        val userId = FirebaseAuth.getInstance().currentUser?.email

        if (userId != null) {
            // Fetch user data from Firestore
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("SELLERS").document(userId)

            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("Name") ?: "No Name Available"
                    tv.text = userName
                } else {
                    tv.text = "No user data available"
                }
            }.addOnFailureListener {
                tv.text = "Failed to load user data"
            }
        } else {
            tv.text = "User not logged in"
        }

        cropRecyclerView = view.findViewById(R.id.cropRecyclerView)
        cropAdapter = CropAdapter(cropList)
        cropRecyclerView.layoutManager = LinearLayoutManager(context)
        cropRecyclerView.adapter = cropAdapter

        lifecycleScope.launch {
            fetchCrops()
        }

        return view
    }
    private suspend fun fetchCrops() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            }
            return@withContext
        }

        val email = currentUser.email ?: return@withContext

        try {
            // Fetch seller data
            val sellerDoc = firestore.collection("SELLERS").document(email).get().await()
            if (sellerDoc != null && sellerDoc.exists()) {
                val sellerName = sellerDoc.getString("Name") ?: "Unknown Seller"

                // Fetch crops data
                val cropsList = sellerDoc.get("crops") as? List<Map<String, Any>> ?: emptyList()
                cropList.clear()

                for (cropData in cropsList) {
                    val crop = Crop(
                        name = cropData["cropName"] as String,
                        type = cropData["cropType"] as String,
                        growingMethod = cropData["growingMethod"] as String,
                        minPrice = (cropData["minPrice"] as Number).toFloat(),
                        maxPrice = (cropData["maxPrice"] as Number).toFloat(),
                        state = cropData["state"] as String,
                        amount = (cropData["amount"] as Number).toInt(),
                        imageUrl = cropData["imageUrl"] as String,
                        ownerName = sellerName // Set owner name here
                    )
                    cropList.add(crop)
                }

                withContext(Dispatchers.Main) {
                    cropAdapter.notifyDataSetChanged()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No seller data found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error fetching crops: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    data class Crop(
        val name: String,
        val type: String,
        val growingMethod: String,
        val minPrice: Float,
        val maxPrice: Float,
        val state: String,
        val amount: Int,
        val imageUrl: String,
        val ownerName: String
    )

    inner class CropAdapter(private val crops: List<Crop>) : RecyclerView.Adapter<CropAdapter.CropViewHolder>() {

        inner class CropViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
           val ownerName: TextView = itemView.findViewById(R.id.ownername)
            val cropImage: ImageView = itemView.findViewById(R.id.cropImage)
            val cropName: TextView = itemView.findViewById(R.id.cropName)
            val cropType: TextView = itemView.findViewById(R.id.cropType)
            val growingMethod: TextView = itemView.findViewById(R.id.growingMethod)
            val price: TextView = itemView.findViewById(R.id.price)
            val state: TextView = itemView.findViewById(R.id.state)
            val amount: TextView = itemView.findViewById(R.id.amount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.crop_item, parent, false)
            return CropViewHolder(view)
        }

        override fun onBindViewHolder(holder: CropViewHolder, position: Int) {
            val crop = crops[position]
            holder.ownerName.text = crop.ownerName
            holder.cropName.text = crop.name
            holder.cropType.text = crop.type
            holder.growingMethod.text = crop.growingMethod
            holder.price.text = "₹${crop.minPrice} - ₹${crop.maxPrice}"
            holder.state.text = crop.state
            holder.amount.text = "${crop.amount} Quintal"

            // Use Glide to load and display the crop image
            Glide.with(holder.itemView.context)
                .load(crop.imageUrl)
                .apply(RequestOptions().override(300, 300).centerCrop())
                .into(holder.cropImage)
        }

        override fun getItemCount() = crops.size
    }

}