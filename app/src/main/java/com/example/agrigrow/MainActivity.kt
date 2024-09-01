package com.example.agrigrow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check user login state and redirect after a short delay
        android.os.Handler().postDelayed({
            checkUserAndRedirect()
        }, 2500) // 2500 milliseconds = 2.5 seconds
    }

    private fun checkUserAndRedirect() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in
            lifecycleScope.launch {
                redirectToAppropriateLandingPage(currentUser.email ?: "")
            }
        } else {
            // No user is signed in, redirect to WelcomePage
            startActivity(Intent(this, WelcomePage::class.java))
            finish()
        }
    }

    private suspend fun redirectToAppropriateLandingPage(email: String) {
        try {
            val buyerDoc = db.collection("BUYERS").document(email).get().await()
            val sellerDoc = db.collection("SELLERS").document(email).get().await()

            val userRole = when {
                buyerDoc.exists() -> "BUYER"
                sellerDoc.exists() -> "SELLER"
                else -> null
            }

            val sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)

            if (userRole != null) {
                // Update cached role
                sharedPreferences.edit().putString("USER_ROLE", userRole).apply()

                // Redirect based on current role
                val intent = when (userRole) {
                    "BUYER" -> Intent(this, BuyerLandingPage::class.java)
                    "SELLER" -> Intent(this, SellerLandingPage::class.java)
                    else -> throw IllegalStateException("Invalid user role")
                }
                intent.putExtra("USER_EMAIL", email)
                startActivity(intent)
                finish()
            } else {
                // User role not recognized, redirect to WelcomePage
                startActivity(Intent(this, WelcomePage::class.java))
                finish()
            }
        } catch (e: Exception) {
            // Error occurred, redirect to WelcomePage
            startActivity(Intent(this, WelcomePage::class.java))
            finish()
        }
    }
}