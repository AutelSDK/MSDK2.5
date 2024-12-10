package com.autel.sdk.debugtools.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.activity.FragmentPageInfoItem
import com.autel.sdk.debugtools.beans.ChildItem
import com.autel.sdk.debugtools.beans.ParentItem

/**
 * item list with title and description adapter
 * Copyright: Autel Robotics
 * @author huangsihua on 2022/12/17.
 */
class MainFragmentListAdapter(private val onClick: (Any) -> Unit) :
    ListAdapter<FragmentPageInfoItem, RecyclerView.ViewHolder>(DiffCallback) {

    private val VIEW_PARENT: Int = R.layout.frag_main_item //父级布局
    private val VIEW_CHILD: Int = R.layout.frag_main_item_child //子级布局

    private val objects: MutableList<Any> = ArrayList()
    var fragmentList: List<FragmentPageInfoItem>?= null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var holder : RecyclerView.ViewHolder ?= null

        if (viewType == VIEW_PARENT) {
            val view = LayoutInflater.from(parent.context).inflate(VIEW_PARENT, parent, false)
            holder = ParentViewHolder(view, onClick)
        } else if (viewType == VIEW_CHILD) {
            val view = LayoutInflater.from(parent.context).inflate(VIEW_CHILD, parent, false)
            holder = ChildViewHolder(view, onClick)
        }

        return holder!!
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ParentViewHolder) {
            var parentFragment = objects[position] as FragmentPageInfoItem
            holder.bind(parentFragment)
        } else if (holder is ChildViewHolder) {
            val childFragment = objects[position] as FragmentPageInfoItem.InnerFragmentPageInfoItem
            holder.bind(childFragment)
        }
    }

    override fun getItemCount(): Int {
        if (fragmentList == null) {
            return 0;
        }

        objects.clear()
        var size = fragmentList?.size
        for (i in fragmentList!!.indices) {
            objects.add(fragmentList!![i])
            if (size != null) {
                if (fragmentList!![i].isExpand) {
                    size += fragmentList!![i].fragmentInfoList!!.size
                    objects.addAll(fragmentList!![i].fragmentInfoList!!)

                    for (j in fragmentList!![i].fragmentInfoList!!) {
                        j.parentId = fragmentList!![i].id
                    }
                }
            }
        }
        return size!!
    }

    inner class ParentViewHolder(itemView: View, val onClick: (FragmentPageInfoItem) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.item_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.item_description)
        private var currentPageInfoItem: FragmentPageInfoItem? = null

        init {
            itemView.setOnClickListener {
                if (currentPageInfoItem!!.fragmentInfoList == null) {
                    onClick(currentPageInfoItem!!)
                    return@setOnClickListener
                }
                currentPageInfoItem!!.isExpand = !(currentPageInfoItem!!.isExpand)
                notifyDataSetChanged()
            }
        }

        fun bind(pageInfo: FragmentPageInfoItem) {
            currentPageInfoItem = pageInfo
            titleTextView.text = itemView.context.resources.getString(pageInfo.title)
            descriptionTextView.text = itemView.context.resources.getString(pageInfo.description)


        }
    }

    inner class ChildViewHolder(itemView: View, val onClick: (FragmentPageInfoItem.InnerFragmentPageInfoItem) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.item_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.item_description)
        private var currentPageInfoItem: FragmentPageInfoItem.InnerFragmentPageInfoItem? = null

        init {
            itemView.setOnClickListener {
                onClick(currentPageInfoItem!!)
            }
        }

        fun bind(pageInfo: FragmentPageInfoItem.InnerFragmentPageInfoItem) {
            currentPageInfoItem = pageInfo
            titleTextView.text = itemView.context.resources.getString(pageInfo.title)
            descriptionTextView.text = itemView.context.resources.getString(pageInfo.description)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<FragmentPageInfoItem>() {
        override fun areItemsTheSame(
            oldItem: FragmentPageInfoItem,
            newItem: FragmentPageInfoItem
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: FragmentPageInfoItem,
            newItem: FragmentPageInfoItem
        ): Boolean {
            return oldItem.title == newItem.title
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (objects[position] is ParentItem) {
            return VIEW_PARENT;
        } else if (objects[position] is ChildItem) {
            return VIEW_CHILD
        }
        return 0
    }
}