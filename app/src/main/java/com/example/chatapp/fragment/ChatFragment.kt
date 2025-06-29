package com.example.chatapp.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.Utils
import com.example.chatapp.adapter.MessageAdapter
import com.example.chatapp.databinding.FragmentChatBinding
import com.example.chatapp.model.Message
import com.example.chatapp.mvvm.ChatAppviewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID


@Suppress("DEPRECATION")
class ChatFragment : Fragment() {

    private lateinit var args: ChatFragmentArgs
    private lateinit var chatBinding: FragmentChatBinding
    private lateinit var chatAppviewModel: ChatAppviewModel
    private lateinit var toolbar: Toolbar
    lateinit var supabase: SupabaseClient
    lateinit var bitmap: Bitmap
    private lateinit var messageAdapter: MessageAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var statusListener: ListenerRegistration? = null
    private lateinit var publicUrl: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        chatBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        return chatBinding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        supabase = createSupabaseClient(
            supabaseUrl = "https://xyxegavczxubwuubjgsz.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh5eGVnYXZjenh1Ynd1dWJqZ3N6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDU3MjU0MDIsImV4cCI6MjA2MTMwMTQwMn0.cwTRlXRGVnB6AxfrQsEp_2U7LyFTblWXmzL5-axjVAE"
        ) {
            install(Postgrest)
            install(Storage)
        }
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        toolbar = view.findViewById(R.id.toolBarChat)
        val circleImageView = toolbar.findViewById<CircleImageView>(R.id.chatImageViewUser)
        val textViewName = toolbar.findViewById<TextView>(R.id.chatUserName)
        val textViewStatus = view.findViewById<TextView>(R.id.chatUserStatus)
        val chatBackBtn = toolbar.findViewById<ImageView>(R.id.chatBackBtn)
        val statusOnline = toolbar.findViewById<ImageView>(R.id.statusOnline)
        val uploadImage = view.findViewById<ImageView>(R.id.upload_image)
        val friendImage = toolbar.findViewById<LinearLayout>(R.id.friendImage)
        val uploadFile = view.findViewById<ImageView>(R.id.upload_file)








        chatAppviewModel = ViewModelProvider(this).get(ChatAppviewModel::class.java)


        args = ChatFragmentArgs.fromBundle(requireArguments())

        chatBinding.viewModel = chatAppviewModel
        chatBinding.lifecycleOwner = viewLifecycleOwner



        Glide.with(view.context).load(args.users.imageUrl!!).placeholder(R.drawable.person)
            .dontAnimate().into(circleImageView);
        textViewName.text = args.users.username

        statusListener = Utils.listenToUserStatus(args.users.userid!!) { status ->
            if (status == "Active now") {
                textViewStatus.text = status
                statusOnline.setImageResource(R.drawable.onlinestatus)
            } else {
                if (Utils.getFormatedDate(status).length > 7 && Utils.getFormatedDate(status)
                        .first().isDigit()
                ) {
                    textViewStatus.text =
                        "Last active: ${(Utils.getFormatedDate(status)).dropLast(8)}"
                } else textViewStatus.text = "Last active: ${Utils.getFormatedDate(status)}"
                statusOnline.setImageResource(R.drawable.offlinestatus)
            }
        }



        chatBackBtn.setOnClickListener {
            firestore.collection("Users")
                .document(Utils.getUiLoggedIn())
                .update("chattingWith", "")
            findNavController().popBackStack()
        }

        friendImage.setOnClickListener {
            Utils.showFullImage(requireContext(), args.users.imageUrl!!)
            (chatBinding.messagesRecyclerView.adapter as? MessageAdapter)?.ReleasingPlayer()
        }


        chatBinding.sendBtn.setOnClickListener {
            if (chatBinding.editTextMessage.text.toString().isEmpty()) {
                return@setOnClickListener
            }
            chatAppviewModel.sendMessage(
                Utils.getUiLoggedIn(),
                args.users.userid!!,
                args.users.username!!,
                args.users.imageUrl!!
            )
        }

        uploadImage.setOnClickListener {
            val options = arrayOf<CharSequence>(
                "Take Photo",
                "Take Video",
                "Choose image from Gallery",
                "Choose video from Gallery",
                "Cancel"
            )
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Choose your media")
            builder.setItems(options) { dialog, item ->
                when {
                    options[item] == "Take Photo" -> takePhotoWithCamera()
                    options[item] == "Take Video" -> takeVideoWithCamera()
                    options[item] == "Choose image from Gallery" -> pickImageFromGallery()
                    options[item] == "Choose video from Gallery" -> pickVideoFromGallery()
                    options[item] == "Cancel" -> dialog.dismiss()
                }
            }
            builder.show()
        }

        uploadFile.setOnClickListener {
            val pickFileIntent =
                Intent(Intent.ACTION_GET_CONTENT)
            pickFileIntent.type = "*/*"
            pickFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
            pickFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf"))
            if (pickFileIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(pickFileIntent, Utils.REQUEST_FILE_PICK)
            }
        }


