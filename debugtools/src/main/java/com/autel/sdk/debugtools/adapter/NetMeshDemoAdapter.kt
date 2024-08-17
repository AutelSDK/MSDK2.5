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
            /*binding.cbCheck.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener{
                override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                    if(p1){
                        deviceIdList?.add(data.iDeviceID)
                    }else{
                        deviceIdList?.remove(data.iDeviceID)
                    }
                }
            })*/

            binding.container.setOnClickListener {
                onItemClick?.invoke(device.deviceNumber())
            }

            if (device is IAutelDroneDevice) {
                val deviceNumber = device.getDeviceNumber()
                val deviceName = device.getDeviceInfoBean()?.deviceName ?: ""
                binding.tvIpName.text = binding.root.context.getString(R.string.drone_device_info, deviceNumber.toString(), deviceName)
                binding.tvValue.text = binding.root.context.getString(R.string.drone_values,
                    device.isCenter(),
                    device.isControlled(),
                    device.isWatched(),
                    device.isConnected())
                binding.tvType.text = ""
                binding.tvControled.text = ""
            } else if (device is IAutelRemoteDevice) {
                val deviceNumber = device.getDeviceNumber()
                val deviceName = device.getDeviceInfoBean()?.deviceName ?: ""
                binding.tvIpName.text = binding.root.context.getString(R.string.remote_device_info, deviceNumber.toString(), deviceName)
                binding.tvValue.text = binding.root.context.getString(R.string.remote_values,
                    device.getDeviceInfoBean()?.isMainRc ?: false,
                    device.getDeviceInfoBean()?.isLocalRc ?: false)
                binding.tvType.text = ""
                binding.tvControled.text = ""
            }



        }
    }
}


