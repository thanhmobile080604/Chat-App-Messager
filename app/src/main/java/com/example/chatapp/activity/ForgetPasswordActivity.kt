package com.example.chatapp.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatapp.R
import com.example.chatapp.databinding.ActivityForgetPasswordBinding
import com.example.chatapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("StaticFieldLeak")
private lateinit var forgetPasswordBinding: ActivityForgetPasswordBinding

class ForgetPasswordActivity : AppCompatActivity() {
    private lateinit var email1: String
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var emailRegistered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        forgetPasswordBinding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(forgetPasswordBinding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val checkedOTP = intent.getBooleanExtra("otp_verified", false)
        val email2 = intent.getStringExtra("email").toString() ?: ""

        forgetPasswordBinding.Backbutton.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }

        if (checkedOTP) {
            forgetPasswordBinding.EmailForgetPassword.visibility = View.GONE
            forgetPasswordBinding.titleForgetPassword.visibility = View.GONE
            forgetPasswordBinding.titleCheckyourInbox.visibility = View.VISIBLE
            forgetPasswordBinding.titleNotifi.visibility = View.VISIBLE
            forgetPasswordBinding.forgetPasswordButton.setText("OK")
            forgetPasswordBinding.Backbutton.visibility = View.GONE
            forgetPasswordBinding.imageViewForgetPassword.setImageResource(R.drawable.ok_icon)
            sendResetPasswordEmail(email2)
        }

        forgetPasswordBinding.forgetPasswordButton.setOnClickListener {
            forgetPasswordBinding.nextProgressBar.visibility = View.VISIBLE
            forgetPasswordBinding.forgetPasswordButton.visibility = View.GONE
            if(!checkedOTP) {
                email1 = forgetPasswordBinding.EmailForgetPassword.text.toString() ?: ""
                checkEmailBeforeAnything(email1)
                forgetPasswordBinding.nextProgressBar.visibility = View.GONE
                forgetPasswordBinding.forgetPasswordButton.visibility = View.VISIBLE
                if (emailRegistered) {
                    val intent = Intent(this, OTPActivity::class.java)
                    intent.putExtra("email", email1)
                    intent.putExtra("source", "forget_password")
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            else{
                startActivity(Intent(this, SignInActivity::class.java))
                overridePendingTransition(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                finish()
            }
        }


    }


    private fun checkEmailBeforeAnything(email: String) {
        if (email.equals("")) {
            forgetPasswordBinding.EmailForgetPassword.error = "Hãy nhập email"
            forgetPasswordBinding.editTextEmailInputLayout.isEndIconVisible = false
            forgetPasswordBinding.nextProgressBar.visibility = View.GONE
            forgetPasswordBinding.forgetPasswordButton.visibility = View.VISIBLE
        } else {
            firestore.collection("Users").whereEqualTo("useremail", email).get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        forgetPasswordBinding.EmailForgetPassword.error = "Hãy nhập email khác"
                        Toast.makeText(this, "Email chưa được đăng ký", Toast.LENGTH_SHORT).show()
                        forgetPasswordBinding.editTextEmailInputLayout.isEndIconVisible = false
                        forgetPasswordBinding.nextProgressBar.visibility = View.GONE
                        forgetPasswordBinding.forgetPasswordButton.visibility = View.VISIBLE
                    } else {
                        emailRegistered = true
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi kiểm tra email", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun sendResetPasswordEmail(email2: String) {
        forgetPasswordBinding.nextProgressBar.visibility = View.GONE
        forgetPasswordBinding.forgetPasswordButton.visibility = View.VISIBLE
        FirebaseAuth.getInstance().sendPasswordResetEmail(email2)
            .addOnCompleteListener {}
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()

    }
}