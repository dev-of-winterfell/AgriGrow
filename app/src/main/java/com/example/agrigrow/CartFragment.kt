package com.example.agrigrow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CartFragment : Fragment() {
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var userNameTextView: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private var totalPrice: Float = 0f
private lateinit var tv:TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)
        cartRecyclerView = view.findViewById(R.id.rvc)
        cartRecyclerView.layoutManager = LinearLayoutManager(context)
        userNameTextView = view.findViewById(R.id.tvbuyer) //

        cartAdapter = CartAdapter(cartItems)
        cartRecyclerView.adapter = cartAdapter

        loadCartItems()  // Call the function to load cart items
        shimmerFrameLayout = view.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()
        tv = view.findViewById(R.id.textView36)

        CoroutineScope(Dispatchers.Main).launch {
            val userName = fetchUserName()
            userNameTextView.text = userName
        }

        cartRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

        val paymentButton = view.findViewById<Button>(R.id.PaymentBtn)
        paymentButton.setOnClickListener {
            // Redirect to payment activity and pass the total price
            val intent = Intent(requireContext(), Payment::class.java)
            intent.putExtra("TOTAL_PRICE", totalPrice)
            startActivity(intent)
        }
        return view
    }
    override fun onResume() {
        super.onResume()
        shimmerFrameLayout.startShimmer()
    }

    override fun onPause() {
        shimmerFrameLayout.stopShimmer()
        super.onPause()
    }

    private fun updateEmptyViewVisibility() {
        if (cartItems.isNotEmpty()) {
            tv.visibility = View.GONE
        } else {
            tv.visibility = View.VISIBLE
        }
    }


    private suspend fun fetchUserName(): String = withContext(Dispatchers.IO) {
        try {
            val userEmail = auth.currentUser?.email ?: throw Exception("User not authenticated")
            val userDocument = db.collection("BUYERS").document(userEmail).get().await()
            userDocument.getString("Name") ?: "Unknown User"
        } catch (e: Exception) {
            Log.e("com.example.agrigrow.BuyerBargain", "Error fetching user name: ${e.message}")
            "Unknown User" // Return a default value in case of error
        }
    }
    private fun loadCartItems() {
        val user = auth.currentUser
        val userEmail = user?.email ?: return

        db.collection("BUYERS").document(userEmail)
            .collection("CART_ITEMS")
            .get()
            .addOnSuccessListener { documents ->
                cartItems.clear()
                var totalPrice = 0f
                for (document in documents) {
                    val cartItem = document.toObject(CartItem::class.java)
                    cartItems.add(cartItem)
                    val itemTotal = cartItem.amount * cartItem.newPrice
                    totalPrice += itemTotal

                }
                cartAdapter.notifyDataSetChanged()
                this.totalPrice = totalPrice
                updateEmptyViewVisibility()
            // Notify the adapter to refresh the RecyclerView
            }
            .addOnFailureListener { exception ->
                // Handle the error
                Toast.makeText(
                    context,
                    "Failed to load cart items: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                updateEmptyViewVisibility()
            }
    }
}