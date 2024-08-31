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
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.agrigrow.databinding.ActivityPhoneAuthSellerDetailsBinding
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


class phoneAuthUserDetailsPage : AppCompatActivity() {
    private lateinit var binding:ActivityPhoneAuthSellerDetailsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar  // Add this line
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityPhoneAuthSellerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressBar = findViewById<ProgressBar>(R.id.progressBar80)  // Initialize ProgressBar
        val items = arrayOf("विकल्प के रूप में किसान या खरीदार का चयन करें", "खरीददार", "किसान")
        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0 // Disable the hint item (first item)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                if (position == 0) {
                    // Set the hint text color for the first item
                    textView.setTextColor(Color.GRAY)
                } else {
                    // Set the regular text color for other items
                    textView.setTextColor(Color.BLACK)
                }
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1054481860590-h8nr9qbh2uc1qlvg6a17eiajt4uurbgc.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleSignInButton.setOnClickListener {

            if (binding.spinner.selectedItemPosition == 0) {
                Toast.makeText(this, "कृपया उपयोगकर्ता प्रकार चुनें", Toast.LENGTH_SHORT).show()
            }
            else{  showProgressBar()
                signInWithGoogle()
                hideProgressBar()
            }
        }
//        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                when (position) {
//                    0 -> {
//                        // Redirect to BuyerSignupPage
//                        startActivity(Intent(this@phoneAuthUserDetailsPage, BuyerLandingPage::class.java))
//                    }
//                    1 -> {
//                        // Redirect to FarmerSignupPage
//                        startActivity(Intent(this@phoneAuthUserDetailsPage, SellerLandingPage::class.java))
//                    }
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                // Do nothing
//            }
//        }
        binding.passa.setupPasswordVisibilityToggle()
        binding.cnfpasss.setupPasswordVisibilityToggle()
        auth = Firebase.auth
        db = Firebase.firestore
        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)
//        try{
//            if (isUserLoggedIn()) {
//                val userEmail = sharedPreferences.getString("USER_EMAIL", null)
//                if (userEmail != null) {
//                    CoroutineScope(Dispatchers.Main).launch {
//                        redirectToAppropriateLandingPage(userEmail)
//                    }
//                }
//                finish()
//            }}
//        catch (e:Exception){
//            Toast.makeText(this,"Error while redirecting",Toast.LENGTH_LONG).show()
//        }
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestEmail()
//          .build()


        //  googleSignInClient = GoogleSignIn.getClient(this, gso)

//        binding.googlesignup.setOnClickListener {
//            signInWithGoogle()
//            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//                if (!task.isSuccessful) {
//                    Log.w("Signup", "Fetching FCM registration token failed", task.exception)
//                    return@addOnCompleteListener
//                }
//                // Get the FCM token
//                val token = task.result
//                Log.d("Signup", "FCM Token: $token")
//
//                // Assuming you have already obtained the current user ID after signup
//                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
//
//                // Save the token to Firestore (Step 3)
//                val db = FirebaseFirestore.getInstance()
//                val userRef = db.collection("SELLERS").document(userId)
//
//                userRef.set(mapOf("fcmToken" to token), SetOptions.merge())
//                    .addOnSuccessListener {
//                        Log.d("Signup", "FCM token saved successfully!")
//                    }
//                    .addOnFailureListener { e ->
//                        Log.w("Signup", "Error saving FCM token", e)
//                    }
//            }
//        }

        binding.signupbtn.setOnClickListener {
            if (binding.spinner.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select a user type", Toast.LENGTH_SHORT).show()
            } else if (check()) {
                val email = binding.emaillll.text.toString().trim()
                val password = binding.passa.text.toString().trim()
                val name = binding.name.text.toString().trim()
                val uuid = FirebaseAuth.getInstance().currentUser?.uid ?: UUID.randomUUID().toString()
                val user = hashMapOf(
                    "uuid" to uuid,
                    "Name" to name,
                    "Email" to email
                )

                lifecycleScope.launch {
                    showProgressBar()
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
                    finally {
                        hideProgressBar()  // Hide progress bar once the operation is complete
                    }
                }
            }
        }

