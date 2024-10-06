package com.autel.sdk.debugtools.adapter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelRemoteDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.sdk.debugtools.R
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.sdk.debugtools.databinding.ItemNetMeshDemoBinding


class NetMeshDemoAdapter : RecyclerView.Adapter<NetMeshDemoAdapter.ViewHolder>() {
    var dataList: MutableList<IBaseDevice>? = null
    var deviceIdList: MutableList<Int>? = mutableListOf()


    var onItemClick : ((Int) -> Unit)? = null


    fun setOnClickListener(listener: ((Int) -> Unit)? ){
        onItemClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemNetMeshDemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        dataList?.let {
            holder.bindData(it[position], position)

        }
    }

    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }

    inner class ViewHolder(val binding: ItemNetMeshDemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(device: IBaseDevice, position: Int) {
            val context = binding.root.context
            if(device is IAutelDroneDevice) {
                binding.tvIpName.text = context.getString(R.string.drone_info, device.getDeviceNumber(), device.getDeviceInfoBean()?.deviceName)
                binding.tvValue.text = context.getString(R.string.drone_status, device.isCenter(), device.isControlled(), device.isWatched(), device.isConnected())
                binding.tvType.text = ""
                binding.tvControled.text = ""
            } else if(device is IAutelRemoteDevice) {
                binding.tvIpName.text = context.getString(R.string.remote_info, device.getDeviceNumber(), device.getDeviceInfoBean()?.deviceName)
                binding.tvValue.text = context.getString(R.string.remote_status, device.getDeviceInfoBean()?.isMainRc, device.getDeviceInfoBean()?.isLocalRc)
                binding.tvType.text = ""
                binding.tvControled.text = ""
            }
        }
    }
}


