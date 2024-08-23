

package com.example.agrigrow

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.reflect.TypeToken
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class ItemsSharedDialogFragment : BottomSheetDialogFragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var sharedPreferences: SharedPreferences

    private val SESSION_KEY_PHONE_AUTH = "PHONE_AUTH_SESSION"

    private val MAX_OTP_ATTEMPTS = 3

    private val PHONE_ATTEMPTS_KEY = "PHONE_ATTEMPTS"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.signup_dialog_box, container, false)

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
        progressBar = view.findViewById(R.id.progressBar2)
        auth = FirebaseAuth.getInstance()

        sharedPreferences = requireActivity().getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)

        val sendOTPbtn = view.findViewById<Button>(R.id.button3)
        val phoneNumberEditText = view.findViewById<EditText>(R.id.editTextPhone)

        sendOTPbtn.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
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

        callbacks =
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    progressBar.visibility = View.INVISIBLE
                    Log.d("PhoneVerificationDialogFragment", "Verification completed: $credential")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    progressBar.visibility = View.INVISIBLE
                    Log.e("PhoneVerificationDialogFragment", "Verification failed", e)
                    handleVerificationFailure(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    progressBar.visibility = View.INVISIBLE
                    Log.d("PhoneVerificationDialogFragment", "Code sent: $verificationId")

                    sharedPreferences.edit().putString(SESSION_KEY_PHONE_AUTH, verificationId).apply()

                    val phoneNumber = phoneNumberEditText.text.toString().trim()
                    val attemptCount = getAttemptCount(phoneNumber)
                    dismiss()
//                val intent = Intent(requireContext(), OTPVerificationFragment::class.java)
//                intent.putExtra("storedVerificationId", verificationId)
//                intent.putExtra("OTP_ATTEMPTS_LEFT", MAX_OTP_ATTEMPTS - attemptCount)
//                intent.putExtra("PHONE_NUMBER", phoneNumber)
//                startActivity(intent)
                    val otpBottomSheet = OTPVerificationFragment.newInstance(phoneNumber, MAX_OTP_ATTEMPTS - attemptCount, verificationId)
                    otpBottomSheet.show(parentFragmentManager, otpBottomSheet.tag)
                }
            }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth
            .signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("PhoneVerificationDialogFragment", "signInWithCredential:success")
                    sharedPreferences.edit().putBoolean(SESSION_KEY_PHONE_AUTH, true).apply()
                    // TODO: Handle successful sign-in, e.g., close dialog, navigate to next screen
                } else {
                    Log.w("PhoneVerificationDialogFragment", "signInWithCredential:failure", task.exception)
                    handleSignInFailure(task.exception)
                }
            }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        incrementAttemptCount(phoneNumber)
        progressBar.visibility = View.VISIBLE

        val options =
            PhoneAuthOptions
                .newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
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
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, object : TypeToken<T>() {}.type)

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

    override fun onStart() {
        super.onStart()
        // Set the dialog to cover only a part of the screen
//        dialog?.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            convertDpToPx(450) // Set the height to 450dp
//        )
//
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun convertDpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
