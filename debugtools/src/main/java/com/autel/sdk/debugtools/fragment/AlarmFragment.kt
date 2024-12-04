package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.v2.bean.AutelDeviceStatus
import com.autel.drone.sdk.v2.callback.AutelDeviceStatusChangeListener
import com.autel.drone.sdk.v2.enum.WarningLevel
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.beans.DeviceAlarm
import com.autel.sdk.debugtools.databinding.FragmentAlarmBinding
import com.autel.sdk.debugtools.databinding.ItemDeviceStatusBinding

/**
 * base fragment for debug tools
 * Copyright: Autel Robotics
 * @author huangsihua on 2022/12/17.
 */
class AlarmFragment : AutelFragment() {

    private lateinit var binding: FragmentAlarmBinding
    private val currentAlarm: MutableList<DeviceAlarm> = mutableListOf()
    private val alamList: MutableList<DeviceAlarm> = mutableListOf()
    private val currentAdapter = AlarmAdapter()
    private val listAdapter = AlarmAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAlarmBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DeviceManager.getDeviceManager().getDroneDevices().filter { it.isConnected() }.forEach {
            val statusManager = it.getDeviceStatusManager()
            statusManager.getCurrentDeviceStatus()?.let { status ->
                currentAlarm.add(DeviceAlarm(it, status))
            }

            statusManager.getCurrentDeviceStatusList().let { status ->
                alamList.addAll(status.map { s -> DeviceAlarm(it, s) })
            }

            SDKLog.i("DeviceStatusManager", "onViewCreated: ${it.getDroneSn()}")
            statusManager.addAutelDeviceStatusChangeListener(object : AutelDeviceStatusChangeListener {
                override fun onDeviceStatusUpdate(from: AutelDeviceStatus?, to: AutelDeviceStatus) {
                    SDKLog.i("DeviceStatusManager", "onDeviceStatusUpdate: $from -> $to")
                    from?.let { status ->
                        currentAlarm.removeAll { alarm -> alarm.device == it && alarm.status.statusCode == status.statusCode }
                    }
                    currentAlarm.add(DeviceAlarm(it, to))

                    currentAdapter.dataList = currentAlarm.toSet().toMutableList()
                    currentAdapter.notifyDataSetChanged()
                }

                override fun onDeviceStatusListUpdate(from: List<AutelDeviceStatus>, to: List<AutelDeviceStatus>) {
                    SDKLog.i("DeviceStatusManager", "onDeviceStatusListUpdate: $from -> $to")
                    from.forEach { status ->
                        alamList.removeAll { alarm -> alarm.device == it && alarm.status.statusCode == status.statusCode }
                    }
                    alamList.addAll(to.map { s -> DeviceAlarm(it, s) })

                    listAdapter.dataList = alamList.toSet().toMutableList()
                    listAdapter.notifyDataSetChanged()
                }
            })
        }

        currentAdapter.dataList = currentAlarm
        binding.rvCurrentAlarm.adapter = currentAdapter
        currentAdapter.notifyDataSetChanged()

        listAdapter.dataList = alamList
        binding.rvAlarmList.adapter = listAdapter
        listAdapter.notifyDataSetChanged()
    }


    internal class AlarmAdapter : RecyclerView.Adapter<ViewHolder>() {

        var dataList: MutableList<DeviceAlarm>? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            val binding = ItemDeviceStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            dataList?.let {
                holder.bindData(it[position])
            }
        }

        override fun getItemCount(): Int {
            return dataList?.size ?: 0
        }
    }

     class ViewHolder(val binding: ItemDeviceStatusBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(data: DeviceAlarm) {
            binding.tvDroneName.text = data.device.getDeviceInfoBean()?.deviceName ?: "Unknown"
            binding.tvAlarmInfo.text = "${data.status.description}(${data.status.statusCode})"
            val ctx = binding.root.context
            when (data.status.warningLevel) {
                WarningLevel.NORMAL -> binding.root.setBackgroundColor(ctx.resources.getColor(R.color.debug_color_white))
                WarningLevel.NOTICE -> binding.root.setBackgroundColor(ctx.resources.getColor(R.color.debug_color_2A83FB))
                WarningLevel.CAUTION -> binding.root.setBackgroundColor(ctx.resources.getColor(R.color.debug_color_secondary_ffda00))
                WarningLevel.WARNING -> binding.root.setBackgroundColor(ctx.resources.getColor(R.color.debug_color_orange))
                WarningLevel.SERIOUS_WARNING -> binding.root.setBackgroundColor(ctx.resources.getColor(R.color.debug_color_FF0000))
            }
        }
     }
}