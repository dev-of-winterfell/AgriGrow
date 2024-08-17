package com.example.agrigrow

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit

class PhoneAuth : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var sharedPreferences: SharedPreferences
    private val SESSION_KEY_PHONE_AUTH = "PHONE_AUTH_SESSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_auth)

        progressBar = findViewById(R.id.progressBar2)
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)
        val sendOTPbtn = findViewById<Button>(R.id.button3)

        sendOTPbtn.setOnClickListener {
            val phoneNumber = findViewById<EditText>(R.id.editTextPhone).text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                startPhoneNumberVerification("+91$phoneNumber")
            } else {
                showToast("Please enter a phone number")
            }
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                progressBar.visibility = View.INVISIBLE
                Log.d("com.example.gradx.PhoneAuth", "Verification completed: $credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressBar.visibility = View.INVISIBLE
                Log.e("com.example.gradx.PhoneAuth", "Verification failed", e)
                handleVerificationFailure(e)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                progressBar.visibility = View.INVISIBLE
                Log.d("com.example.gradx.PhoneAuth", "Code sent: $verificationId")
                val storedVerificationId = verificationId
                val   resendToken = token

                sharedPreferences.edit().putString(SESSION_KEY_PHONE_AUTH, verificationId).apply()
                val intent = Intent(this@PhoneAuth, OTPVerification::class.java)
                intent.putExtra("storedVerificationId", verificationId)
                startActivity(intent)
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("com.example.gradx.PhoneAuth", "signInWithCredential:success")
                    sharedPreferences.edit().putBoolean(SESSION_KEY_PHONE_AUTH, true).apply()
                    // TODO: Navigate to the next screen or update UI  val intent = Intent(this@PhoneAuth, OTPVerification::class.java)
                    val user = task.result?.user
                } else {
                    Log.w("com.example.gradx.PhoneAuth", "signInWithCredential:failure", task.exception)
                    handleSignInFailure(task.exception)
                }
            }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        progressBar.visibility = View.VISIBLE
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun handleVerificationFailure(exception: Exception) {
        when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                showToast("Invalid phone number.")
            }
            is FirebaseTooManyRequestsException -> {
                showToast("Too many requests. Please try again later.")
            }
            else -> {
                showToast("Verification failed: ${exception.message}")
            }
        }
    }

    private fun handleSignInFailure(exception: Exception?) {
        showToast("Sign-in failed: ${exception?.message}")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
