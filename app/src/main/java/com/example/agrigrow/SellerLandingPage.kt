package com.example.agrigrow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.agrigrow.databinding.ActivityLandingPageBinding
import com.example.agrigrow.databinding.ActivitySellerLandingPageBinding
import com.example.sellerProfileFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SellerLandingPage : AppCompatActivity() {
    companion object {
        private const val PROFILE_IMAGE_URI_KEY = "ProfileImageUri"
        private const val SHARED_PREFS_KEY = "GradxPrefs"
    }
    private lateinit var binding: ActivitySellerLandingPageBinding

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var progressBar: ProgressBar
    private val REQUEST_IMAGE_PICK = 1002
    //    private val PROFILE_IMAGE_URI_KEY = "ProfileImageUri"
//    private val SHARED_PREFS_KEY = "GradxPrefs"
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySellerLandingPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navigationView = findViewById<NavigationView>(R.id.navigationView1)
        val headerView = navigationView.getHeaderView(0)
        val profileImageView = headerView.findViewById<CircleImageView>(R.id.profilepic)
//        val nameTextView = headerView.findViewById<TextView>(R.id.name)
//        val emailTextView = headerView.findViewById<TextView>(R.id.email)

        sharedPreferences = getSharedPreferences("GradxPrefs", Context.MODE_PRIVATE)
        auth = Firebase.auth
        progressBar = headerView.findViewById(R.id.progressBar6)
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null ) {

                startActivity(Intent(this@SellerLandingPage, WelcomePage::class.java))
                finish()
            } else{
                loadUserData(profileImageView)
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firestore = FirebaseFirestore.getInstance()
        storage = Firebase.storage
        sharedPreferences = getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE)

        drawerLayout = binding.drawer



//        val isLoggedIn = sharedPreferences.getBoolean("IS_LOGGED_IN", false)
        profileImageView.setOnClickListener {
            openImagePicker()
        }

//        if (!isLoggedIn || auth.currentUser == null) {
//            startActivity(Intent(this@BuyerLandingPage, LoginPage::class.java))
//            finish()
//        } else {
//            emailTextView.text = auth.currentUser?.email
//            loadUserData(auth.currentUser?.displayName ?: "", nameTextView, profileImageView)
//        }

        val savedImageUri = sharedPreferences.getString(PROFILE_IMAGE_URI_KEY, null)
        if (savedImageUri != null) {
            Glide.with(this)
                .load(savedImageUri)
                .placeholder(R.drawable.baseline_person_24) // Placeholder while loading
                .error(R.drawable.baseline_error_24)
                .into(profileImageView)
        }

        val drawernavView = findViewById<FrameLayout>(R.id.drawernav)
        val menuBtn = drawernavView.findViewById<ImageView>(R.id.menubtn)
        menuBtn.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    logoutUser()
                    true
                }
                R.id.colormode -> {
                    toggleDarkMode()
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigationView1.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> FragmentHandler(SellerHomePage())
                R.id.connect -> FragmentHandler(SellerBargain())
                R.id.profile -> FragmentHandler(sellerProfileFragment())
                R.id.message -> FragmentHandler(UploadNewCrops())
                else -> showFragment(SellerHomePage())
            }
            true
        }
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // This will be called when the drawer is sliding
            }

            override fun onDrawerOpened(drawerView: View) {
                // Hide BottomNavigationView when the drawer is opened
                binding.bottomNavigationView1.visibility = View.GONE
            }

            override fun onDrawerClosed(drawerView: View) {
                // Show BottomNavigationView when the drawer is closed
                binding.bottomNavigationView1.visibility = View.VISIBLE
            }

            override fun onDrawerStateChanged(newState: Int) {
                // This will be called when the drawer motion state changes
            }
        })

        if (savedInstanceState == null) {
            binding.bottomNavigationView1.selectedItemId = R.id.home
        }
    }

    private fun FragmentHandler(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout1, fragment)
            .commit()
    }

    private fun showFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout1, fragment)
            .commit()
        return true
    }

    private fun loadUserData(


        profileImageView: CircleImageView
    ) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        lifecycleScope.launch {
            try {
                val documents = withContext(Dispatchers.IO) {
                    firestore.collection("SELLERS").whereEqualTo("Email", email).get().await()
                }
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val name =document.getString("Name")
                    //  val firestoreProfileImageUrl = document.getString("profileImageUrl")
                    var profileImageUrl = sharedPreferences.getString(PROFILE_IMAGE_URI_KEY, null)?: document.getString("profileImageUrl")

                    if (profileImageUrl.isNullOrEmpty()) {
                        profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            saveImageUriToSharedPreferences(profileImageUrl)
                        }
                    }

                    withContext(Dispatchers.Main) {


                        if (!profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@SellerLandingPage)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.baseline_person_24)
                                .error(R.drawable.baseline_error_24)
                                .into(profileImageView)
                        } else {
                            Glide.with(this@SellerLandingPage)
                                .load(R.drawable.baseline_person_24)
                                .into(profileImageView)
                        }
                    }
                } else {
                    Log.d("Profile", "No user found with email: $email")
                }
            } catch (exception: Exception) {
                Log.e("Profile", "Error getting user data: $exception")
            }
        }
    }
    private fun logoutUser() {
        progressBar.visibility = View.VISIBLE
        auth.signOut()
        sharedPreferences.edit()
            .putBoolean("IS_LOGGED_IN", false)
            .remove(PROFILE_IMAGE_URI_KEY)
            .apply()
        progressBar.visibility = View.GONE
        // The Auth state listener will handle navigation to LoginPage
    }

    private fun toggleDarkMode() {
        progressBar.visibility = View.VISIBLE
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        this@SellerLandingPage.recreate()
        progressBar.visibility = View.GONE
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                uploadImageToFirebase(imageUri)
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        progressBar.visibility = View.VISIBLE
        val userId = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: return
        val storageRef = storage.reference.child("profileImages/$userId.jpg")

        lifecycleScope.launch {
            try {
                val uploadTask = withContext(Dispatchers.IO) {
                    storageRef.putFile(imageUri).await()
                }
                val downloadUrl = uploadTask.storage.downloadUrl.await()
                val imageUrlString = downloadUrl.toString()
                saveImageUriToSharedPreferences(imageUrlString)
                updateUserProfileImage(imageUrlString, email)
                loadImageIntoHeader(downloadUrl)
            } catch (exception: Exception) {
                Log.e("Upload", "Failed to upload image: $exception")
                if (exception is com.google.firebase.storage.StorageException) {
                    Log.e("Upload", "StorageException: ${exception.message}")
                }
            } finally {
                // Hide progress bar after upload completes (success or failure)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateUserProfileImage(downloadUrl: String, email: String) {
        progressBar.visibility = View.VISIBLE
        val userDocument = firestore.collection("SELLERS").document(email)
        userDocument.update("profileImageUrl", downloadUrl)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Log.d("Firestore", "User profile image updated successfully.")
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("Firestore", "Error updating user profile image: $e")
            }
    }

    private fun saveImageUriToSharedPreferences(imageUri: String) {
        sharedPreferences.edit().putString(PROFILE_IMAGE_URI_KEY, imageUri).apply()
    }

    private fun loadImageIntoHeader(imageUri: Uri) {
        progressBar.visibility = View.VISIBLE
        val navigationView = findViewById<NavigationView>(R.id.navigationView1)
        val headerView = navigationView.getHeaderView(0)
        val profileImageView = headerView.findViewById<CircleImageView>(R.id.profilepic)

        Glide.with(this)
            .load(imageUri)
            .into(profileImageView)
        progressBar.visibility = View.GONE
    }
    private fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
