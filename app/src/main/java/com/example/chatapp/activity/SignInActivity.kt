@file:Suppress("DEPRECATION")

package com.example.chatapp.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.Utils
import com.example.chatapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var email : String
    private lateinit var password : String
    private lateinit var signinauth : FirebaseAuth
    private lateinit var progressDialogSignIn: ProgressDialog
    private lateinit var signInBinding : ActivitySignInBinding
    private lateinit var firestore: FirebaseFirestore



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
        signinauth = FirebaseAuth.getInstance()


        if(signinauth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
        }

        progressDialogSignIn = ProgressDialog(this)
        signInBinding.signInTextToSignUp.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        }

        signInBinding.forgetPassword.setOnClickListener{
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }



        signInBinding.loginButton.setOnClickListener{
            signInBinding.signinProgressBar.visibility = View.VISIBLE
            signInBinding.loginButton.visibility = View.GONE

            email = signInBinding.loginetemail.text.toString()
            password = signInBinding.loginetpassword.text.toString()

            if(signInBinding.loginetemail.text.isEmpty()){
                signInBinding.loginetemail.error = "Hãy nhập email"
                signInBinding.signinProgressBar.visibility = View.GONE
                signInBinding.loginButton.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if(signInBinding.loginetpassword.text.isEmpty()){
                signInBinding.editTextPasswordInputLayout.isEndIconVisible = false
                signInBinding.loginetpassword.error = "Hãy nhập password"
                signInBinding.signinProgressBar.visibility = View.GONE
                signInBinding.loginButton.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if(signInBinding.loginetemail.text.isNotEmpty()&&signInBinding.loginetpassword.text.isNotEmpty()){
                checkUserBeforeSigningin(email,password)
            }
            
        }
    }

    private fun checkUserBeforeSigningin(email: String, password: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("Users").whereEqualTo("useremail", email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    progressDialogSignIn.dismiss()
                    Toast.makeText(this, "Email chưa được đăng ký", Toast.LENGTH_SHORT).show()
                    signInBinding.loginetemail.error = "Email chưa được đăng ký"
                    signInBinding.signinProgressBar.visibility = View.GONE
                    signInBinding.loginButton.visibility = View.VISIBLE
                } else {
                    signin(email, password)
                }
            }
            .addOnFailureListener {
                progressDialogSignIn.dismiss()
                Toast.makeText(this, "Lỗi kiểm tra email", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signin(email: String, password: String) {
        progressDialogSignIn.setMessage("Đang đăng nhập...")
        progressDialogSignIn.setCanceledOnTouchOutside(false)
        progressDialogSignIn.show()

        signinauth.signInWithEmailAndPassword(email, password).addOnCompleteListener{
            if(it.isSuccessful){
                progressDialogSignIn.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
            }
        }.addOnFailureListener { exception ->
            progressDialogSignIn.dismiss()
            when (exception) {
                is FirebaseAuthInvalidCredentialsException -> {
                    Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show()
                    signInBinding.editTextPasswordInputLayout.isEndIconVisible = false
                    signInBinding.loginetpassword.error = "Sai mật khẩu"
                    signInBinding.signinProgressBar.visibility = View.GONE
                    signInBinding.loginButton.visibility = View.VISIBLE

                    signInBinding.loginetpassword.setSelection(
                        signInBinding.loginetpassword.text?.length ?: 0
                    )

                    signInBinding.loginetpassword.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            if (!s.isNullOrEmpty()) {
                                signInBinding.editTextPasswordInputLayout.isEndIconVisible = true
                                signInBinding.loginetpassword.error = null
                            }
                        }

                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                    }
                    )
                }
                else -> {
                    Toast.makeText(this, "Lỗi đăng nhập, thử lại sau", Toast.LENGTH_SHORT).show()
                }
            }

        }

    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        progressDialogSignIn.dismiss()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialogSignIn.dismiss()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

}