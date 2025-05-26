@file:Suppress("DEPRECATION")

package com.example.chatapp.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.chatapp.R
import com.example.chatapp.databinding.ActivitySignUpBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    private lateinit var name: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var signupauth: FirebaseAuth
    private lateinit var progressDialogSignUp: ProgressDialog
    private lateinit var signupBinding: ActivitySignUpBinding
    private var emailChecked: Boolean = false
    private var passwordChecked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signupBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        signupauth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialogSignUp = ProgressDialog(this)

        val passwordLayout = findViewById<TextInputLayout>(R.id.PasswordInputLayout)
        var isPasswordVisible = false

        passwordLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible

            val transformationMethod =
                if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()

            signupBinding.signUpPassword.transformationMethod = transformationMethod
            signupBinding.verifyPassword.transformationMethod = transformationMethod

            signupBinding.signUpPassword.setSelection(
                signupBinding.signUpPassword.text?.length ?: 0
            )
            signupBinding.verifyPassword.setSelection(
                signupBinding.verifyPassword.text?.length ?: 0
            )
        }

        signupBinding.signUpTextToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)

        }


        signupBinding.signUpBtn.setOnClickListener {
            signupBinding.signupProgressBar.visibility = View.VISIBLE
            signupBinding.signUpBtn.visibility = View.GONE

            name = signupBinding.signUpEtName.text.toString()
            email = signupBinding.signUpEmail.text.toString()
            password = signupBinding.signUpPassword.text.toString()

            if (signupBinding.signUpEtName.text.isEmpty()) {
                signupBinding.signUpEtName.error = "Hãy nhập tên"
                signupBinding.signupProgressBar.visibility = View.GONE
                signupBinding.signUpBtn.visibility = View.VISIBLE
                signupBinding.signUpEtName.setSelection(
                    signupBinding.signUpEtName.text?.length ?: 0
                )
                return@setOnClickListener
            }
            if (signupBinding.signUpEmail.text.isEmpty()) {
                signupBinding.signUpEmail.error = "Hãy nhập email"
                signupBinding.signUpEmail.setSelection(signupBinding.signUpEmail.text?.length ?: 0)
                signupBinding.signupProgressBar.visibility = View.GONE
                signupBinding.signUpBtn.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (!signupBinding.signUpEmail.text.toString().endsWith("@gmail.com")) {
                signupBinding.signUpEmail.error = "Hãy nhập đúng định dạng email"
                signupBinding.signUpEmail.setSelection(signupBinding.signUpEmail.text?.length ?: 0)
                signupBinding.signupProgressBar.visibility = View.GONE
                signupBinding.signUpBtn.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (signupBinding.signUpPassword.text.isEmpty()) {
                signupBinding.signUpPassword.error = "Hãy nhập mật khẩu"
                signupBinding.signupProgressBar.visibility = View.GONE
                signupBinding.signUpBtn.visibility = View.VISIBLE
                signupBinding.signUpPassword.setSelection(
                    signupBinding.signUpPassword.text?.length ?: 0
                )
                return@setOnClickListener
            }
            if (signupBinding.signUpPassword.text.toString() != signupBinding.verifyPassword.text.toString()) {
                signupBinding.verifyPassword.error = "Mật khẩu nhập lại bị sai"
                signupBinding.signupProgressBar.visibility = View.GONE
                signupBinding.signUpBtn.visibility = View.VISIBLE
                signupBinding.verifyPassword.setSelection(
                    signupBinding.verifyPassword.text?.length ?: 0
                )
                return@setOnClickListener
            }
            if (signupBinding.signUpEtName.text.isNotEmpty() &&
                signupBinding.signUpEmail.text.isNotEmpty() &&
                signupBinding.signUpPassword.text.isNotEmpty()
            ) {
                signupBinding.signupProgressBar.visibility = View.GONE
                signupBinding.signUpBtn.visibility = View.VISIBLE
                checkEmailBeforeSigningup(email)
                checkPasswordBeforeSigningup(password)

                if (emailChecked && passwordChecked) {
                    val intent = Intent(this, OTPActivity::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("password", password)
                    intent.putExtra("name", name)
                    intent.putExtra("source", "sign_up")
                    startActivity(intent)
                }
            }
        }
    }

    private fun checkEmailBeforeSigningup(email: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("Users").whereEqualTo("useremail", email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    emailChecked = true
                } else {
                    signupBinding.signUpEmail.error = "Hãy nhập email khác"
                    Toast.makeText(this, "Email đã được sử dụng", Toast.LENGTH_SHORT).show()
                    signupBinding.signupProgressBar.visibility = View.GONE
                    signupBinding.signUpBtn.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kiểm tra email", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPasswordBeforeSigningup(password: String) {
        if (password.length < 6) {
            signupBinding.PasswordInputLayout.isEndIconVisible = false
            signupBinding.signUpPassword.error = "Hãy nhập mật khẩu với ít nhất 6 kí tự"
            signupBinding.signupProgressBar.visibility = View.GONE
            signupBinding.signUpBtn.visibility = View.VISIBLE

            signupBinding.signUpPassword.setSelection(
                signupBinding.signUpPassword.text?.length ?: 0
            )

            signupBinding.signUpPassword.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty()) {
                        signupBinding.PasswordInputLayout.isEndIconVisible = true
                        signupBinding.signUpPassword.error = null
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
            Toast.makeText(this, "Mật khẩu cần ít nhất 6 kí tự", Toast.LENGTH_SHORT).show()
        } else {
            passwordChecked = true
        }
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
    }

}