package com.example.agrigrow

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CropDataTransferFromBuyer : DialogFragment() {

    private var cropDetail: homeFragment.CropDetail? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
//        setStyle(STYLE_NORMAL, R.style.FullscreenDialog)
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        cropDetail = arguments?.getParcelable("CROP_DETAIL")

        Log.d("DialogFragment", "DialogFragment opened, CropDetail received: $cropDetail")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_crop_options, container, false)



        // Create a list with your crop detail(s)
        val cropDetailsList = mutableListOf<CropInfo>()
        cropDetail?.let { crop ->
            val item = CropInfo(
                growingMethod = crop.growingMethod,
                state = crop.state,
                cropType = crop.cropType,
                amount = "${crop.amount} क्विंटल"
            )
            cropDetailsList.add(item)
        }
        updateUI(view)



        return view
    }
//    override fun onStart() {
//        super.onStart()
//        dialog?.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,  // Width
//            ViewGroup.LayoutParams.MATCH_PARENT   // Height
//        )
//    }

    private fun updateUI(view: View) {
        cropDetail?.let { crop ->

            Log.d("UpdateUI", "Updating UI with CropDetail: $crop")

            view.findViewById<TextView>(R.id.cropName).text = crop.cropName

            view.findViewById<TextView>(R.id.price).text = "₹${crop.maxPrice}"

            val cropStateTextView = view.findViewById<TextView>(R.id.state)
    // val sellerNameTextView = view.findViewById<TextView>(R.id.sellerName)
            val cropTypeTextView = view.findViewById<TextView>(R.id.cropType)
            val growingMethodTextView = view.findViewById<TextView>(R.id.growingMethod)
            val amountTextView = view.findViewById<TextView>(R.id.amount)


            //   cropStateTextView.text = item?.cropState
      // sellerNameTextView.text = item?.sellerName
            cropTypeTextView.text = crop.cropType
            growingMethodTextView.text = crop.growingMethod
            amountTextView.text = "${crop.amount} क्विंटल"
            cropStateTextView.text = crop.state
            val mediaList = mutableListOf<Uri>()
            Log.d("UpdateUI", "Image URLs: ${crop.imageUrls}")

            crop.imageUrls.forEach { imageUrl ->
                Log.d("UpdateUI", "Adding image URL to media list: $imageUrl")
                mediaList.add(Uri.parse(imageUrl))
            }


            // Add the video URL at the end (if available)
            if (crop.videoUrl.isNotEmpty()) {
                Log.d("UpdateUI", "Adding video URL to media list: ${crop.videoUrl}")
                mediaList.add(Uri.parse(crop.videoUrl))
            }
            // Set up ViewPager2 with MediaAdapter
            val mediaViewPager = view.findViewById<ViewPager2>(R.id.mediaViewPager)
            val mediaAdapter = MediaAdapter(requireContext(), mediaList)
            Log.d("UpdateUI", "Setting up MediaAdapter with media list: $mediaList")
            mediaViewPager.adapter = mediaAdapter

            // Load image using Glide
//            val cropImageView = view.findViewById<ImageView>(R.id.cropImage1)
//            Glide.with(requireContext())
//                .load(crop.imageUrl)
//                .placeholder(R.drawable.baseline_image_24)
//                .error(R.drawable.pikaso_embed_a_middleaged_indian_farmer_wearing_a_turban_and_tr_removebg_preview)
//
//                .into(cropImageView)

            view.findViewById<Button>(R.id.negotiate).setOnClickListener {
                Log.d("CropDataTransfer", "Negotiate button clicked.")
                Log.d("CropDataTransfer", "Storing negotiation data for cropId: ${crop.cropId}")
                storeNegotiationDataInFirestore(crop)

            }
          view.findViewById<Button>(R.id.addtocart1).setOnClickListener {
          Log.d("CropDataTransfer", "Add to Cart button clicked.")
              cropDetail?.let { crop ->
                  storeCropInCart(crop)




              }


}
        }?: run {
            Log.e("UpdateUI", "CropDetail is null, cannot update UI")
        }

    }
    fun updateCropDetail(newCropDetail: homeFragment.CropDetail) {
        cropDetail = newCropDetail
        view?.let { updateUI(it) }
    }


    private fun storeCropInCart(crop: homeFragment.CropDetail) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email ?: run {
                Log.e("Firestore", "User email is null.")
                Toast.makeText(context, "User email is null.", Toast.LENGTH_SHORT).show()
                return
            }

            val cartItem = hashMapOf(
                "cropId" to crop.cropId,
                "cropName" to crop.cropName,
                "cropType" to crop.cropType,
                "growingMethod" to crop.growingMethod,
                "state" to crop.state,
                "amount" to crop.amount,
                "maxPrice" to crop.maxPrice,
                "imageUrl" to crop.imageUrl,
                "sellerUUId" to crop.sellerUUId
            )

            // Store the crop details under CART_ITEMS collection for the user
            val userDocumentRef = firestore.collection("BUYERS").document(userEmail)
            userDocumentRef.collection("CART_ITEMS")
                .document(crop.cropId)  // Use the cropId as the document ID
                .set(cartItem)
                .addOnSuccessListener {
                    Log.d("Firestore", "Crop added to cart successfully: ${crop.cropId}")
                    Toast.makeText(context, "Crop added to cart", Toast.LENGTH_SHORT).show()
                    dismiss()  // Close the dialog after adding to the cart
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to add crop to cart", e)
                    Toast.makeText(context, "Failed to add crop to cart: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // Optionally, update the ViewModel if you want to track cart items in real-time
            val viewModel = ViewModelProvider(requireActivity()).get(CropViewModel::class.java)
            val currentCartList = viewModel.selectedCrops.value?.toMutableList() ?: mutableListOf()
            currentCartList.add(crop)
            viewModel.updateSelectedCrops(currentCartList)
        } else {
            Log.e("Firestore", "User not authenticated.")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }


    private fun storeNegotiationDataInFirestore(crop: homeFragment.CropDetail) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email ?: run {
                Log.e("Firestore", "User email is null.")
                Toast.makeText(context, "User email is null.", Toast.LENGTH_SHORT).show()
                return
            }
            val userDocumentRef = firestore.collection("BUYERS").document(userEmail)

            val negotiationData = hashMapOf(

                "userName" to user.displayName,
                "cropId" to crop.cropId,
                "cropName" to crop.cropName,
                "cropType" to crop.cropType,
                "growingMethod" to crop.growingMethod,
                "state" to crop.state,
                "amount" to crop.amount,
                "maxPrice" to crop.maxPrice,
                "imageUrl" to crop.imageUrl,
                "sellerUUId" to crop.sellerUUId
            )

            Log.d("Firestore", "Attempting to store data: $negotiationData")

            userDocumentRef.collection("NEGOTIATED_CROPS").document(crop.cropId).set(negotiationData)
                .addOnSuccessListener {
                    Log.d("Firestore", "Negotiation data saved successfully.for cropID:${crop.cropId} and sellerUUId: ${crop.sellerUUId}")
                    Toast.makeText(context, "Negotiation data saved successfully", Toast.LENGTH_SHORT).show()
                      dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to save negotiation data", e)
                    Toast.makeText(context, "Failed to save negotiation data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("Firestore", "User not authenticated.")
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
        val viewModel = ViewModelProvider(requireActivity()).get(CropViewModel::class.java)
        val currentList = viewModel.selectedCrops.value?.toMutableList() ?: mutableListOf()
        currentList.add(crop)
        viewModel.updateSelectedCrops(currentList)
    }

    companion object {
        fun newInstance(cropDetail: homeFragment.CropDetail): CropDataTransferFromBuyer {
            val fragment = CropDataTransferFromBuyer()
            val args = Bundle()
            args.putParcelable("CROP_DETAIL", cropDetail)
            fragment.arguments = args
            return fragment
        }


    }
}