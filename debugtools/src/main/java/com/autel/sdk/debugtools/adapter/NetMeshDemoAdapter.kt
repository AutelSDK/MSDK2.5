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

            if(device is IAutelDroneDevice){

                binding.tvIpName.text = "无人机【id:${ device.getDeviceNumber()}, name:${device.getDeviceInfoBean()?.deviceName}】"
                binding.tvValue.text = "中继飞机:"+device.isCenter()+"， 受控飞机:"+device.isControlled()+"， Watch飞机:"+device.isWatched()+ "， 在线:"+device.isConnected();
                binding.tvType.text ="";
                 binding.tvControled.text = "";

            } else if(device is IAutelRemoteDevice){
                binding.tvIpName.text = "遥控器【id:${ device.getDeviceNumber()}, name:${device.getDeviceInfoBean()?.deviceName}】"
                binding.tvValue.text = "主遥控器:"+device.getDeviceInfoBean()?.isMainRc + "， 本地遥控器:"+device.getDeviceInfoBean()?.isLocalRc;

                binding.tvType.text = "";
                binding.tvControled.text = "";
            }


        }
    }
}