        chatAppviewModel.getMessage(args.users.userid!!)
            .observe(viewLifecycleOwner) { messages ->
                initRecyclerView(messages)
                chatAppviewModel.seenMessage(args.users.userid!!) { success, message ->
                    if (success) {
                        Log.d("ChatFragment", message)
                    } else {
                        Log.e("ChatFragment", message)
                    }
                }
            }


    }


    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView(it: List<Message>) {
        messageAdapter = MessageAdapter()
        val layoutManager = LinearLayoutManager(context)
        chatBinding.messagesRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true
        messageAdapter.setMessageList(it)
        messageAdapter.notifyDataSetChanged()
        messageAdapter.setImageUrl(args.users.imageUrl!!)
        chatBinding.messagesRecyclerView.adapter = messageAdapter

    }

    override fun onDestroyView() {
        super.onDestroyView()

        statusListener?.remove()
        statusListener = null
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun pickImageFromGallery() {
        val pickPictureIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (pickPictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(pickPictureIntent, Utils.REQUEST_IMAGE_PICK)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePhotoWithCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, Utils.REQUEST_IMAGE_CAPTURE)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun pickVideoFromGallery() {
        val pickVideoIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        if (pickVideoIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(pickVideoIntent, Utils.REQUEST_VIDEO_PICK)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takeVideoWithCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(takePictureIntent, Utils.REQUEST_VIDEO_CAPTURE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                Utils.REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    lifecycleScope.launch {
                        uploadImageToSupabaseStorageAndFirebase(imageBitmap)
                    }
                }

                Utils.REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    val imageBitmap =
                        MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri)
                    lifecycleScope.launch {
                        uploadImageToSupabaseStorageAndFirebase(imageBitmap)
                    }
                }

                Utils.REQUEST_VIDEO_CAPTURE -> {
                    val videoUri = data?.data
                    lifecycleScope.launch {
                        if (videoUri != null) {
                            uploadVideoToSupabaseStorageAndFirebase(videoUri)
                        }
                    }
                }

                Utils.REQUEST_VIDEO_PICK -> {
                    val videoUri = data?.data
                    lifecycleScope.launch {
                        if (videoUri != null) {
                            uploadVideoToSupabaseStorageAndFirebase(videoUri)
                        }
                    }
                }

                Utils.REQUEST_FILE_PICK -> {
                    val fileUri = data?.data
                    lifecycleScope.launch {
                        if (fileUri != null) {
                            uploadFileToSupabaseStorageAndFirebase(fileUri)
                        }
                    }
                }
            }
        }
    }

    private suspend fun uploadFileToSupabaseStorageAndFirebase(fileUri: Uri) {
        val fileName = "${UUID.randomUUID()}.pdf"
        val data = context?.contentResolver?.openInputStream(fileUri)?.readBytes() ?: return

        val bucket = supabase.storage["chatapp0806photo"]

        try {
            bucket.upload(
                path = fileName,
                data = data,
                upsert = true
            )

            publicUrl = bucket.publicUrl(fileName)
            chatAppviewModel.sendMedia(
                Utils.getUiLoggedIn(),
                args.users.userid!!,
                args.users.username!!,
                args.users.imageUrl!!,
                publicUrl
            )


        } catch (e: Exception) {
            Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun uploadVideoToSupabaseStorageAndFirebase(videoUri: Uri) {
        val inputStream = context?.contentResolver?.openInputStream(videoUri)
        val videoData = inputStream?.readBytes() ?: return

        val fileName = "${UUID.randomUUID()}.mp4"
        val bucket = supabase.storage["chatapp0806photo"]

        try {
            bucket.upload(
                path = fileName,
                data = videoData,
                upsert = true
            )


            publicUrl = bucket.publicUrl(fileName)
            chatAppviewModel.sendMedia(
                Utils.getUiLoggedIn(),
                args.users.userid!!,
                args.users.username!!,
                args.users.imageUrl!!,
                publicUrl
            )


        } catch (e: Exception) {
            Toast.makeText(context, "Failed to upload video: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }

    }

    private suspend fun uploadImageToSupabaseStorageAndFirebase(imageBitmap: Bitmap?) {
        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        bitmap = imageBitmap!!
        //settingBinding.settingUpdateImage.setImageBitmap(imageBitmap)

        val fileName = "${UUID.randomUUID()}.jpg"
        val bucket = supabase.storage["chatapp0806photo"]

        try {
            bucket.upload(
                path = fileName,
                data = data,
                upsert = true
            )

            publicUrl = bucket.publicUrl(fileName)
            //settingviewModel.imageURL.value = publicUrl
            chatAppviewModel.sendMedia(
                Utils.getUiLoggedIn(),
                args.users.userid!!,
                args.users.username!!,
                args.users.imageUrl!!,
                publicUrl
            )


        } catch (e: Exception) {
            Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }

    }

    override fun onPause() {
        super.onPause()
        (chatBinding.messagesRecyclerView.adapter as? MessageAdapter)?.ReleasingPlayer()
    }



}