//        binding.backtologin.setOnClickListener {
//            startActivity(Intent(this, LoginPage::class.java))
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
//        }
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
        //  val pnumber = binding.phonenumber.text.toString().trim()
        val email = binding.emaillll.text.toString().trim()
        val password = binding.passa.text.toString().trim()
        val confirmPassword = binding.cnfpasss.text.toString().trim()

        return when {
            name.isEmpty() ||  email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
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
                            if (binding.spinner.selectedItemPosition == AdapterView.INVALID_POSITION) {
                                Toast.makeText(this, "Please select whether you're a buyer or seller", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }
                            val userData = hashMapOf(
                                "Name" to it.displayName,
                                "Email" to it.email,
                                "profileImageUrl" to (it.photoUrl?.toString() ?: ""),
                                //"Phone" to (it.phoneNumber ?: "")
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
        val selectedRole = binding.spinner.selectedItem.toString()
        val collectionName = if (selectedRole == "खरीददार") "BUYERS" else "SELLERS"

        lifecycleScope.launch {
            try {
                val userDocument = db.collection(collectionName).document(email)
                val documentSnapshot = userDocument.get().await()

                if (!documentSnapshot.exists()) {
                    userDocument.set(userData).await()
                } else {
                    userDocument.update(userData as Map<String, Any>).await()
                }

                Toast.makeText(this@phoneAuthUserDetailsPage, "User registered successfully", Toast.LENGTH_SHORT).show()
                saveUserLoginState(email)

                val intent = when (selectedRole) {
                    "खरीददार" -> Intent(this@phoneAuthUserDetailsPage, BuyerLandingPage::class.java)
                    "किसान" -> Intent(this@phoneAuthUserDetailsPage, SellerLandingPage::class.java)
                    else -> Intent(this@phoneAuthUserDetailsPage, WelcomePage::class.java)
                }
                intent.putExtra("USER_NAME", userData["Name"])
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@phoneAuthUserDetailsPage, "Failed to register user: ${e.message}", Toast.LENGTH_SHORT).show()
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
            val selectedRole = binding.spinner.selectedItem.toString()
            val intent = when (selectedRole) {
                "खरीददार" -> Intent(this@phoneAuthUserDetailsPage, BuyerLandingPage::class.java)
                "किसान" -> Intent(this@phoneAuthUserDetailsPage, SellerLandingPage::class.java)
                else -> Intent(this@phoneAuthUserDetailsPage, WelcomePage::class.java)
            }
            intent.putExtra("USER_NAME", name)
            startActivity(intent)
            // ... (FCM token logic)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } else {
            showAlertDialog("Alert", "User registration failed. Please try again.")
        }
    }
    private fun saveUserLoginState(email: String?) {
        val selectedRole = binding.spinner.selectedItem.toString()
        val editor = sharedPreferences.edit()
        editor.putBoolean("IS_LOGGED_IN", true)
        editor.putString("USER_EMAIL", email)
        editor.putString("USER_ROLE", selectedRole)  // Store the user role
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
    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
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
//private suspend fun redirectToAppropriateLandingPage(email: String) {
//    val buyerDoc = db.collection("BUYERS").document(email).get().await()
//    val sellerDoc = db.collection("SELLERS").document(email).get().await()
//
//    if (buyerDoc.exists()) {
//
//        startActivity(Intent(this, BuyerLandingPage::class.java).apply {
//            putExtra("USER_EMAIL", email)
//        })
//    } else if (sellerDoc.exists()) {
//        startActivity(Intent(this, SellerLandingPage::class.java).apply {
//            putExtra("USER_EMAIL", email)
//        })
//    } else {
//        Toast.makeText(this, "User role not recognized.", Toast.LENGTH_SHORT).show()
//    }
//
//    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
//    finish()
//}
//private fun isUserLoggedIn(): Boolean {
//    return sharedPreferences.getBoolean("IS_LOGGED_IN", false)
//}

}

