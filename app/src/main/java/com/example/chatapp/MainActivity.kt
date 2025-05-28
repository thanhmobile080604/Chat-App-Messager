package com.example.chatapp


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.chatapp.fragment.HomeFragmentDirections
import com.example.chatapp.model.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {
    private lateinit var navControler: NavController
    private val firestore = FirebaseFirestore.getInstance()
    private var auth = FirebaseAuth.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFrag =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navControler = navHostFrag.navController


    }

    override fun onResume() {
        super.onResume()

        if (auth.currentUser != null) {
            firestore.collection("Users").document(Utils.getUiLoggedIn())
                .update("status", "Active now")
        }
    }

    override fun onStop() {
        super.onStop()

        if (auth.currentUser != null) {
            firestore.collection("Users").document(Utils.getUiLoggedIn())
                .update("status", Utils.getTimeStamp())

            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val ChatFromHome = navHostFragment?.childFragmentManager?.fragments?.find {
                it is ChatFromHome
            } as? ChatFromHome

            val ChatFragment = navHostFragment?.childFragmentManager?.fragments?.find {
                it is ChatFragment
            } as? ChatFragment

            if (ChatFromHome != null) {
                ChatFromHome.stopvideo()
            }
            if (ChatFragment != null) {
                ChatFragment.stopvideo()
            }
        }
    }


    override fun onStart() {
        super.onStart()

        if (auth.currentUser != null) {


            firestore.collection("Users").document(Utils.getUiLoggedIn())
                .update("status", "Active now")


        }
    }



    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment)
        val currentDest = navController.currentDestination?.id

        when (currentDest) {
            R.id.homeFragment -> {
                moveTaskToBack(true)
                finishAffinity()
            }

            R.id.chatFragment -> {
                firestore.collection("Users")
                    .document(Utils.getUiLoggedIn())
                    .update("chattingWith", "")
                super.onBackPressed()
            }

            R.id.chatFromHome -> {
                firestore.collection("Users")
                    .document(Utils.getUiLoggedIn())
                    .update("chattingWith", "")
                super.onBackPressed()
            }

            else -> {
                super.onBackPressed()
            }
        }
    }

}
