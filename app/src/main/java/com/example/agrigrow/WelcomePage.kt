package com.example.agrigrow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
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

class WelcomePage : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomePageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var db: FirebaseFirestore
private lateinit var progressBar8:ProgressBar
private lateinit var progressBar9:ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        binding = ActivityWelcomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
progressBar8=binding.progressBar8
        progressBar9=binding.progressBar9
        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googlebutton2.setOnClickListener {
            signInWithGoogle()
        }

        binding.button6.setOnClickListener {
            startActivity(Intent(this@WelcomePage, LoginPage::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val phoneAuth = findViewById<Button>(R.id.button3)
        phoneAuth.setOnClickListener {
            startActivity(Intent(this@WelcomePage,phoneAuthUserDetailsPage::class.java))
//            showProgressBar()
//            val existingDialog = supportFragmentManager.findFragmentByTag("PhoneVerificationDialog") as? ItemsSharedDialogFragment
//            if (existingDialog == null) {
//                val dialog = ItemsSharedDialogFragment()
//                dialog.show(supportFragmentManager, "PhoneVerificationDialog")
//                hideProgressBar()
//            }
        }
    }

    private fun signInWithGoogle() {
        showProgressBar9()
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
                fetchGoogleAccountDetails(account)
            } catch (e: Exception) {
                Toast.makeText(this@WelcomePage, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchGoogleAccountDetails(account: GoogleSignInAccount) {
        showProgressBar9()

        // Retrieve Google account details
        val userName = account.displayName ?: "No Name"
        val userEmail = account.email ?: "No Email"
        val userProfilePic = account.photoUrl?.toString() ?: "No Profile Picture"

        // Save user login state in shared preferences
        saveUserLoginState(userEmail)

        // Redirect to setup page, pass the fetched user details
        val intent = Intent(this, phoneAuthUserDetailsPage::class.java).apply {
            putExtra("USER_NAME", userName)
            putExtra("USER_EMAIL", userEmail)
            putExtra("USER_PROFILE_PIC", userProfilePic)
        }
        startActivity(intent)
        hideProgressBar9()
    }


    private suspend fun determineUserRoleAndRedirect(email: String) {
        val buyerDoc = db.collection("BUYERS").document(email).get().await()
        val sellerDoc = db.collection("SELLERS").document(email).get().await()

        val userRole = when {
            buyerDoc.exists() -> "BUYER"
            sellerDoc.exists() -> "SELLER"
            else -> null
        }

        if (userRole != null) {
            sharedPreferences.edit().putString("USER_ROLE", userRole).apply()
            val intent = Intent(this, if (userRole == "BUYER") BuyerLandingPage::class.java else SellerLandingPage::class.java)
            intent.putExtra("USER_EMAIL", email)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } else {
            Toast.makeText(this, "User role not recognized.", Toast.LENGTH_SHORT).show()
            clearUserLoginState()
        }
    }

    private fun saveUserLoginState(email: String?) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("IS_LOGGED_IN", true)
        editor.putString("USER_EMAIL", email)
        editor.apply()
    }

    private fun clearUserLoginState() {
        sharedPreferences.edit()
            .clear()
            .putBoolean("IS_LOGGED_IN", false)
            .apply()
    }
    private fun showProgressBar() {
       progressBar8 .visibility = View.VISIBLE
    }
    private fun showProgressBar9() {
        progressBar9.visibility = View.GONE
    }

    private fun hideProgressBar9() {
        progressBar9.visibility = View.GONE
    }
    private fun hideProgressBar() {
        progressBar8.visibility = View.GONE
    }
}