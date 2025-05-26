package com.example.chatapp.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.Utils
import com.example.chatapp.activity.SignInActivity
import com.example.chatapp.databinding.FragmentSettingBinding
import com.example.chatapp.model.Users
import com.example.chatapp.mvvm.ChatAppviewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User


import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.util.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.launch


class SettingFragment : Fragment() {
    private lateinit var settingBinding: FragmentSettingBinding
    lateinit var settingviewModel: ChatAppviewModel
    lateinit var supabase: SupabaseClient
    private lateinit var backCallback: OnBackPressedCallback


    lateinit var bitmap: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Utils.getUiLoggedIn())
                    .get()
                    .addOnSuccessListener { document ->
                        val originName = document.getString("username")
                        val originImage = document.getString("imageUrl")
                        val currentName = settingBinding.settingUpdateName.text.toString()
                        val currentImage = settingviewModel.imageURL.value

                        val isChanged = currentName != originName || currentImage != originImage

                        if (isChanged) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("EXIT")
                                .setMessage("You have updated but not saved, are you sure you want to exit?")
                                .setPositiveButton("Yes") { _, _ ->
                                    backCallback.isEnabled = false
                                    requireActivity().onBackPressedDispatcher.onBackPressed()
                                }
                                .setNegativeButton("cancel", null)
                                .show()
                        } else {
                            backCallback.isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), backCallback)

        settingBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_setting, container, false)
        return settingBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //must add storage policy on the web, tick all the method CRUD, pick anon and authenticated and done, you good to go
        supabase = createSupabaseClient(
            supabaseUrl = "https://xyxegavczxubwuubjgsz.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh5eGVnYXZjenh1Ynd1dWJqZ3N6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDU3MjU0MDIsImV4cCI6MjA2MTMwMTQwMn0.cwTRlXRGVnB6AxfrQsEp_2U7LyFTblWXmzL5-axjVAE"
        ) {
            install(Postgrest)
            install(Storage)
        }


        settingviewModel = ViewModelProvider(this).get(ChatAppviewModel::class.java)
        settingBinding.viewModel = settingviewModel
        settingBinding.lifecycleOwner = viewLifecycleOwner

        settingviewModel.imageURL.observe(viewLifecycleOwner, Observer {
            Glide.with(requireContext())
            .load(it)
            .placeholder(R.drawable.person)
            .dontAnimate()
            .into(settingBinding.settingUpdateImage)
        })


        settingBinding.settingBackBtn.setOnClickListener {
            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Utils.getUiLoggedIn())
                .get()
                .addOnSuccessListener { document ->
                    val originName = document.getString("username")
                    val originImage = document.getString("imageUrl")
                    val currentName = settingBinding.settingUpdateName.text.toString()
                    val currentImage = settingviewModel.imageURL.value

                    val isChanged = currentName != originName || currentImage != originImage

                    if (isChanged) {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("EXIT")
                            .setMessage("You have updated but not saved, are you sure you want to exit?")
                            .setPositiveButton("Yes") { _, _ ->
                                findNavController().popBackStack()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    } else {
                        findNavController().popBackStack()
                    }
                }
        }





        settingBinding.settingUpdateImage.setOnClickListener {
            val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Choose your profile picture")
            builder.setItems(options) { dialog, item ->
                when {
                    options[item] == "Take Photo" -> takePhotoWithCamera()
                    options[item] == "Choose from Gallery" -> pickImageFromGallery()
                    options[item] == "Cancel" -> dialog.dismiss()
                }
            }
            builder.show()
        }

        settingBinding.settingUpdateButton.setOnClickListener {
            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Utils.getUiLoggedIn())
                .get()
                .addOnSuccessListener { document ->
                    val originName = document.getString("username")
                    val originImage = document.getString("imageUrl")
                    val currentName = settingBinding.settingUpdateName.text.toString()
                    val currentImage = settingviewModel.imageURL.value

                    val isChanged = currentName != originName || currentImage != originImage

                    if (isChanged) {
                        settingviewModel.updateProfile()
                    } else {
                        Toast.makeText(context, "You haven't changed anything", Toast.LENGTH_LONG).show()
                    }
                }
        }
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                Utils.REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    lifecycleScope.launch {
                        uploadImageToSupabaseStorage(imageBitmap)
                    }
                }

                Utils.REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    val imageBitmap =
                        MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri)
                    lifecycleScope.launch {
                        uploadImageToSupabaseStorage(imageBitmap)
                    }
                }
            }
        }
    }

    private suspend fun uploadImageToSupabaseStorage(imageBitmap: Bitmap?) {
        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        bitmap = imageBitmap!!
        settingBinding.settingUpdateImage.setImageBitmap(imageBitmap)

        val fileName = "${UUID.randomUUID()}.jpg"
        val bucket = supabase.storage["chatapp0806photo"]

        try {
            bucket.upload(
                path = fileName,
                data = data,
                upsert = true
            )

            val publicUrl = bucket.publicUrl(fileName)
            settingviewModel.imageURL.value = publicUrl

            Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onResume() {
        super.onResume()
        settingviewModel.imageURL.observe(viewLifecycleOwner, Observer {
            Glide.with(requireContext())
                .load(it)
                .placeholder(R.drawable.person)
                .dontAnimate()
                .into(settingBinding.settingUpdateImage)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backCallback.remove() // Dọn callback tránh leak
    }



}


