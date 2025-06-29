package com.example.chatapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.R
import com.example.chatapp.databinding.ActivityOtpactivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import papaya.`in`.sendmail.SendMail
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

private lateinit var OTPbinding: ActivityOtpactivityBinding

class OTPActivity : AppCompatActivity() {
    private lateinit var name: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var source: String
    private var timeoutSeconds: Long = 60L
    private var random: Int = 0
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OTPbinding = ActivityOtpactivityBinding.inflate(layoutInflater)
        setContentView(OTPbinding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        email = intent.getStringExtra("email").toString() ?: ""
        password = intent.getStringExtra("password").toString() ?: ""
        name = intent.getStringExtra("name").toString() ?: ""
        source = intent.getStringExtra("source").toString() ?: ""

        OTPbinding.resendOtpTextview.setOnClickListener {
            random()
            startResendTimer()
        }

        when (source) {
            "sign_up" -> {
                random()
                startResendTimer()
                OTPbinding.loginProgressBar.visibility = View.GONE

                OTPbinding.loginNextBtn.setOnClickListener {
                    OTPbinding.loginProgressBar.visibility = View.VISIBLE
                    OTPbinding.loginNextBtn.visibility = View.GONE

                    if (OTPbinding.loginOtp.text.isEmpty()) {
                        OTPbinding.loginOtp.error = "Vui lòng nhập OTP"
                        OTPbinding.loginProgressBar.visibility = View.GONE
                        OTPbinding.loginNextBtn.visibility = View.VISIBLE
                        return@setOnClickListener
                    } else if (!OTPbinding.loginOtp.text.toString().equals(random.toString())) {
                        OTPbinding.loginOtp.error = "Nhập sai OTP"
                        OTPbinding.loginProgressBar.visibility = View.GONE
                        OTPbinding.loginNextBtn.visibility = View.VISIBLE
                        return@setOnClickListener
                    } else {
                        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                            if (it.isSuccessful) {
                                val user = auth.currentUser
                                val userHashMap = hashMapOf(
                                    "userid" to user!!.uid!!,
                                    "username" to name,
                                    "useremail" to email,
                                    "status" to "Offline",
                                    "imageUrl" to "https://cdn-icons-png.flaticon.com/512/4128/4128244.png"
                                )
                                firestore.collection("Users").document(user.uid).set(userHashMap)
                                 val intent = Intent(this, SignInActivity::class.java)
                                intent.putExtra("Just signed up", true)
                                startActivity(intent)
                                Toast.makeText(
                                    this@OTPActivity, "Đăng ký thành công", Toast.LENGTH_LONG
                                )
                                overridePendingTransition(
                                    R.anim.slide_in_left, R.anim.slide_out_right
                                )
                                finish()
                            } else {
                                Toast.makeText(this, "Lỗi", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi Firestore: ${e.message}", Toast.LENGTH_LONG)
                                .show()
                            finish()
                        }
                    }
                }

                OTPbinding.Backbutton.setOnClickListener {
                    startActivity(Intent(this, SignUpActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }


            }

            "forget_password" -> {
                random()
                startResendTimer()
                OTPbinding.loginProgressBar.visibility = View.GONE

                OTPbinding.loginNextBtn.setOnClickListener {
                    OTPbinding.loginProgressBar.visibility = View.VISIBLE
                    OTPbinding.loginNextBtn.visibility = View.GONE

                    if (OTPbinding.loginOtp.text.isEmpty()) {
                        OTPbinding.loginOtp.error = "Vui lòng nhập OTP"
                        OTPbinding.editTextOTPInputLayout.isEndIconVisible = false
                        OTPbinding.loginProgressBar.visibility = View.GONE
                        OTPbinding.loginNextBtn.visibility = View.VISIBLE
                        return@setOnClickListener
                    } else if (!OTPbinding.loginOtp.text.toString().equals(random.toString())) {
                        OTPbinding.loginOtp.error = "Nhập sai OTP"
                        OTPbinding.editTextOTPInputLayout.isEndIconVisible = false
                        OTPbinding.loginProgressBar.visibility = View.GONE
                        OTPbinding.loginNextBtn.visibility = View.VISIBLE
                        return@setOnClickListener
                    } else {
                        val intent = Intent(this, ForgetPasswordActivity::class.java)
                        intent.putExtra("otp_verified", true)
                        intent.putExtra("email", email)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }


                }

                OTPbinding.Backbutton.setOnClickListener {
                    startActivity(Intent(this, ForgetPasswordActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }


            }
        }
    }

    private fun startResendTimer() {
        OTPbinding.resendOtpTextview.isEnabled = false
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timeoutSeconds--
                runOnUiThread {
                    OTPbinding.resendOtpTextview.text = "Resend OTP in $timeoutSeconds seconds"
                }
                if (timeoutSeconds <= 0) {
                    timeoutSeconds = 60L
                    timer.cancel()
                    runOnUiThread {
                        OTPbinding.resendOtpTextview.isEnabled = true
                        OTPbinding.resendOtpTextview.text = "Resend OTP"
                    }
                }
            }
        }, 0, 1000)
    }

    private fun random() {
        random = Random.nextInt(100000, 999999 + 1)
        var mail = SendMail(
            "thanh08062004@gmail.com",
            "dbipfpjbbbhxgvfa",
            email,
            "Login Signup app's OTP",
            "Your OTP is -> $random"
        )
        mail.execute()
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
