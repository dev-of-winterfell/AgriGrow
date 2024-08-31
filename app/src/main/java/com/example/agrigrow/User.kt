package com.example.agrigrow

import com.google.firebase.firestore.PropertyName

data class User(

    @field:JvmField @PropertyName("Name") val name: String = "",
val userType: String = "",  // "Seller" or "Buyer"
    @field:JvmField @PropertyName("profileImageUrl") val profileImageUrl: String = "",
    val uuid: String = ""
)


