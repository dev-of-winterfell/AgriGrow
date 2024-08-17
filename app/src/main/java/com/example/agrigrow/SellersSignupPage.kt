package com.example.agrigrow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.agrigrow.databinding.ActivitySignupPageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SellersSignupPage : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignupPageBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.passa.setupPasswordVisibilityToggle()
        binding.cnfpasss.setupPasswordVisibilityToggle()
        auth = Firebase.auth
        db = Firebase.firestore
        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googlesignup.setOnClickListener {
            signInWithGoogle()
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("Signup", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                // Get the FCM token
                val token = task.result
                Log.d("Signup", "FCM Token: $token")

                // Assuming you have already obtained the current user ID after signup
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener

                // Save the token to Firestore (Step 3)
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("SELLERS").document(userId)

                userRef.set(mapOf("fcmToken" to token), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("Signup", "FCM token saved successfully!")
                    }
                    .addOnFailureListener { e ->
                        Log.w("Signup", "Error saving FCM token", e)
                    }
            }
        }

        binding.signupbtn.setOnClickListener {
            if (check()) {
                val email = binding.emaillll.text.toString().trim()
                val password = binding.passa.text.toString().trim()
                val name = binding.name.text.toString().trim()
                val phoneNumber = binding.phonenumber.text.toString().trim()
                val uuid = FirebaseAuth.getInstance().currentUser?.uid ?: UUID.randomUUID().toString()
                val user = hashMapOf(
                    "uuid" to uuid,
                    "Name" to name,
                    "Phone" to phoneNumber,
                    "Email" to email
                )

                lifecycleScope.launch {
                    try {
                        val documents = db.collection("SELLERS").whereEqualTo("Email", email).get().await()
                        if (documents.isEmpty) {
                            val isUserCreated = createUserWithEmailAndPassword(email, password, user)
                            handleUserCreationResult(isUserCreated, email, name)

                        } else {
                            showAlertDialog("Alert", "User already exists")
                        }
                    } catch (exception: Exception) {
                        Log.e("error", "Server error: ${exception.message}")
                    }
                }
            }
        }

        binding.backtologin.setOnClickListener {
            startActivity(Intent(this, LoginPage::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        val name = binding.name.text.toString().trim()
        val pnumber = binding.phonenumber.text.toString().trim()
        val email = binding.emaillll.text.toString().trim()
        val password = binding.passa.text.toString().trim()
        val confirmPassword = binding.cnfpasss.text.toString().trim()

        return when {
            name.isEmpty() || pnumber.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
                false
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Password mismatch", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val idToken = account.idToken
        if (idToken != null) {
            val credentials = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credentials)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            val userData = hashMapOf(
                                "Name" to it.displayName,
                                "Email" to it.email,
                                "profileImageUrl" to (it.photoUrl?.toString() ?: ""),
                                "Phone" to (it.phoneNumber ?: "")
                            )
                            createUserDocumentInFirestore(it.email ?: "", userData)
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Google sign-in failed: ID token is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUserDocumentInFirestore(email: String, userData: HashMap<String, String?>) {
        lifecycleScope.launch {
            try {
                val userDocument = db.collection("SELLERS").document(email)
                val documentSnapshot = userDocument.get().await()

                if (!documentSnapshot.exists()) {
                    userDocument.set(userData).await()
                } else {
                    userDocument.update(userData as Map<String, Any>).await()
                }

                Toast.makeText(this@SellersSignupPage, "User registered successfully", Toast.LENGTH_SHORT).show()
                saveUserLoginState(email)
                startActivity(Intent(this@SellersSignupPage, landingPage::class.java).apply {
                    putExtra("USER_NAME", userData["Name"])
                })
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@SellersSignupPage, "Failed to register user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun createUserWithEmailAndPassword(email: String, password: String, user: Map<String, String>): Boolean {
        return try {
            val task = auth.createUserWithEmailAndPassword(email, password).await()
            if (task.user != null) {
                val userWithPhoto = user.toMutableMap()
                userWithPhoto["profileImageUrl"] = ""
                db.collection("SELLERS").document(email).set(userWithPhoto).await()
                true
            } else {
                false
            }
        } catch (exception: Exception) {
            showAlertDialog("Alert", exception.message ?: "Unknown Error")
            false
        }
    }

    private fun handleUserCreationResult(isSuccessful: Boolean, email: String, name: String) {
        if (isSuccessful) {
            saveUserLoginState(email)

            startActivity(Intent(this@SellersSignupPage, landingPage::class.java).apply {
                putExtra("USER_NAME", name)
            })


            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("Signup", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                // Get the FCM token
                val token = task.result
                Log.d("Signup", "FCM Token: $token")

                // Assuming you have already obtained the current user ID after signup
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener

                // Save the token to Firestore
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("SELLERS").document(userId)

                userRef.set(mapOf("fcmToken" to token), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("Signup", "FCM token saved successfully!")
//                        // Send the token to your server after saving it in Firestore
//                        sendTokenToServer(token)
                    }
                    .addOnFailureListener { e ->
                        Log.w("Signup", "Error saving FCM token", e)
                    }
            }


            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } else {
            showAlertDialog("Alert", "User registration failed. Please try again.")
        }
    }

    private fun saveUserLoginState(email: String?) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("IS_LOGGED_IN", true)
        editor.putString("USER_EMAIL", email)
        editor.apply()
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
//    private fun sendTokenToServer(token: String?) {
//        val url = "http://127.0.0.1:8000/send-signup-notification/"
//        val requestQueue = Volley.newRequestQueue(this)
//
//        val jsonBody = JSONObject().apply {
//            put("fcm_token", token)
//        }
//
//        val stringRequest = object : StringRequest(
//            Method.POST, url,
//            Response.Listener { response ->
//                Log.d(ContentValues.TAG, "Server response: $response")
//            },
//            Response.ErrorListener { error ->
//                Log.e(ContentValues.TAG, "Server error: ${error.message}")
//            }) {
//
//            override fun getBodyContentType(): String {
//                return "application/json; charset=utf-8"
//            }
//
//            override fun getBody(): ByteArray {
//                return jsonBody.toString().toByteArray(Charsets.UTF_8)
//            }
//        }
//
//        requestQueue.add(stringRequest)
//    }


}
