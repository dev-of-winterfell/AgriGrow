package com.example.agrigrow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener // Import the interface
import org.json.JSONObject

class Payment : AppCompatActivity(), PaymentResultListener { // Implement the interface
    private var totalPrice: Float = 0f // Total price passed from CartFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        totalPrice = intent.getFloatExtra("TOTAL_PRICE", 0f)

        Checkout.preload(applicationContext)
        startPayment()
    }

    private fun startPayment() {
        val activity = this
        val co = Checkout()

        // Set your Razorpay API key (Key ID)
        // For testing, you can hardcode your Test API Key here
        // Replace "rzp_test_YourKeyId" with your actual Test Key ID
        co.setKeyID("rzp_test_E1APpQ25ic3Rwy")

        try {
            val options = JSONObject()
            options.put("name", "Fasal Vypar") // Replace with your app's name
            options.put("description", "Order Payment")
            options.put("image", "https://imgur.com/a/XpFgUho") // Optional: Add your logo URL
            options.put("currency", "INR")
            options.put("enable_otp_auto_read", false)

            // Convert total price to paise (multiply by 100)
            val amountInPaise = (totalPrice * 100).toInt()
            options.put("amount", amountInPaise)

            // Prefill user information (optional)
            val prefill = JSONObject()
            prefill.put("email", "user@example.com")
            prefill.put("contact", "9876543210")
            options.put("prefill", prefill)

            // Additional options (optional)
            val theme = JSONObject()
            theme.put("color", "#386641") // Customize the payment form color
            options.put("theme", theme)

            // Open Razorpay Checkout Activity
            co.open(activity, options)

        } catch (e: Exception) {
            Toast.makeText(activity, "Error in payment: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // Implement the PaymentResultListener methods
    override fun onPaymentSuccess(razorpayPaymentID: String) {
        Toast.makeText(this, "Payment Successful: $razorpayPaymentID", Toast.LENGTH_SHORT).show()
        // Handle post-payment success actions here
        // For example, you might navigate to an order confirmation screen or update the order status in your database
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_SHORT).show()
        // Handle payment failure actions here
        // For example, you might show an error dialog or allow the user to retry the payment
    }

    override fun onDestroy() {
        super.onDestroy()
        Checkout.clearUserData(this)
    }
}
