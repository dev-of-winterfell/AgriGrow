package com.example.agrigrow

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MediaAdapter(private val context: Context, private val mediaList: List<Uri>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        // Assuming the last item in the list is a video, check if the Uri is a video
        return if (mediaList[position].toString().endsWith(".mp4")) TYPE_VIDEO else TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_IMAGE) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
            ImageViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false)
            VideoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mediaUri = mediaList[position]

        if (holder is ImageViewHolder) {
            // Load image using Glide
            Glide.with(context)
                .load(mediaUri)
                .into(holder.imageView)

        } else if (holder is VideoViewHolder) {
            // Set video URI and start playing
            holder.videoView.setVideoURI(mediaUri)
            holder.videoView.setOnPreparedListener {
                it.isLooping = true
                holder.videoView.start()
            }
        }
    }

    override fun getItemCount(): Int = mediaList.size

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.mediaImageView)
    }

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoView: VideoView = view.findViewById(R.id.mediaVideoView)
    }
}
