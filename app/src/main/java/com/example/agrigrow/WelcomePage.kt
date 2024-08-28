package com.example.agrigrow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.agrigrow.databinding.ActivityWelcomePageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.log

class WelcomePage : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomePageBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
private lateinit var db:FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
db= FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        binding=ActivityWelcomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)
        if (isUserLoggedIn()) {
            val userEmail = sharedPreferences.getString("USER_EMAIL", null)
            if (userEmail != null) {
                lifecycleScope.launch {
                    redirectToAppropriateLandingPage(userEmail)
                }
            } else {
                // If email is null, clear preferences and stay on WelcomePage
                clearUserLoginState()
            }
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googlebutton2.setOnClickListener {

            signInWithGoogle()
        }

binding.button6.setOnClickListener {
    startActivity(Intent(this@WelcomePage,LoginPage::class.java))
}



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val phoneAuth=findViewById<Button>(R.id.button3)
        phoneAuth.setOnClickListener {
           // startActivity(Intent(this@WelcomePage,PhoneAuth::class.java))
            val existingDialog = supportFragmentManager.findFragmentByTag("PhoneVerificationDialog") as? ItemsSharedDialogFragment
            if (existingDialog == null) {
                // The dialog is not currently shown, so show it
                val dialog = ItemsSharedDialogFragment()
                dialog.show(supportFragmentManager, "PhoneVerificationDialog")
            } else {
                // The dialog is already shown, maybe bring it to the front or interact with it
            }


        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            }
        }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val account = task.await()
                firebaseAuthWithGoogle(account)
            } catch (e: Exception) {
                Toast.makeText(this@WelcomePage, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {

            val credentials = GoogleAuthProvider.getCredential(account.idToken, null)
            try {
                auth.signInWithCredential(credentials).await()

                // Cache user login state
                saveUserLoginState(account.email)

                // Determine the role and cache it
                account.email?.let { email ->
                    val buyerDoc = db.collection("BUYERS").document(email).get().await()
                    val sellerDoc = db.collection("SELLERS").document(email).get().await()

                    val userRole = when {
                        buyerDoc.exists() -> "BUYER"
                        sellerDoc.exists() -> "SELLER"
                        else -> null
                    }

                    if (userRole != null) {
                        // Cache the role for future use
                        sharedPreferences.edit().putString("USER_ROLE", userRole).apply()

                        // Redirect based on role
                        startActivity(Intent(this, if (userRole == "BUYER") BuyerLandingPage::class.java else SellerLandingPage::class.java).apply {
                            putExtra("USER_EMAIL", email)
                        })
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    } else {
                        Toast.makeText(this, "User role6  not recognized.", Toast.LENGTH_SHORT).show()
                        clearUserLoginState()
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }



    private fun saveUserLoginState(email: String?) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("IS_LOGGED_IN", true)
        editor.putString("USER_EMAIL", email)
        editor.apply()
    }
    private suspend fun redirectToAppropriateLandingPage(email: String) {
        // 1. Check for cached role
        val cachedRole = sharedPreferences.getString("USER_ROLE", null)
        if (cachedRole != null) {
            // Redirect immediately based on cached role
            startActivity(Intent(this, if(cachedRole == "BUYER") BuyerLandingPage::class.java else SellerLandingPage::class.java).apply {
                putExtra("USER_EMAIL", email)
            })
            finish()
            return // Stop further execution
        }

        // 2. Role not cached, query Firestore
        try {
            val buyerDoc = db.collection("BUYERS").document(email).get().await()
            val sellerDoc = db.collection("SELLERS").document(email).get().await()

            when {
                buyerDoc.exists() -> {
                    // Cachethe role for future use
                    sharedPreferences.edit().putString("USER_ROLE", "BUYER").apply()
                    startActivity(Intent(this, BuyerLandingPage::class.java).apply {
                        putExtra("USER_EMAIL", email)
                    })
                }
                sellerDoc.exists() -> {
                    // Cache the role for future use
                    sharedPreferences.edit().putString("USER_ROLE", "SELLER").apply()
                    startActivity(Intent(this, SellerLandingPage::class.java).apply {
                        putExtra("USER_EMAIL", email)
                    })
                }
                else -> {
                    Toast.makeText(this, "User role not recognized.", Toast.LENGTH_SHORT).show()
                    clearUserLoginState()
                    return // Stop further execution
                }
            }
            finish() // Close WelcomePage after redirection
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            clearUserLoginState()
        }
    }

    private fun clearUserLoginState() {
        sharedPreferences.edit()
            .clear()
            .putBoolean("IS_LOGGED_IN", false)
            .apply()
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("IS_LOGGED_IN", false)
    }


}