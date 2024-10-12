package com.example

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.agrigrow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
const val ARG_PARAM1 = "param1"
const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [sellerProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class sellerProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_seller_profile, container, false)

        // Initialize Firebase Auth and Firestore
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Retrieve UI elements
        val profileImageView = view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)
        val nameTextView = view.findViewById<TextView>(R.id.textView18)
        val emailTextView = view.findViewById<TextView>(R.id.textView19)
        val roleTextView = view.findViewById<TextView>(R.id.textView20)
        val locationTextView = view.findViewById<TextView>(R.id.textView21)

        // Get the current user
        val currentUser = auth.currentUser

        currentUser?.let {
            val userEmail = it.email
            emailTextView.text = userEmail

            // Check if the user is in the SELLERS collection
            db.collection("SELLERS").document(userEmail!!).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // User is a seller
                        displayUserData(document, "Seller", userEmail, profileImageView, nameTextView, emailTextView, roleTextView, locationTextView)
                    } else {
                        // If not found in SELLERS, check in BUYERS collection
                        db.collection("BUYERS").document(userEmail).get()
                            .addOnSuccessListener { buyerDocument ->
                                if (buyerDocument.exists()) {
                                    // User is a buyer
                                    displayUserData(buyerDocument, "Buyer", userEmail, profileImageView, nameTextView, emailTextView, roleTextView, locationTextView)
                                } else {
                                    // Handle case where user is not found in either collection
                                    nameTextView.text = "User data not found"
                                    roleTextView.text = "Unknown Role"
                                }
                            }
                            .addOnFailureListener { exception ->
                                // Handle errors for BUYERS collection
                                nameTextView.text = "Error: ${exception.message}"
                                roleTextView.text = "Error fetching role"
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle errors for SELLERS collection
                    nameTextView.text = "Error: ${exception.message}"
                    roleTextView.text = "Error fetching role"
                }
        }

        return view
    }

    // Helper function to display user data
    private fun displayUserData(
        document: DocumentSnapshot,
        role: String,
        userEmail: String?,
        profileImageView: de.hdodenhof.circleimageview.CircleImageView,
        nameTextView: TextView,
        emailTextView: TextView,
        roleTextView: TextView,
        locationTextView: TextView
    ) {
        val name = document.getString("Name") ?: "No Name"
        val location = document.getString("Location") ?: "by pass road near icici bank Robertaganj Sonebhadra Uttar Pradesh 231216"
        val profileImageUrl = document.getString("profileImageUrl")

        nameTextView.text = name
        emailTextView.text = userEmail
        roleTextView.text = role
        locationTextView.text = location

        // Load the profile image using Glide
        if (!profileImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(profileImageUrl).into(profileImageView)
        }
    }
}