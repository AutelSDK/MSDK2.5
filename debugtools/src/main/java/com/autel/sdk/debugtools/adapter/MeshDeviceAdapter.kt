package com.autel.sdk.debugtools.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.vmodelx.module.networking.bean.DeviceInfoBean
import com.autel.sdk.debugtools.databinding.ItemMeshDeviceInfoBinding
import com.autel.sdk.debugtools.R

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

            val context = binding.root.context
            binding.tvIpName.text = context.getString(R.string.device_id_ip, data.sDeviceID, data.ip)
            binding.tvValue.text = context.getString(R.string.device_type_biz_node_type, data.deviceType, data.bizNodeType)
            binding.tvType.text = context.getString(R.string.network_node_type, data.networkNodeType)
            binding.tvControled.text = context.getString(R.string.local_device_active, data.isLocal, data.active)
        }
    }
}

