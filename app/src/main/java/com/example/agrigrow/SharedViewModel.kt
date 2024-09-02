// SharedViewModel.kt
package com.example.agrigrow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _crops = MutableLiveData<MutableList<homeFragment.CropDetail>>()
    val crops: LiveData<MutableList<homeFragment.CropDetail>> get() = _crops
    private val _maxPrice = MutableLiveData<Float>()
    val maxPrice: LiveData<Float> get() = _maxPrice
    init {
        _crops.value = mutableListOf()
        _maxPrice.value = 0f // Default maxPrice
    }

    fun addCrop(crop: homeFragment.CropDetail) {
        _crops.value?.add(crop)
        _crops.value = _crops.value // Trigger LiveData update

    }
    fun setMaxPrice(price: Float) {
        _maxPrice.value = price
    }

}
