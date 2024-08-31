package com.example.agrigrow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConnectFragment : Fragment() {

    private val sellersRef = Firebase.firestore.collection("SELLERS")
    private val buyersRef = Firebase.firestore.collection("BUYERS")
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val db: FirebaseFirestore = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connect, container, false)
        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.RecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        userAdapter = UserAdapter(userList)
        recyclerView.adapter = userAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchUsers()
    }

    private fun fetchUsers() {
        val allUsers = mutableListOf<User>()

        val sellerTask = sellersRef.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val user = document.toObject(User::class.java).copy(uuid = document.id, userType = "Seller")
                allUsers.add(user)
            }
            // Continue fetching buyers once sellers are done
            fetchBuyers(allUsers)
        }.addOnFailureListener { exception ->
            Log.e("Connect", "Error fetching sellers", exception)
            Toast.makeText(context, "Error fetching sellers: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchBuyers(allUsers: MutableList<User>) {
        buyersRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val user = document.toObject(User::class.java).copy(uuid = document.id, userType = "Buyer")
                    allUsers.add(user)
                }
                userList.clear()
                userList.addAll(allUsers)
                userAdapter.updateUsers(userList)
                Log.d("Connect", "Fetched ${userList.size} users")
            }
            .addOnFailureListener { exception ->
                Log.e("Connect", "Error fetching buyers", exception)
                Toast.makeText(context, "Error fetching buyers: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterUsers(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            userList
        } else {
            userList.filter {
                it.name.contains(query, true)
            }
        }
        userAdapter.updateUsers(filteredList)
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ConnectFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
