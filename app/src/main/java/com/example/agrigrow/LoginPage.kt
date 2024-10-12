package com.example.agrigrow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.agrigrow.databinding.ActivityLoginPageBinding
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

class LoginPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var progressBar: ProgressBar
    private lateinit var binding: ActivityLoginPageBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db:FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Make system bars transparent
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT


        binding=ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pass.setupPasswordVisibilityToggle()
        binding.emailll.setupPasswordVisibilityToggle()
        progressBar = binding.progressBar
        auth = FirebaseAuth.getInstance()
        db= FirebaseFirestore.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)

        // If the user is already signed in, go to the landing page
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

        binding.googlesignup.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            signInWithGoogle()
        }

        binding.login.setOnClickListener {
            if (check()) {
                progressBar.visibility = View.VISIBLE
                val email = binding.emailll.text.toString().trim()
                val password = binding.pass.text.toString().trim()

                CoroutineScope(Dispatchers.Main).launch {
                    handleEmailPasswordSignIn(email, password)
                }
            } else {
                Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_LONG).show()
            }

        }

        binding.signup.setOnClickListener {
            startActivity(Intent(this, WelcomePage::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.button5.setOnClickListener {
            startActivity(Intent(this@LoginPage,UpdatePassword::class.java))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun EditText.setupPasswordVisibilityToggle() {
        val drawableEnd: Drawable? = compoundDrawablesRelative[2]

        drawableEnd?.let {
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.rawX >= (right - it.bounds.width())) {
                        val isVisible = transformationMethod == PasswordTransformationMethod.getInstance()
                        val newTransformationMethod = if (isVisible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
                        transformationMethod = newTransformationMethod
                        setSelection(text.length)
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
    }

    private fun check(): Boolean {
        val email = binding.emailll.text.toString().trim()
        val password = binding.pass.text.toString().trim()
        return email.isNotEmpty() && password.isNotEmpty()
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
                Toast.makeText(this@LoginPage, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun handleEmailPasswordSignIn(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            progressBar.visibility = View.GONE
            saveUserLoginState(email)
            redirectToAppropriateLandingPage(email)
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Wrong credentials", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credentials = GoogleAuthProvider.getCredential(account.idToken, null)
        try {
            auth.signInWithCredential(credentials).await()
            account.email?.let { email ->
                saveUserLoginState(email)
                redirectToAppropriateLandingPage(email)
            } ?: run {
                Toast.makeText(this, "Failed to get email from Google account", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun redirectToAppropriateLandingPage(email: String) {
        try {
            val buyerDoc = db.collection("BUYERS").document(email).get().await()
            val sellerDoc = db.collection("SELLERS").document(email).get().await()

            when {
                buyerDoc.exists() -> {
                    startActivity(Intent(this, BuyerLandingPage::class.java).apply {
                        putExtra("USER_EMAIL", email)
                    })
                }
                sellerDoc.exists() -> {
                    startActivity(Intent(this, SellerLandingPage::class.java).apply {
                        putExtra("USER_EMAIL", email)
                    })
                }
                else -> {
                    Toast.makeText(this, "User role not recognized.", Toast.LENGTH_SHORT).show()
                    clearUserLoginState()
                    return
                }
            }
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            clearUserLoginState()
        }
    }



    private fun saveUserLoginState(email: String?) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("IS_LOGGED_IN", true)
        editor.putString("USER_EMAIL", email)
        editor.apply()
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("IS_LOGGED_IN", false)
    }


    private fun clearUserLoginState() {
        sharedPreferences.edit()
            .clear()
            .putBoolean("IS_LOGGED_IN", false)
            .apply()
    }

    fun forgotPasswordClicked(view: View) {}

}
