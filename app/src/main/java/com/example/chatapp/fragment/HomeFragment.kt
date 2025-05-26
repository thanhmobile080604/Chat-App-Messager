package com.example.chatapp.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.Utils
import com.example.chatapp.Utils.Companion.vibrate
import com.example.chatapp.activity.SignInActivity
import com.example.chatapp.adapter.OnUserClickListener
import com.example.chatapp.adapter.RecentChatAdapter
import com.example.chatapp.adapter.UserAdapter
import com.example.chatapp.adapter.onRecentChatClickListener
import com.example.chatapp.adapter.onRecentChatLongClickListener
import com.example.chatapp.databinding.FragmentHomeBinding
import com.example.chatapp.model.RecentChats
import com.example.chatapp.model.Users
import com.example.chatapp.mvvm.ChatAppviewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView


class HomeFragment : Fragment(), OnUserClickListener, onRecentChatClickListener,
    onRecentChatLongClickListener {

    lateinit var rvUser: RecyclerView
    lateinit var usersadapter: UserAdapter
    lateinit var userViewModel: ChatAppviewModel
    lateinit var homebinding: FragmentHomeBinding
    lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    lateinit var toolbar: Toolbar
    lateinit var circleImageView: CircleImageView
    lateinit var rvRecentChat: RecyclerView
    lateinit var recentChatAdapter: RecentChatAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homebinding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        return homebinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(this).get(ChatAppviewModel::class.java)
        auth = FirebaseAuth.getInstance()
        toolbar = view.findViewById(R.id.toolbarMain)
        circleImageView = view.findViewById(R.id.tlImage)
        firestore = FirebaseFirestore.getInstance()




        homebinding.swipeRefreshLayout.setOnRefreshListener {
            homebinding.swipeRefreshLayout.postDelayed({
                homebinding.swipeRefreshLayout.isRefreshing = false
            }, 400)
        }

        homebinding.logOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("LOG OUT")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("OK") { _, _ ->
                    firestore.collection("Users").document(Utils.getUiLoggedIn())
                        .update("status", Utils.getTimeStamp())
                    auth.signOut()
                    startActivity(Intent(requireContext(), SignInActivity::class.java))
                    requireActivity().overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        userViewModel.imageURL.observe(viewLifecycleOwner, Observer {
            Glide.with(requireContext()).load(it).into(circleImageView)
        })

        circleImageView.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToSettingFragment()
            view.findNavController().navigate(action)

        }


        homebinding.lifecycleOwner = viewLifecycleOwner

        //
        usersadapter = UserAdapter()
        rvUser = view.findViewById(R.id.rvUsers)

        val layoutManager1 = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        rvUser.layoutManager = layoutManager1

        userViewModel.getUsers().observe(viewLifecycleOwner, Observer {

            usersadapter.setUserList(it)
            rvUser.adapter = usersadapter
        })

        usersadapter.setOnUserClickListener(this)


        //
        recentChatAdapter = RecentChatAdapter()
        rvRecentChat = view.findViewById(R.id.rvRecentChats)


        val layoutManager2 = LinearLayoutManager(activity)
        rvRecentChat.layoutManager = layoutManager2


        userViewModel.getRecentChatList().observe(viewLifecycleOwner, Observer {

            recentChatAdapter.setRecentChatList(it)
            rvRecentChat.adapter = recentChatAdapter
        })

        recentChatAdapter.setOnRecentChatClickListener(this)
        recentChatAdapter.setOnRecentChatLongClickListener(this)


    }

    override fun onUserSelected(position: Int, users: Users) {

        val action = HomeFragmentDirections.actionHomeFragmentToChatFragment(users)
        view?.findNavController()?.navigate(action)

    }


    override fun onRecentChatSelected(position: Int, recentChats: RecentChats) {

        val action = HomeFragmentDirections.actionHomeFragmentToChatFromHome(recentChats)
        view?.findNavController()?.navigate(action)

    }

    override fun onRecentChatLongSelected(position: Int, recentChats: RecentChats) {
        val options = arrayOf("Archive")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select an option")
        builder.setCancelable(true)
        vibrate(requireContext())

        builder.setItems(options) { dialog, item ->
            when (options[item]) {
                "Archive" -> Utils.Archive(recentChats)
            }
        }

        builder.show()

    }
}
