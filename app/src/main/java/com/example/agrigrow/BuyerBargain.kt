package com.example.agrigrow

import android.app.Notification
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class BuyerBargain : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var cropAdapter: CropListAdapter
    private val cropList = mutableListOf<homeFragment.CropDetail>()
    private lateinit var userNameTextView: TextView
    private lateinit var emptyTextView: TextView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var notificationsListener: ValueEventListener? = null
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
        emptyTextView = view.findViewById(R.id.tv3)
        recyclerView = view.findViewById(R.id.rv)
        cropAdapter = CropListAdapter(cropList){ cropDetail, newPrice ->
            // This lambda is our onAddToCart callback
            addToCart(cropDetail, newPrice)
        }
        userNameTextView = view.findViewById(R.id.tv) //
        recyclerView.adapter = cropAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        sharedViewModel.crops.observe(viewLifecycleOwner) { crops ->
            cropList.clear()
            cropList.addAll(crops)
            cropAdapter.notifyDataSetChanged()
            updateEmptyViewVisibility()
        }


        // Fetch crop details from Firestore and update the RecyclerView
        CoroutineScope(Dispatchers.Main).launch {
            val fetchedCropList = fetchCropDetailsFromFirestore()
            cropList.clear()
            cropList.addAll(fetchedCropList)
            cropAdapter.notifyDataSetChanged()
            updateEmptyViewVisibility()
        }

        CoroutineScope(Dispatchers.Main).launch {
            val userName = fetchUserName()
            userNameTextView.text = userName
        }


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val activity = activity as? BuyerLandingPage
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





        //  startListeningForNotifications()
        return view
    }
    private fun updateEmptyViewVisibility() {
        if (cropList.isNotEmpty()) {
            emptyTextView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.VISIBLE
        }
    }

//    private fun startListeningForNotifications() {
//        val userEmail = auth.currentUser?.email ?: return
//        val db = FirebaseDatabase.getInstance()
//        // Encode the email address to make it a valid Firebase path
//        val encodedEmail = userEmail.replace(".", "_")
//
//        // Reference to the Notifications node for the current user
//        val notificationsRef = db.getReference("Notifications").child(encodedEmail)
//
//        notificationsListener = notificationsRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                for (notificationSnapshot in snapshot.children) {
//                    val notification = notificationSnapshot.getValue(Notification::class.java)
//                    notification?.let {
//                        handleNotification(it)
//                    }
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("BuyerBargain", "Error listening for notifications: ${error.message}")
//            }
//        })
//    }
//
//
//    private fun handleNotification(notification: Notification) {
//        // Update the UI or show a Toast notification
//        Toast.makeText(context, "Price accepted by seller", Toast.LENGTH_SHORT).show()
//        // You can also update the RecyclerView or other UI elements here
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        // Remove the listener when the view is destroyed
//
//
//        val db = FirebaseDatabase.getInstance()
//        val userEmail = auth.currentUser?.email ?: return
//        val encodedEmail = userEmail.replace(".", "_")
//        val notificationsRef = db.getReference("Notifications").child(encodedEmail)
//        notificationsListener?.let { notificationsRef.removeEventListener(it) }
//    }


    private fun addToCart(cropDetail: homeFragment.CropDetail, newPrice: Float) {
        Log.d(TAG, "Adding to cart: ${cropDetail.cropName}, Price: $newPrice")

        // Create a bundle to pass data to the AddToCart fragment
        val bundle = Bundle().apply {
            putParcelable("cropDetail", cropDetail)
            putFloat("newPrice", newPrice)
        }
        Log.d(TAG, "Bundle created with crop details: ${cropDetail.cropName}, ${cropDetail.cropType}, $newPrice")
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