package com.example.chatapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.media.browse.MediaBrowser
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.OptIn
import androidx.cardview.widget.CardView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.chatapp.R
import com.example.chatapp.Utils

import com.example.chatapp.model.Message
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.internal.wait

class MessageAdapter : RecyclerView.Adapter<MessageHolder>() {

    private var listOfMessage = listOf<Message>()
    private val playerList = mutableListOf<ExoPlayer>()
    private var listener: onMessageClickListener? = null
    private var imageUrl: String? = null

    private var LEFT = 0
    private var RIGHT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == RIGHT) {
            val view = inflater.inflate(R.layout.chatitemright, parent, false)
            MessageHolder(view)
        } else {
            val view = inflater.inflate(R.layout.chatitemleft, parent, false)
            MessageHolder(view)
        }
    }


    override fun getItemCount(): Int {
        return listOfMessage.size
    }

    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val message = listOfMessage[position]

        val isImage = message.message.toString().endsWith(".jpg")
        val isVideo = message.message.toString().endsWith(".mp4")
        val isPDF = message.message.toString().endsWith(".pdf")
        val isLink = try {
            Utils.decrypt(message.message.toString()).startsWith("https://")
        } catch (e: Exception) {
            false
        }



        if (isImage) {
            holder.messageText.visibility = View.GONE
            holder.cardView.visibility = View.GONE
            holder.image.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(message.message)
                .into(holder.image)
        } else if (isVideo) {
            holder.messageText.visibility = View.GONE
            holder.image.visibility = View.GONE
            holder.cardView.visibility = View.VISIBLE

            val player = ExoPlayer.Builder(holder.itemView.context).build()
            holder.videoView.player = player
            holder.videoView.controllerShowTimeoutMs = 0
            holder.exoPlayer = player

            playerList.add(player)

            val videoUri = Uri.parse(message.message)
            val mediaItem = MediaItem.fromUri(videoUri)
            player.setMediaItem(mediaItem)
            player.prepare()

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        holder.videoView.hideController()
                    } else {
                        holder.videoView.showController()
                    }
                }
            })


        } else if (isPDF) {
            holder.image.visibility = View.GONE
            holder.messageText.visibility = View.VISIBLE
            holder.cardView.visibility = View.GONE

            val spannable = SpannableString(message.message!!.takeLast(20))

            spannable.setSpan(
                UnderlineSpan(),
                0,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            holder.messageText.text = spannable
            holder.messageText.setTypeface(null, Typeface.BOLD)

        } else if (isLink) {
            holder.messageText.visibility = View.VISIBLE
            holder.image.visibility = View.GONE
            holder.cardView.visibility = View.GONE

            val rawMessage = Utils.decrypt(message.message!!)
            val spannable = SpannableString(rawMessage)

            spannable.setSpan(
                UnderlineSpan(),
                0,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            holder.messageText.text = spannable
            holder.messageText.setTypeface(null, Typeface.BOLD)

        } else {
            holder.image.visibility = View.GONE
            holder.messageText.visibility = View.VISIBLE
            holder.cardView.visibility = View.GONE

            val rawMessage = Utils.decrypt(message.message!!)
            val formattedMessage = rawMessage.chunked(30).joinToString("\n")
            holder.messageText.text = formattedMessage
        }

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams

        if (showDateHeader(message, position)) {
            layoutParams.topMargin = 70.dpToPx(holder.itemView.context)
            holder.timeText.visibility = View.VISIBLE
            holder.timeText.text = message.timestamp?.let { Utils.getFormatedDate(it) }
        } else {
            layoutParams.topMargin = 0.dpToPx(holder.itemView.context)
            holder.timeText.visibility = if (message.isTimestampVisible) View.VISIBLE else View.GONE
        }
        holder.timeText.text = message.timestamp?.let { Utils.getFormatedDate(it) }

        if (getItemViewType(position) == LEFT) {
            holder.chatImage.visibility = View.VISIBLE
            if (position == itemCount - 1) {
                Glide.with(holder.itemView.context).load(imageUrl).placeholder(R.drawable.person)
                    .dontAnimate().into(holder.chatImage)
            } else {
                if (showImageProfile(listOfMessage[position + 1], position + 1)) {
                    Glide.with(holder.itemView.context).load(imageUrl)
                        .placeholder(R.drawable.person)
                        .dontAnimate().into(holder.chatImage)
                } else {
                    holder.chatImage.visibility = View.GONE
                }
            }
        } else {
            holder.chatImage.visibility = View.GONE
        }

        if (getItemViewType(position) == RIGHT) {
            holder.seen.visibility = View.VISIBLE
            if ((message.seen == true && position == itemCount - 1) || (message.seen == true && getItemViewType(
                    position + 1
                ) == RIGHT && listOfMessage[position + 1].seen == false)
            ) {
                holder.seen.text = "Seen"
            } else if (message.seen == false && position == itemCount - 1) {
                holder.seen.text = "Delivered"
            } else {
                holder.seen.visibility = View.GONE
            }
        } else {
            holder.seen.visibility = View.GONE
        }

        holder.image.setOnClickListener() {
            Utils.showFullImage(holder.itemView.context, message.message!!)
            ReleasingPlayer()
        }

        holder.chatImage.setOnClickListener {
            Utils.showFullImage(holder.itemView.context, imageUrl!!)
            ReleasingPlayer()
        }

        holder.videoView.setOnClickListener {
            ReleasingPlayer()
            (holder.cardView.context as? Activity)?.let {
                holder.exoPlayer?.seekTo(0)
                holder.exoPlayer?.playWhenReady = false
                Utils.showFullVideo(it, message.message!!)
            }

        }


        if (isPDF) {
            holder.messageText.setOnClickListener {
                Utils.downloadThing(holder.itemView.context, message.message!!)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.message))
                holder.itemView.context.startActivity(intent)
            }
        } else if (isLink) {
            holder.messageText.setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(Utils.decrypt(message.message.toString())))
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.messageText.setOnClickListener()
            {
                message.isTimestampVisible = !message.isTimestampVisible
                notifyItemChanged(position)
                listener?.onMessageSelected(position, message)
            }
        }


    }


    private fun Int.dpToPx(context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun showDateHeader(message: Message, position: Int): Boolean {
        val currentDate = message.timestamp!!.substring(0, 10)
        val currentHour = message.timestamp!!.substring(11, 13)
        val isImage = message.message.toString().endsWith(".jpg")
        val isVideo = message.message.toString().endsWith(".mp4")
        if (isImage || isVideo) return true
        return position == 0 || currentDate != listOfMessage[position - 1].timestamp!!.substring(
            0,
            10
        )
                || currentHour != listOfMessage[position - 1].timestamp!!.substring(11, 13)
    }

    private fun showImageProfile(message: Message, position: Int): Boolean {
        return getItemViewType(position) == RIGHT || showDateHeader(message, position)
    }


    override fun getItemViewType(position: Int): Int {
        return when {
            listOfMessage[position].sender == Utils.getUiLoggedIn() -> RIGHT

            else -> LEFT
        }

    }


    fun setMessageList(list: List<Message>) {
        this.listOfMessage = list
    }

    fun setImageUrl(url: String?) {
        this.imageUrl = url
        notifyDataSetChanged()
    }

    override fun onViewDetachedFromWindow(holder: MessageHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder.exoPlayer?.isPlaying == true) {
            holder.exoPlayer?.seekTo(0)
            holder.exoPlayer?.playWhenReady = false
        }
    }

    fun ReleasingPlayer() {
        for (player in playerList) {
            if (player.isPlaying) {
                player.seekTo(0)
                player.playWhenReady = false
            }
        }
    }

}


class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView.rootView) {
    val messageText: TextView = itemView.findViewById(R.id.show_message)
    val timeText: TextView = itemView.findViewById(R.id.timeView)
    val seen: TextView = itemView.findViewById(R.id.seen)
    val image: ImageView = itemView.findViewById(R.id.show_image)
    val chatImage: CircleImageView = itemView.findViewById(R.id.ChatImageView)
    val videoView: PlayerView = itemView.findViewById(R.id.videoView)
    val cardView: CardView = itemView.findViewById(R.id.cardviewvideo)
    var exoPlayer: ExoPlayer? = null

}

interface onMessageClickListener {
    fun onMessageSelected(position: Int, message: Message)
}



