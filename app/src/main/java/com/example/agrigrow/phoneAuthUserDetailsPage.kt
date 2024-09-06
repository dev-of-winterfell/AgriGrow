package com.example.agrigrow

import android.annotation.SuppressLint
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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.agrigrow.databinding.ActivityPhoneAuthSellerDetailsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class phoneAuthUserDetailsPage : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneAuthSellerDetailsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar  // Add this line
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPhoneAuthSellerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressBar = findViewById<ProgressBar>(R.id.progressBar80)  // Initialize ProgressBar

        val items = arrayOf("विकल्प के रूप में किसान या खरीदार का चयन करें", "खरीददार", "किसान")
        val adapter: ArrayAdapter<String> =
            object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
                override fun isEnabled(position: Int): Boolean {
                    return position != 0 // Disable the hint item (first item)
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
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

        binding.passa.setupPasswordVisibilityToggle()
        binding.cnfpasss.setupPasswordVisibilityToggle()
        auth = Firebase.auth
        db = Firebase.firestore
        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)
        handleIncomingData()
        binding.signupbtn.setOnClickListener {
            showProgressBar()
            signUpUser()
            hideProgressBar()
        }
    }

    private fun signUpUser() {
        if (binding.spinner.selectedItemPosition == 0) {
            Toast.makeText(this, "कृपया एक उपयोगकर्ता प्रकार चुनें", Toast.LENGTH_SHORT).show()
        } else if (check()) {
            val email = binding.emaillll.text.toString().trim()
            val password = binding.passa.text.toString().trim()

            showProgressBar() // Show the progress bar at the start

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    hideProgressBar() // Hide the progress bar after completion
                    if (task.isSuccessful) {
                        val account = GoogleSignIn.getLastSignedInAccount(this)
                        val profilePicUrl = account?.photoUrl?.toString() // Extract profile picture URL if available
                        val name = binding.name.text.toString().trim()
                        val uuid = UUID.randomUUID().toString()

                        // Create the user HashMap with Any? values
                        val user = hashMapOf(
                            "uuid" to uuid,
                            "Name" to name,
                            "Email" to email,
                            "profileImageUrl" to profilePicUrl // This will be null if no URL is available
                        ) as HashMap<String, Any?>

                        // Collect the selected role from the spinner
                        val selectedRole = binding.spinner.selectedItem.toString()

                        // Determine the Firestore collection based on the selected role
                        val collectionName = if (selectedRole == "खरीददार") "BUYERS" else "SELLERS"

                        lifecycleScope.launch {
                            showProgressBar() // Show progress bar during Firestore operation
                            try {
                                // Check if the user already exists in the selected collection
                                val documents = db.collection(collectionName).whereEqualTo("Email", email).get().await()
                                if (documents.isEmpty) {
                                    // Create the user in Firestore
                                    createUserDocumentInFirestore(email, user)
                                } else {
                                    showAlertDialog("सतर्कता", "उपयोगकर्ता पहले से मौजूद है")
                                }
                            } catch (exception: Exception) {
                                Log.e("error", "सर्वर त्रुटि: ${exception.message}")
                            } finally {
                                hideProgressBar() // Hide progress bar after operation
                            }
                        }
                    } else {
                        Toast.makeText(this, "साइन-अप विफल: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }



    private fun handleIncomingData() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            binding.name.setText(account.displayName)
            binding.emaillll.setText(account.email)
            // Reset password field
            // binding.passa.setText("")
            // Reset confirm password field
            // binding.cnfpasss.setText("")

            // Automatically trigger user creation if data is pre-filled from Google
            if (check()) {
                val profilePicUrl = account.photoUrl?.toString() // Get profile image URL from Google
                val email = binding.emaillll.text.toString().trim()
                val name = binding.name.text.toString().trim()
                val uuid = UUID.randomUUID().toString()

                // Construct the user object with Any? values
                val user = hashMapOf(
                    "uuid" to uuid,
                    "Name" to name,
                    "Email" to email,
                    "profileImageUrl" to profilePicUrl // Include profile image URL
                )

                // Collect the selected role from the spinner
                val selectedRole = binding.spinner.selectedItem.toString()

                // Determine the Firestore collection based on the selected role
                val collectionName = if (selectedRole == "खरीददार") "BUYERS" else "SELLERS"

                lifecycleScope.launch {
                    showProgressBar()
                    try {
                        // Check if the user already exists in the selected collection
                        val documents = db.collection(collectionName).whereEqualTo("Email", email).get().await()
                        if (documents.isEmpty) {
                            // Create the user in Firestore
                            createUserDocumentInFirestore(email, user as HashMap<String, Any?>)
                        } else {
                            showAlertDialog("सतर्कता", "उपयोगकर्ता पहले से मौजूद है")
                        }
                    } catch (exception: Exception) {
                        Log.e("error", "सर्वर त्रुटि: ${exception.message}")
                    } finally {
                        hideProgressBar() // Hide the progress bar after the operation is complete
                    }
                }
            }
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
        val email = binding.emaillll.text.toString().trim()
        val password = binding.passa.text.toString().trim()
        val confirmPassword = binding.cnfpasss.text.toString().trim()

        var isValid = true

        // Reset error messages
        binding.name.error = null
        binding.emaillll.error = null
        binding.passa.error = null
        binding.cnfpasss.error = null

        if (name.isEmpty()) {
            binding.name.error = "नाम आवश्यक है"
            isValid = false
        }
        if (email.isEmpty()) {
            binding.emaillll.error = "ईमेल आवश्यक है"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.passa.error = "पासवर्ड आवश्यक है"
            isValid = false
        }
        if (confirmPassword.isEmpty()) {
            binding.cnfpasss.error = "पासवर्ड की पुष्टि आवश्यक है"
            isValid = false
        } else if (password != confirmPassword) {
            binding.cnfpasss.error = "पासवर्ड मेल नहीं खाते"
            isValid = false
        }

        return isValid
    }

    private fun createUserDocumentInFirestore(email: String, userData: HashMap<String, Any?>) {
        val selectedRole = binding.spinner.selectedItem.toString()
        val collectionName = if (selectedRole == "खरीददार") "BUYERS" else "SELLERS"

        lifecycleScope.launch {
            try {
                val userDocument = db.collection(collectionName).document(email)
                val documentSnapshot = userDocument.get().await()

                if (!documentSnapshot.exists()) {
                    userDocument.set(userData).await()
                } else {
                    userDocument.update(userData as Map<String, Any?>).await()
                }

                Toast.makeText(this@phoneAuthUserDetailsPage, "उपयोगकर्ता सफलतापूर्वक पंजीकृत", Toast.LENGTH_SHORT).show()
                saveUserLoginState(email, selectedRole)  // Fixed: Passing selectedRole as the second argument

                // Redirect to the specific landing page based on user role
                val intent = Intent(this@phoneAuthUserDetailsPage,
                    if (selectedRole == "खरीददार") BuyerLandingPage::class.java else SellerLandingPage::class.java)
                startActivity(intent)
                finish() // Finish current activity to prevent going back to it
            } catch (e: Exception) {
                Toast.makeText(this@phoneAuthUserDetailsPage, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }
    private fun saveUserLoginState(email: String?, selectedRole: String) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("IS_LOGGED_IN", true)
        editor.putString("USER_EMAIL", email)
        editor.putString("USER_ROLE", selectedRole)
        editor.apply()
    }
    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("ठीक है") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}





























