package com.example.agrigrow

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.android.parcel.Parcelize

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [homeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class homeFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var cropAdapter: CropAdapter
    private val cropList = mutableListOf<CropDetail>()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sellerRecyclerView: RecyclerView
    private lateinit var sellerAdapter: SellerAdapter
    private val sellerList = mutableListOf<Seller>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        firestore = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        cropAdapter = CropAdapter(cropList){ cropDetail ->
            navigateToCropDetail(cropDetail)

        }
        recyclerView.adapter = cropAdapter
        sellerRecyclerView = view.findViewById(R.id.horizontalScrollView)
        sellerRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        sellerAdapter = SellerAdapter(sellerList)
        sellerRecyclerView.adapter = sellerAdapter

        fetchSellerDetails()
        // Set up the buyer's name
        val layout = view.findViewById<LinearLayout>(R.id.linearLayoutbuyer)
        val tvBuyer = layout.findViewById<TextView>(R.id.tvbuyer)
        setupBuyerName(tvBuyer)

        val userId = FirebaseAuth.getInstance().currentUser?.email
        if (userId != null) {
            firestore.collection("BUYERS").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("uuid") ?: "No Name Available"
                        fetchCropDetails(userName)
                    }
                }
        }


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val activity = activity as? BuyerLandingPage
                if (dy > 0) {
                    // User is scrolling up -> Hide Bottom Navigation
                    Log.d("homeFragment", "User scrolling up, hiding BottomNav")
                    activity?.hideBottomNavBar()
                } else if (dy < 0) {
                    // User is scrolling down -> Show Bottom Navigation
                    Log.d("homeFragment", "User scrolling down, showing BottomNav")
                    activity?.showBottomNavBar()
                }
            }
        })




        return view
    }
    private fun navigateToCropDetail(cropDetail: CropDetail) {
        Log.d("CropSelection", "Selected Crop ID: ${cropDetail.cropId}")
        Log.d("CropSelection", "Seller UUID: ${cropDetail.sellerUUId}")

        // Check if the dialog is already showing
        val existingDialog = parentFragmentManager.findFragmentByTag("crop_options_dialog")
        if (existingDialog == null) {
            val cropOptionsDialog = CropDataTransferFromBuyer.newInstance(cropDetail)
            cropOptionsDialog.show(parentFragmentManager, "crop_options_dialog")
        } else {
            Log.d("CropSelection", "Dialog is already showing")
            // Optionally, you can update the existing dialog with new data
            (existingDialog as? CropDataTransferFromBuyer)?.updateCropDetail(cropDetail)
        }

        // Pass maxPrice to shared ViewModel
        sharedViewModel.setMaxPrice(cropDetail.maxPrice)

        // Navigate to BuyerBargainFragment and pass cropId and maxPrice
//        val buyerBargainFragment = BuyerBargain.newInstance(cropDetail.cropId, cropDetail.maxPrice,cropDetail.sellerUUId)
//        requireActivity().supportFragmentManager.beginTransaction()
//            .replace(R.id.frameLayout1, buyerBargainFragment)
//            .addToBackStack(null)
//            .commit()
    }


    private fun fetchSellerDetails() {
        firestore.collection("SELLERS")
            .get()
            .addOnSuccessListener { documents ->
                sellerList.clear()
                for (document in documents) {
                    val seller = document.toObject(Seller::class.java)
                    sellerList.add(seller)
                }
                sellerAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Error fetching seller data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    //    private fun onCropSelected(cropDetail: homeFragment.CropDetail) {
//        sharedViewModel.addCrop(cropDetail)
//    }
    private fun setupBuyerName(tvBuyer: TextView) {
        val userId = FirebaseAuth.getInstance().currentUser?.email
        if (userId != null) {
            firestore.collection("BUYERS").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("Name") ?: "No Name Available"
                        tvBuyer.text = userName
                    } else {
                        tvBuyer.text = "No user data available"
                    }
                }.addOnFailureListener {
                    tvBuyer.text = "Failed to load user data"
                }
        } else {
            tvBuyer.text = "User not logged in"
        }
    }

    private fun fetchCropDetails(buyerUUId: String) {
        firestore.collection("SELLERS")
            .get()
            .addOnSuccessListener { sellerDocuments ->
                cropList.clear() // Clear the existing list before adding new data
                for (sellerDocument in sellerDocuments) {
                    val sellerName = sellerDocument.getString("Name") ?: "Unknown Seller"
                    val sellerUUID = sellerDocument.getString("uuid") ?: "" // Fetch the uuid from the seller's document
                    val crops = sellerDocument.get("crops") as? List<Map<String, Any>> ?: continue


                    for (crop in crops) {
                        val imageUrls = crop["imageUrls"] as? List<String> ?: emptyList()
                        val firstImageUrl = imageUrls.getOrNull(0) ?: ""
                        val videoUrl = crop["videoUrl"] as? String ?: ""
                        // Create a CropDetail object for each crop in the seller's list
                        val cropDetail = CropDetail(
                            cropName = crop["cropName"] as? String ?: "",
                            cropType = crop["cropType"] as? String ?: "",
                            growingMethod = crop["growingMethod"] as? String ?: "",
                            minPrice = (crop["minPrice"] as? Number)?.toFloat() ?: 0f,
                            maxPrice = (crop["maxPrice"] as? Number)?.toFloat() ?: 0f,
                            state = crop["state"] as? String ?: "",
                            amount = (crop["amount"] as? Number)?.toInt() ?: 0,
                           // imageUrl = crop["imageUrl"] as? String ?: "",
                            imageUrl = firstImageUrl,
                            imageUrls = imageUrls,
                            videoUrl = videoUrl,
                            ownerName = sellerName,  // Set the seller's name as the owner
                            cropId = crop["cropId"] as? String ?: "", // Fetch cropId
                            sellerUUId = sellerUUID,  // Assign the fetched uuid to the sellerUUId property
                        )
                        // Add the cropDetail to the list
                        cropList.add(cropDetail)
                    }
                }
                cropAdapter.notifyDataSetChanged() // Notify the adapter about data changes
            }
            .addOnFailureListener { exception ->
                // Handle any errors that occur during the Firestore query
                Log.e("Firestore Error", "Error fetching crops: ${exception.message}")
            }
    }




    data class Seller(
        val Name: String = "",
        val profileImageUrl: String = ""
    )

    class SellerAdapter(private val sellers: List<Seller>) :
        RecyclerView.Adapter<SellerAdapter.SellerViewHolder>() {

        class SellerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val profileImage: ImageView = view.findViewById(R.id.profileImage)
            val sellerName: TextView = view.findViewById(R.id.sellerName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.seller_item, parent, false)
            return SellerViewHolder(view)
        }

        override fun onBindViewHolder(holder: SellerViewHolder, position: Int) {
            val seller = sellers[position]
            holder.sellerName.text = seller.Name

            Glide.with(holder.itemView.context)
                .load(seller.profileImageUrl)
                .placeholder(R.drawable.baseline_image_24) // Set a placeholder image while loading
                .error(R.drawable.pikaso_embed_a_middleaged_indian_farmer_wearing_a_turban_and_tr_removebg_preview) // Set an error image if loading fails
                .apply(RequestOptions().circleCrop())
                .into(holder.profileImage)
        }

        override fun getItemCount() = sellers.size
    }

    @Parcelize
    data class CropDetail(
        val cropId: String = "",
        val cropName: String = "NAME",
        val cropType: String = "",
        val growingMethod: String = "",
        val minPrice: Float = 0f,
        val imageUrls: List<String> = emptyList(), // List of image URLs
        val videoUrl: String = "", // Video URL
        val maxPrice: Float = 0f,
        val state: String = "",
        val amount: Int = 0,
        val imageUrl: String = "",
        val ownerName: String = "",
        var sellerUUId:String="",
        var negotiatedPrice: Float = 0f

    ) : Parcelable


    class CropAdapter(
        private val crops: List<CropDetail>,
        private val onItemClick: (CropDetail) -> Unit
    ) :
        RecyclerView.Adapter<CropAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ownerName: TextView = view.findViewById(R.id.ownername)
            val cropImage: ImageView = view.findViewById(R.id.cropImage)
            val cropName: TextView = view.findViewById(R.id.cropName)
            val cropType: TextView = view.findViewById(R.id.cropType)
            val growingMethod: TextView = view.findViewById(R.id.growingMethod)
            val price: TextView = view.findViewById(R.id.price)
            val state: TextView = view.findViewById(R.id.state)
            val amount: TextView = view.findViewById(R.id.amount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =LayoutInflater.from(parent.context).inflate(R.layout.crop_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val crop = crops[position]
            holder.ownerName.text = crop.ownerName
            holder.cropName.text = crop.cropName
            holder.cropType.text = crop.cropType
            holder.growingMethod.text = crop.growingMethod
            holder.price.text = "₹${crop.maxPrice}"
            holder.state.text = crop.state
            holder.amount.text = "${crop.amount} क्विंटल"


            Glide.with(holder.itemView.context)
                .load(crop.imageUrl)
                .placeholder(R.drawable.baseline_image_24) // Set a placeholder image while loading
                .error(R.drawable.pikaso_embed_a_middleaged_indian_farmer_wearing_a_turban_and_tr_removebg_preview) // Set an error image if loading fails
                .apply(RequestOptions().transform(RoundedCorners(12)))
                .apply(RequestOptions().centerCrop())
                .into(holder.cropImage);


            holder.itemView.setOnClickListener { onItemClick(crop) }
//            holder.cropImage.setOnClickListener { showDialog(crop) }
//            holder.cropName.setOnClickListener { showDialog(crop) }
//            holder.cropType.setOnClickListener { showDialog(crop) }
//            holder.price.setOnClickListener { showDialog(crop) }
//            holder.amount.setOnClickListener { showDialog(crop) }

        }

        override fun getItemCount() = crops.size

//        private fun showDialog(crop: CropDetail) {
//            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_crop_options, null)
//            val builder = AlertDialog.Builder(context)
//            builder.setView(dialogView)
//            val dialog = builder.create()
//
//            val negotiate = dialogView.findViewById<Button>(R.id.negotiate)
//            val addToCart = dialogView.findViewById<Button>(R.id.addtocart)
//
//            negotiate.setOnClickListener {
//                // Navigate to BuyerBargainFragment and pass crop details
//
//                dialog.dismiss()
//            }
//
//            addToCart.setOnClickListener {
//                // Implement functionality to add to cart here
//                Toast.makeText(context, "Added ${crop.name} to the cart", Toast.LENGTH_SHORT).show()
//                dialog.dismiss()
//            }
//
//            dialog.show()
//        }




        companion object {
            fun newInstance(
                cropId: String,
                sellerUUId: String,
                name: String,
                type: String,
                growingMethod: String,
                minPrice: Float,
                maxPrice: Float,
                state: String,
                amount: Int,
                imageUrl: String,
                ownerName: String
            ): CropDataTransferFromBuyer {
                val fragment = CropDataTransferFromBuyer()
                val args = Bundle()
                args.putString("cropId", cropId)
                args.putFloat("maxPrice", maxPrice)
                args.putString("sellerUUId", sellerUUId)
                args.putString("name", name)
                args.putString("type", type)
                args.putString("growingMethod", growingMethod)
                args.putFloat("minPrice", minPrice)
                args.putFloat("maxPrice", maxPrice)
                args.putString("state", state)
                args.putInt("amount", amount)
                args.putString("imageUrl", imageUrl)
                args.putString("ownerName", ownerName)
                fragment.arguments = args
                return fragment
            }
        }

    }
}


