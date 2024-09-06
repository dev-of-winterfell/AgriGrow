package com.example.agrigrow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BuyerBargain : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var cropAdapter: CropListAdapter
    private val cropList = mutableListOf<homeFragment.CropDetail>()
    private lateinit var userNameTextView: TextView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            val savedCrops = it.getParcelableArrayList<homeFragment.CropDetail>("CROP_LIST")
            savedCrops?.let { crops ->
                cropList.addAll(crops)
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("CROP_LIST", ArrayList(cropList))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connect, container, false) // Use correct layout

        recyclerView = view.findViewById(R.id.rv)
        cropAdapter = CropListAdapter(cropList)
        userNameTextView = view.findViewById(R.id.tv) //
        recyclerView.adapter = cropAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        sharedViewModel.crops.observe(viewLifecycleOwner) { crops ->
            cropList.clear()
            cropList.addAll(crops)
            cropAdapter.notifyDataSetChanged()
        }
        sharedViewModel.crops.observe(viewLifecycleOwner) { crops ->
            cropList.clear()
            cropList.addAll(crops)
            cropAdapter.notifyDataSetChanged()
        }

        // Fetch crop details from Firestore and update the RecyclerView
        CoroutineScope(Dispatchers.Main).launch {
            val fetchedCropList = fetchCropDetailsFromFirestore()
            cropList.clear()
            cropList.addAll(fetchedCropList)
            cropAdapter.notifyDataSetChanged()
        }

        CoroutineScope(Dispatchers.Main).launch {
            val userName = fetchUserName()
            userNameTextView.text = userName
        }
        return view
    }
    private suspend fun fetchUserName(): String = withContext(Dispatchers.IO) {
        try {
            val userEmail = auth.currentUser?.email ?: throw Exception("User not authenticated")
            val userDocument = firestore.collection("BUYERS").document(userEmail).get().await()
            userDocument.getString("Name") ?: "Unknown User"
        } catch (e: Exception) {
            Log.e("com.example.agrigrow.BuyerBargain", "Error fetching user name: ${e.message}")
            "Unknown User" // Return a default value in case of error
        }
    }
    private suspend fun fetchCropDetailsFromFirestore(): List<homeFragment.CropDetail> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.email ?: throw Exception("User not authenticated")
            val cropsList = mutableListOf<homeFragment.CropDetail>()

            // Reference to the NegotiatedCrops subcollection for the current user
            val cropsRef = firestore.collection("BUYERS")
                .document(userId)
                .collection("NEGOTIATED_CROPS")

            // Fetch all crop documents
            val snapshot = cropsRef.get().await()
            for (document in snapshot.documents) {
                val crop = document.toObject(homeFragment.CropDetail::class.java)
                val sellerUid = document.getString("uuid") // Fetch sellerUid from the document
                crop?.let {
                    if (sellerUid != null) {
                        it.sellerUUId = sellerUid
                    } // Add sellerUid to CropDetail
                    cropsList.add(it)
                }
            }
            cropsList // Return the list of crops fetched from Firestore
        } catch (e: Exception) {
            Log.e("com.example.agrigrow.BuyerBargain", "Error fetching negotiated crops from Firestore: ${e.message}")
            emptyList() // Return an empty list in case of error
        }
    }



    companion object {
        private const val ARG_CROP_ID = "crop_id"
        private const val ARG_MAX_PRICE = "max_price"

        fun newInstance(cropId: String, maxPrice: Float, sellerUUId: String): BuyerBargain {
            val fragment = BuyerBargain()
            val args = Bundle()
            args.putString(ARG_CROP_ID, cropId)
            args.putFloat(ARG_MAX_PRICE, maxPrice)
            fragment.arguments = args
            return fragment
        }
    }

}
