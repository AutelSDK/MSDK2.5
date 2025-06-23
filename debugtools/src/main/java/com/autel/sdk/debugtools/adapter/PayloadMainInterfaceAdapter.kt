/*
package com.autel.sdk.debugtools.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.vmodelx.module.payload.widget.PayloadWidget
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.view.PayloadWidgetView

*/
/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/17
 * 主界面
 *//*

class PayloadMainInterfaceAdapter(private val onItemClick: ((position: Int, widget: PayloadWidget) -> Unit)? = null) :
    ListAdapter<PayloadWidget, PayloadMainInterfaceAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): PayloadMainInterfaceAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_main_interface_list_item, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: PayloadMainInterfaceAdapter.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(position, item)
        }
        holder.bind(item)
    }

    class ViewHolder(itemView: View, val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val iconImage: PayloadWidgetView = itemView.findViewById(R.id.iv_widget_icon)
        private val iconDesc: TextView = itemView.findViewById(R.id.tv_widget_name)

        fun bind(item: PayloadWidget) {
            iconImage.setPayloadWidget(item)
            iconDesc.text = item.widgetName
        }
    }


    object DiffCallback : DiffUtil.ItemCallback<PayloadWidget>() {

        override fun areItemsTheSame(oldItem: PayloadWidget, newItem: PayloadWidget): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: PayloadWidget, newItem: PayloadWidget): Boolean {
            return oldItem.widgetType.equals(newItem.widgetType) && oldItem.widgetIndex == newItem.widgetIndex
        }

    }
}*/
