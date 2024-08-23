package com.example.agrigrow

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.agrigrow.databinding.FragmentOtpBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class OTPVerificationFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentOtpBottomSheetBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var tvTimer: TextView
    private lateinit var progressBarTimer: ProgressBar
    private var countDownTimer: CountDownTimer? = null
    private var valueAnimator: ValueAnimator? = null
    private var timeRemaining = 60 // Total time in seconds
    private var storedVerificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentOtpBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                it.layoutParams.height = convertDpToPx(450)
                behavior.peekHeight = convertDpToPx(450)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        tvTimer = binding.tvTimer
        progressBarTimer = binding.progressBarTimer

        // Retrieve data from arguments or use default values
//        val phoneNumber = arguments?.getString("PHONE_NUMBER") ?: ""
//        val attemptsLeft = arguments?.getInt("OTP_ATTEMPTS_LEFT", 3) ?: 3
        storedVerificationId = arguments?.getString("storedVerificationId")

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        progressBarTimer.max = timeRemaining
        progressBarTimer.progress = 0

        // Start the countdown timer
        startTimer()

        if (storedVerificationId == null) {
            Toast.makeText(context, "Verification ID not found", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        binding.buttonVerify.setOnClickListener {
            val otp = getEnteredOTP()
            if (otp.length == 6) {
                binding.progressBar3.visibility = View.VISIBLE
                verifyPhoneNumberWithCode(storedVerificationId, otp)
            } else {
                Toast.makeText(context, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        setupOtpInput()
    }

    private fun getEnteredOTP(): String =
        binding.run {
            editTextOTP1.text.toString().trim() +
                editTextOTP2.text.toString().trim() +
                editTextOTP3.text.toString().trim() +
                editTextOTP4.text.toString().trim() +
                editTextOTP5.text.toString().trim() +
                editTextOTP6.text.toString().trim()
        }

    private fun verifyPhoneNumberWithCode(
        verificationId: String?,
        code: String,
    ) {
        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            lifecycleScope.launch(Dispatchers.Main) {
                binding.progressBar3.visibility = View.VISIBLE
                signInWithPhoneAuthCredential(credential)
            }
        } else {
            binding.progressBar3.visibility = View.INVISIBLE
            Toast.makeText(context, "Verification ID is null", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        try {
            val authResult =
                withContext(Dispatchers.IO) {
                    auth.signInWithCredential(credential).await()
                }

            binding.progressBar3.visibility = View.INVISIBLE
            // Inside the signInWithPhoneAuthCredential function after successful authentication
            if (authResult.user != null) {
                // Store user authentication state (e.g., user ID) for session management
                val userId = authResult.user?.uid
                // Pass user data to com.example.gradx.com.example.gradx.LandingPage
                val intent = Intent(context, phoneAuthUserDetailsPage::class.java)
                intent.putExtra("userId", userId)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                dismiss()
            } else {
                Toast.makeText(context, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show()
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            binding.progressBar3.visibility = View.INVISIBLE
            Toast.makeText(context, "Invalid OTP", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            binding.progressBar3.visibility = View.INVISIBLE
            Toast.makeText(context, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupOtpInput() {
        val editTexts =
            listOf(
                binding.editTextOTP1,
                binding.editTextOTP2,
                binding.editTextOTP3,
                binding.editTextOTP4,
                binding.editTextOTP5,
                binding.editTextOTP6,
            )

        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {}

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
                },
            )
        }
    }

    private fun startTimer() {
        countDownTimer =
            object : CountDownTimer(timeRemaining * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeRemaining = (millisUntilFinished / 1000).toInt()

                    // Format the time as MM:SS
//                    val minutes = timeRemaining / 60
//                    val seconds = timeRemaining % 60
                    val timeRemainingDuration = timeRemaining.seconds
                    val timeFormatted =
                        timeRemainingDuration.toComponents { minutes, seconds ->
                            String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                minutes,
                                seconds,
                            ) // Use Locale.getDefault()
                        }
                    tvTimer.text = timeFormatted
                    updateProgressBar(60 - timeRemaining)
                }

                override fun onFinish() {
                    tvTimer.text = "00:00"
                    updateProgressBar(60)

                    // Check if the dialog is already shown
                    val existingDialog = parentFragmentManager.findFragmentByTag("PhoneVerificationDialog") as? ItemsSharedDialogFragment

                    if (existingDialog == null) {
                        // The dialog is not currently shown, so show it
                        val dialog = ItemsSharedDialogFragment()
                        dialog.show(parentFragmentManager, "PhoneVerificationDialog")
                    } else {
                        // Optionally, you can dismiss the existing dialog or update it if needed
                        existingDialog.dismiss()
                    }

                    // Dismiss the current fragment
                    dismiss()

//                val intent = Intent(requireContext(), ItemsSharedDialogFragment::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
                }
            }.start()
    }

    private fun updateProgressBar(progress: Int) {
        valueAnimator?.cancel()
        valueAnimator =
            ValueAnimator.ofInt(progressBarTimer.progress, progress).apply {
                duration = 1000 // Animation duration in milliseconds
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    progressBarTimer.progress = animation.animatedValue as Int
                }
                start()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        valueAnimator?.cancel()
        dismiss()
    }

    companion object {
        fun newInstance(
            phoneNumber: String,
            otpAttemptsLeft: Int,
            verificationId: String,
        ): OTPVerificationFragment {
            val fragment = OTPVerificationFragment()
            val args =
                Bundle().apply {
                    putString("PHONE_NUMBER", phoneNumber)
                    putInt("OTP_ATTEMPTS_LEFT", otpAttemptsLeft)
                    putString("storedVerificationId", verificationId)
                }
            fragment.arguments = args
            return fragment
        }
    }

    private fun convertDpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
