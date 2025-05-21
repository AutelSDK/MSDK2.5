package com.autel.sdk.debugtools.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autel.sdk.debugtools.R
import com.bumptech.glide.Glide


/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/14
 */
class PayloadWidgetIconAdapter :
    ListAdapter<PayloadWidgetItem, PayloadWidgetIconAdapter.ViewHolder>(DiffCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_payload_wdiget_item, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View, val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val iconImage: ImageView = itemView.findViewById(R.id.iv_widget_icon)
        private val iconDesc: TextView = itemView.findViewById(R.id.tv_widget_desc)

        fun bind(item: PayloadWidgetItem) {
            Glide.with(context).load(item.imgPath).placeholder(R.drawable.icon_item_checked)
                .into(iconImage)
            iconDesc.text = item.des
        }
    }


    object DiffCallback : DiffUtil.ItemCallback<PayloadWidgetItem>() {
        override fun areItemsTheSame(
            oldItem: PayloadWidgetItem,
            newItem: PayloadWidgetItem
        ): Boolean {
            return oldItem == newItem
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: PayloadWidgetItem,
            newItem: PayloadWidgetItem
        ): Boolean {
            return oldItem.des == newItem.des && oldItem.imgPath == newItem.imgPath
        }

    }
}