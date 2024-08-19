package com.example.agrigrow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BuyerOrSellerDecider2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buyer_or_seller_decider2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val buyer=findViewById<Button>(R.id.button10)
        val seller=findViewById<Button>(R.id.button20)

        buyer.setOnClickListener { startActivity(Intent(this@BuyerOrSellerDecider2,phoneAuthBuyerDetails::class.java) )}
        seller.setOnClickListener { startActivity(Intent(this@BuyerOrSellerDecider2,phoneAuthUserDetailsPage::class.java) )}
    }
}