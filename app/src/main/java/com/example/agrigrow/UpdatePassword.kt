package com.example.agrigrow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.agrigrow.databinding.ActivityUpdatePasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UpdatePassword : AppCompatActivity() {
    private lateinit var binding: ActivityUpdatePasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.changePasswordButton.setOnClickListener {
            val email = binding.cnfnewpass.text.toString().trim()
            if (email.isNotEmpty()) {
                startPasswordReset(email)
            } else {
                Toast.makeText(this@UpdatePassword, "Please enter your email address.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPasswordReset(email: String) {
        // Show the progress bar on the main thread
        binding.progressBar4.visibility = View.VISIBLE

        // Perform the network operation on an IO thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                // Switch to the main thread to update the UI
                withContext(Dispatchers.Main) {
                    binding.progressBar4.visibility = View.GONE
                    Toast.makeText(this@UpdatePassword, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@UpdatePassword, LoginPage::class.java))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar4.visibility = View.GONE
                    handleException(e)
                }
            }
        }
    }

    private fun handleException(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                Toast.makeText(this, "No user found with this email.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
