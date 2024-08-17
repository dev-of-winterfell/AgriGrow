package com.example.agrigrow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BuyerOrSellerDecider : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buyer_or_seller_decider)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val buyer=findViewById<Button>(R.id.button)
        val seller=findViewById<Button>(R.id.button2)

        buyer.setOnClickListener { startActivity(Intent(this@BuyerOrSellerDecider,BuyerSignupPage::class.java)) }
        seller.setOnClickListener { startActivity(Intent(this@BuyerOrSellerDecider,SellersSignupPage::class.java)) }
    }
}