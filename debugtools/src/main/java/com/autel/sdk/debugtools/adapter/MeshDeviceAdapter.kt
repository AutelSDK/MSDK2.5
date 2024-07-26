package com.autel.sdk.debugtools.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.vmodelx.module.networking.bean.DeviceInfoBean
import com.autel.sdk.debugtools.databinding.ItemMeshDeviceInfoBinding


/**
 * Created by lizhiping on 2023/3/14.
 * <p>
 * xxx
 */
class MeshDeviceAdapter : RecyclerView.Adapter<MeshDeviceAdapter.ViewHolder>() {
    var dataList: MutableList<DeviceInfoBean>? = null
    var deviceIdList: MutableList<Int>? = mutableListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemMeshDeviceInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class ViewHolder(val binding: ItemMeshDeviceInfoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(data: DeviceInfoBean, position: Int) {
            binding.cbCheck.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener{
                override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                    if(p1){
                        deviceIdList?.add(data.iDeviceID)
                    }else{
                        deviceIdList?.remove(data.iDeviceID)
                    }
                }
            })

            binding.tvIpName.text = "DeviceID:"+ data.sDeviceID+" IP:"+ data.ip;
            binding.tvValue.text = "deviceType(类型):"+data.deviceType+" bizNodeType(可控):"+data.bizNodeType;
            binding.tvType.text = "networkNodeType(中心结点)"+data.networkNodeType;
            binding.tvControled.text = "bLocal(本地设备):"+data.isLocal+" active(当前飞机):"+data.active;
        }
    }
}

