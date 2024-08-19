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
import com.google.common.reflect.TypeToken
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class PhoneAuth : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var sharedPreferences: SharedPreferences
    private val SESSION_KEY_PHONE_AUTH = "PHONE_AUTH_SESSION"
    private val MAX_OTP_ATTEMPTS = 3
    private val PHONE_ATTEMPTS_KEY = "PHONE_ATTEMPTS"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_auth)

        progressBar = findViewById(R.id.progressBar2)
        auth = FirebaseAuth.getInstance()

        if (intent.getBooleanExtra("FROM_OTP_VERIFICATION", false)) {
            Toast.makeText(this, "Try Again", Toast.LENGTH_SHORT).show()
        }
        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)
        val sendOTPbtn = findViewById<Button>(R.id.button3)

        sendOTPbtn.setOnClickListener {
            val phoneNumber = findViewById<EditText>(R.id.editTextPhone).text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                val attemptCount = getAttemptCount(phoneNumber)
                if (attemptCount < MAX_OTP_ATTEMPTS) {
                    startPhoneNumberVerification("+91$phoneNumber")
                } else {
                    showToast("You have exceeded the maximum number of attempts for this number. Try again later.")
                }
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
                val phoneNumber = findViewById<EditText>(R.id.editTextPhone).text.toString().trim()
                val attemptCount = getAttemptCount(phoneNumber)
               // val attemptCount = sharedPreferences.getInt("OTP_ATTEMPTS", 0)
                sharedPreferences.edit().putString(SESSION_KEY_PHONE_AUTH, verificationId).apply()
                val intent = Intent(this@PhoneAuth, OTPVerification::class.java)
                intent.putExtra("storedVerificationId", verificationId)
                intent.putExtra("OTP_ATTEMPTS_LEFT", MAX_OTP_ATTEMPTS - attemptCount)
                intent.putExtra("PHONE_NUMBER", phoneNumber)
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
       // val attemptCount = sharedPreferences.getInt("OTP_ATTEMPTS", 0)

        incrementAttemptCount(phoneNumber)

        progressBar.visibility = View.VISIBLE
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
      //  sharedPreferences.edit().putInt("OTP_ATTEMPTS", attemptCount + 1).apply()
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
    } private inline fun <reified T> Gson.fromJson(json: String): T =
        fromJson(json, object : TypeToken<T>() {}.type)

    private fun getAttemptCount(phoneNumber: String): Int {
        val attemptsJson = sharedPreferences.getString(PHONE_ATTEMPTS_KEY, "{}")
        val attemptsMap: Map<String, Int> = attemptsJson?.let { Gson().fromJson(it) } ?: emptyMap()
        return attemptsMap[phoneNumber] ?: 0
    }

    private fun incrementAttemptCount(phoneNumber: String) {
        val attemptsJson = sharedPreferences.getString(PHONE_ATTEMPTS_KEY, "{}")
        val attemptsMap: MutableMap<String, Int> = attemptsJson?.let { Gson().fromJson(it) } ?: mutableMapOf()
        attemptsMap[phoneNumber] = (attemptsMap[phoneNumber] ?: 0) + 1
        sharedPreferences.edit().putString(PHONE_ATTEMPTS_KEY, Gson().toJson(attemptsMap)).apply()
    }
}
