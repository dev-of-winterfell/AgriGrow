package com.example.agrigrow


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SellerBargain : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var cropAdapter: CropListAdapter
    private val cropList = mutableListOf<homeFragment.CropDetail>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restore state if available
        savedInstanceState?.let {
            val savedCrops = it.getParcelableArrayList<homeFragment.CropDetail>("CROP_LIST")
            savedCrops?.let { crops ->
                cropList.addAll(crops)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current state
        outState.putParcelableArrayList("CROP_LIST", ArrayList(cropList))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_seller_bargain, container, false) // Use correct layout

              //        recyclerView = view.findViewById(R.id.rv )

               //        cropAdapter = CropListAdapter(cropList)
               //
           //        recyclerView.adapter = cropAdapter
            //        recyclerView.layoutManager = LinearLayoutManager(context)

                  //        arguments?.let {
                 //            val newCrops = it.getParcelableArrayList<homeFragment.CropDetail>("CROP_LIST")
                 //            newCrops?.let { crops ->
                 //                cropList.clear() // Clear the list to avoid duplicate items
                //                cropList.addAll(crops)
               //                cropAdapter.notifyDataSetChanged() // Notify adapter of data changes
                  //            }
             //        }
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        sharedViewModel.crops.observe(viewLifecycleOwner) { crops ->
            cropList.clear()
            cropList.addAll(crops)
            cropAdapter.notifyDataSetChanged()
        }


        return view
    }

    companion object {
        fun newInstance(cropList: List<homeFragment.CropDetail>): BuyerBargain {
            return BuyerBargain().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("CROP_LIST", ArrayList(cropList))
                }
            }
        }
    }

}




