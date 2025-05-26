package com.example.chatapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.provider.OpenableColumns
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat.getSystemService
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.example.chatapp.model.RecentChats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.internal.wait
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class Utils {
    companion object {
        private val auth = FirebaseAuth.getInstance()

        @SuppressLint("StaticFieldLeak")
        private val firestore = FirebaseFirestore.getInstance()
        private var userid: String = ""
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
        const val REQUEST_VIDEO_PICK = 3
        const val REQUEST_VIDEO_CAPTURE = 4
        const val REQUEST_FILE_PICK = 5

        private const val secretKey = "thereisnosecretinthisprojecthaha"


        fun getUiLoggedIn(): String {
            if (auth.currentUser != null) {
                userid = auth.currentUser!!.uid
            }
            return userid
        }

        fun listenToUserStatus(userId: String, callback: (String) -> Unit): ListenerRegistration {
            return firestore.collection("Users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ChatFragment", "Listen to status failed", error)
                        return@addSnapshotListener
                    }

                    snapshot?.getString("status")?.let { status ->
                        callback(status)
                    }
                }
        }

        fun getTimeStamp(): String {
            val format = SimpleDateFormat("YYYY-MM-dd HH:mm:ss")
            val date = Date(System.currentTimeMillis())
            val stringdate = format.format(date)

            return stringdate
        }

        fun showFullImage(context: Context, imageUrl: String) {
            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.full_image)

            val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
            val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
            val btnDowload = dialog.findViewById<ImageView>(R.id.btnDowload)

            Glide.with(context)
                .load(imageUrl)
                .into(fullImageView)

            fullImageView.scaleX = 0.5f
            fullImageView.scaleY = 0.5f
            fullImageView.alpha = 0f

            fullImageView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .start()


            btnClose.setOnClickListener {
                fullImageView.animate()
                    .translationY(fullImageView.height.toFloat())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        dialog.dismiss()
                    }
                    .start()
            }

            btnDowload.setOnClickListener {
                Utils.downloadThing(context, imageUrl)
            }

            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    fullImageView.animate()
                        .translationY(fullImageView.height.toFloat())
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            dialog.dismiss()
                        }
                        .start()
                    true
                } else {
                    false
                }
            }
            dialog.show()
        }

        @OptIn(UnstableApi::class)
        fun showFullVideo(activity: Activity, message: String) {
            val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.full_video)

            val fullVideoView = dialog.findViewById<PlayerView>(R.id.fullvideoView)
            var exoPlayer: ExoPlayer? = null
            val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
            val btnDowload = dialog.findViewById<ImageView>(R.id.btnDowload)

            val player = ExoPlayer.Builder(activity).build()
            fullVideoView.player = player
            fullVideoView.controllerShowTimeoutMs = 0
            exoPlayer = player

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        fullVideoView.hideController()
                    } else {
                        fullVideoView.showController()
                    }
                }
            })

            val videoUri = Uri.parse(message)
            val mediaItem = MediaItem.fromUri(videoUri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            fullVideoView.scaleX = 0.5f
            fullVideoView.scaleY = 0.5f
            fullVideoView.alpha = 0f

            fullVideoView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .start()


            btnClose.setOnClickListener {
                exoPlayer.release()
                fullVideoView.animate()
                    .translationY(fullVideoView.height.toFloat())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        dialog.dismiss()
                    }
                    .start()
            }

            btnDowload.setOnClickListener {
                Utils.downloadThing(activity, message)
            }

            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    exoPlayer.release()
                    fullVideoView.animate()
                        .translationY(fullVideoView.height.toFloat())
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            dialog.dismiss()
                        }
                        .start()
                    true
                } else {
                    false
                }
            }
            dialog.show()

        }

        fun getFormatedDate(timestamp: String): String {
            if (timestamp!!.substring(1, 4) != Utils.getTimeStamp().substring(1, 4)) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm", Locale.getDefault())

                val date = inputFormat.parse(timestamp)
                val formattedDate = outputFormat.format(date)

                return formattedDate
            } else if (timestamp!!.substring(9, 10).toInt() + 1 == Utils.getTimeStamp()
                    .substring(9, 10)
                    .toInt()
            ) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("'Yesterday at' HH:mm", Locale.getDefault())

                val date = inputFormat.parse(timestamp)
                val formattedDate = outputFormat.format(date)

                return formattedDate
            } else if (timestamp!!.substring(6, 7) != Utils.getTimeStamp().substring(6, 7)
                || timestamp!!.substring(9, 10) != Utils.getTimeStamp().substring(9, 10)
            ) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM 'at' HH:mm", Locale.getDefault())

                val date = inputFormat.parse(timestamp)
                val formattedDate = outputFormat.format(date)

                return formattedDate
            } else
                return timestamp!!.substring(11, 16)

        }

        fun getFormattedDate(timestamp: String): String {
            val currentTimestamp = Utils.getTimeStamp()

            return try {
                if (timestamp.substring(1, 4) != currentTimestamp.substring(1, 4)) {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(timestamp)
                    outputFormat.format(date!!)
                } else if (timestamp.substring(9, 10).toInt() + 1 == currentTimestamp.substring(
                        9,
                        10
                    ).toInt()
                ) {
                    "Yesterday"
                } else if (
                    timestamp.substring(6, 7) != currentTimestamp.substring(6, 7) ||
                    timestamp.substring(9, 10) != currentTimestamp.substring(9, 10)
                ) {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                    val date = inputFormat.parse(timestamp)
                    outputFormat.format(date!!)
                } else {
                    timestamp.substring(11, 16)
                }
            } catch (e: Exception) {
                Log.e("DateFormat", "Error formatting date: ${e.message}")
                ""
            }
        }


        fun encrypt(strToEncrypt: String): String {
            val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted = cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encrypted, Base64.DEFAULT)
        }


        fun decrypt(strToDecrypt: String): String {
            val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(strToDecrypt, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decodedBytes)
            return String(decrypted, Charsets.UTF_8)
        }

        fun downloadThing(context: Context, Url: String) {
            val fileName = "${Url.takeLast(12)}.pdf"
            val downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadDir, fileName)

            if (file.exists()) {
                Toast.makeText(context, "This media is already saved", Toast.LENGTH_SHORT).show()
            } else {
                val request = DownloadManager.Request(Uri.parse(Url))
                    .setTitle(fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
                Toast.makeText(context, "Saved media", Toast.LENGTH_SHORT).show()
            }
        }


        fun vibrate(context: Context, durationMs: Long = 100L) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(durationMs)
            }
        }

        fun Archive(recentChats: RecentChats){
            firestore.collection("Conversations${Utils.getUiLoggedIn()}").document(recentChats.friendid!!).update("archive", true)
        }




    }

}