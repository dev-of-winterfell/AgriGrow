package com.example.agrigrow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.agrigrow.databinding.ActivityOtpverificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OTPVerification : AppCompatActivity() {
    private lateinit var binding: ActivityOtpverificationBinding
    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

binding=ActivityOtpverificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storedVerificationId = intent.getStringExtra("storedVerificationId")
        if (storedVerificationId == null) {
            Toast.makeText(this, "Verification ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        binding.progressBar3.visibility = View.INVISIBLE

        binding.buttonVerify.setOnClickListener {
            val otp = getEnteredOTP()
            if (otp.length == 6) {
                binding.progressBar3.visibility = View.VISIBLE
                verifyPhoneNumberWithCode(storedVerificationId, otp)
            } else {
                Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        setupOtpInput()
    }

    private fun getEnteredOTP(): String {
        return binding.run {
            editTextOTP1.text.toString().trim() +
                    editTextOTP2.text.toString().trim() +
                    editTextOTP3.text.toString().trim() +
                    editTextOTP4.text.toString().trim() +
                    editTextOTP5.text.toString().trim() +
                    editTextOTP6.text.toString().trim()
        }
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            lifecycleScope.launch(Dispatchers.Main) {
                binding.progressBar3.visibility = View.VISIBLE
                signInWithPhoneAuthCredential(credential)
            }
        } else {
            binding.progressBar3.visibility = View.INVISIBLE
            Toast.makeText(this, "Verification ID is null", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        try {
            val authResult = withContext(Dispatchers.IO) {
                auth.signInWithCredential(credential).await()
            }

            binding.progressBar3.visibility = View.INVISIBLE
            // Inside the signInWithPhoneAuthCredential function after successful authentication
            if (authResult.user != null) {
                // Store user authentication state (e.g., user ID) for session management
                val userId = authResult.user?.uid
                // Pass user data to com.example.gradx.com.example.gradx.LandingPage
                val intent = Intent(this@OTPVerification, BuyerOrSellerDecider2::class.java)
                intent.putExtra("userId", userId)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Log.w("failed", "signInWithCredential:failure")
                Toast.makeText(this@OTPVerification, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show()
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            binding.progressBar3.visibility = View.INVISIBLE
            Toast.makeText(this@OTPVerification, "Invalid OTP", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            binding.progressBar3.visibility = View.INVISIBLE
            Toast.makeText(this@OTPVerification, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupOtpInput() {
        val editTexts = listOf(
            binding.editTextOTP1, binding.editTextOTP2, binding.editTextOTP3,
            binding.editTextOTP4, binding.editTextOTP5, binding.editTextOTP6
        )

        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        if (i < editTexts.size - 1) {
                            editTexts[i + 1].requestFocus()
                        }
                    } else if (s?.length == 0) {
                        if (i > 0) {
                            editTexts[i - 1].requestFocus()
                        }
                    }
                }
            })
        }
    }
}
