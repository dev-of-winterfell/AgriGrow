package com.example.agrigrow

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var tvTimer: TextView
    private lateinit var progressBarTimer: ProgressBar
 //   private lateinit var progressAnimation: Animation
    private var countDownTimer: CountDownTimer? = null
    private var valueAnimator: ValueAnimator? = null
    private lateinit var phoneNumber: String
    private var timeRemaining = 60 // Total time in seconds



    private var storedVerificationId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

binding=ActivityOtpverificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tvTimer = findViewById(R.id.tvTimer)
        progressBarTimer = findViewById(R.id.progressBarTimer)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        val attemptsLeft = intent.getIntExtra("OTP_ATTEMPTS_LEFT", 3)
        when (attemptsLeft) {
            2 -> showToast("Two OTP attempts left for $phoneNumber")
            1 -> showToast("One OTP attempt left for $phoneNumber")
            0 -> showToast("No OTP attempts left for $phoneNumber. Please try again later.")}
        progressBarTimer.max = timeRemaining
        progressBarTimer.progress = 0

        // Start the countdown timer
        startTimer()
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
                val intent = Intent(this@OTPVerification, phoneAuthUserDetailsPage::class.java)
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


    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeRemaining * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = (millisUntilFinished / 1000).toInt()

                // Format the time as MM:SS
                val minutes = timeRemaining / 60
                val seconds = timeRemaining % 60
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                tvTimer.text = timeFormatted
                updateProgressBar(60 - timeRemaining)
            }

            override fun onFinish() {
                tvTimer.text = "00:00"
                updateProgressBar(60)
                moveToPhoneAuth()
            }
        }.start()
    }

    private fun updateProgressBar(progress: Int) {
        valueAnimator?.cancel()

        val colorFrom: Int
        val colorTo: Int
        val colorMid: Int

        when {
            progress <= 20 -> { // First 20 seconds
                colorFrom = getColor(R.color.green22)
                colorMid = getColor(R.color.green)
                colorTo = getColor(R.color.green11)
            }
            progress <= 40 -> { // Middle 20 seconds
                colorFrom = getColor(R.color.yellow1)
                colorMid = getColor(R.color.yellow2)
                colorTo = getColor(R.color.yellow)
            }
            else -> { // Last 20 seconds
                colorFrom = getColor(R.color.red)
                colorMid = getColor(R.color.red)
                colorTo = getColor(R.color.red)
            }
        }

        // Determine fraction of progress within current segment
        val segmentDuration = 20
        val fraction = if (progress <= segmentDuration) {
            progress / segmentDuration.toFloat()
        } else if (progress <= 2 * segmentDuration) {
            (progress - segmentDuration) / segmentDuration.toFloat()
        } else {
            (progress - 2 * segmentDuration) / segmentDuration.toFloat()
        }

        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000 // Duration for each segment
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val animFraction = animation.animatedValue as Float
                val blendedColor = blendColors(colorFrom, colorMid, colorTo, animFraction, fraction)
                progressBarTimer.progressDrawable.setColorFilter(blendedColor, PorterDuff.Mode.SRC_IN)
                progressBarTimer.progress = progress
            }
            start()
        }
    }


    private fun blendColors(colorStart: Int, colorMid: Int, colorEnd: Int, fraction: Float, segmentFraction: Float): Int {
        val r: Int
        val g: Int
        val b: Int

        if (fraction <= 0.5f) {
            // Blend between colorStart and colorMid
            val interpolFraction = fraction * 2
            val startR = (colorStart shr 16) and 0xFF
            val startG = (colorStart shr 8) and 0xFF
            val startB = colorStart and 0xFF

            val midR = (colorMid shr 16) and 0xFF
            val midG = (colorMid shr 8) and 0xFF
            val midB = colorMid and 0xFF

            r = (startR + (midR - startR) * interpolFraction).toInt()
            g = (startG + (midG - startG) * interpolFraction).toInt()
            b = (startB + (midB - startB) * interpolFraction).toInt()
        } else {
            // Blend between colorMid and colorEnd
            val interpolFraction = (fraction - 0.5f) * 2
            val midR = (colorMid shr 16) and 0xFF
            val midG = (colorMid shr 8) and 0xFF
            val midB = colorMid and 0xFF

            val endR = (colorEnd shr 16) and 0xFF
            val endG = (colorEnd shr 8) and 0xFF
            val endB = colorEnd and 0xFF

            r = (midR + (endR - midR) * interpolFraction).toInt()
            g = (midG + (endG - midG) * interpolFraction).toInt()
            b = (midB + (endB - midB) * interpolFraction).toInt()
        }

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }


    private fun moveToPhoneAuth() {
        val intent = Intent(this, PhoneAuth::class.java)
        intent.putExtra("FROM_OTP_VERIFICATION", true)
        startActivity(intent)
        finish() // Close the OTPVerification activity
    }
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        valueAnimator?.cancel()
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}


