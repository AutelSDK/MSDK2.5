package com.autel.sdk.debugtools.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelRemoteDevice
import com.autel.drone.sdk.vmodelx.module.networking.bean.DeviceInfoBean
import com.autel.sdk.debugtools.databinding.ItemNetMeshDemoBinding


class NetMeshDemoAdapter : RecyclerView.Adapter<NetMeshDemoAdapter.ViewHolder>() {
    var dataList: MutableList<Any>? = null
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

        @SuppressLint("SetTextI18n")
        fun bindData(device: Any, position: Int) {
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
                val deviceId = when (device) {
                    is IAutelDroneDevice -> {
                        device.getDeviceNumber()
                    }

                    is IAutelRemoteDevice -> {
                        device.getDeviceNumber()
                    }

                    is DeviceInfoBean -> {
                        device.iDeviceID
                    }

                    else -> {
                        0
                    }
                }
                onItemClick?.invoke(deviceId)
            }

            when (device) {
                is IAutelDroneDevice -> {
                    binding.tvIpName.text = "Drone【id:${ device.getDeviceNumber()}, name:${device.getDeviceInfoBean()?.deviceName}】"
                    binding.tvValue.text = "Relay : ${device.isCenter()}, Controlled : ${device.isControlled()}, Watched : ${device.isWatched()}, Online : ${device.isConnected()}"

                    binding.tvType.text ="";
                    binding.tvControled.text = "";

                }

                is IAutelRemoteDevice -> {
                    binding.tvIpName.text = "Remoter【id:${ device.getDeviceNumber()}, name : ${device.getDeviceInfoBean()?.deviceName}】"
                    binding.tvValue.text = "Main RC : ${device.getDeviceInfoBean()?.isMainRc}, Local RC : ${device.getDeviceInfoBean()?.isLocalRc}"

                    binding.tvType.text = ""
                    binding.tvControled.text = ""
                }

                is DeviceInfoBean -> {
                    binding.tvIpName.text = "Remoter【id:${ device.iDeviceID}, name : ${device.deviceName}】"
                    binding.tvValue.text = "Main RC : ${device.isMainRc}, Local RC : ${device.isLocalRc}"

                    binding.tvType.text = ""
                    binding.tvControled.text = ""
                }
            }
        }
    }
}


