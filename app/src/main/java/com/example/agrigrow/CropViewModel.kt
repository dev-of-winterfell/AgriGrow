package com.example.agrigrow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CropViewModel : ViewModel() {
    private val _selectedCrops = MutableLiveData<List<homeFragment.CropDetail>>()
    val selectedCrops: LiveData<List<homeFragment.CropDetail>> get() = _selectedCrops

    fun updateSelectedCrops(crops: List<homeFragment.CropDetail>) {
        _selectedCrops.value = crops
    }
}