/*
package com.autel.sdk.debugtools.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.PayloadAckBean
import com.autel.drone.sdk.vmodelx.module.payload.PayloadCenter
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetValue
import com.autel.drone.sdk.vmodelx.module.payload.widget.PayloadWidget
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.listener.OnSeekBarChangeListener


*/
/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/17
 * 配置界面
 *//*

class PayloadConfigInterfaceAdapter(
    private val payloadIndexType: PayloadIndexType,
    private val onItemClick: ((position: Int, widget: PayloadWidget) -> Unit)? = null
) :
    ListAdapter<PayloadWidget, RecyclerView.ViewHolder>(DiffCallback) {


    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val callBack: (WidgetValue) -> Unit = { widgetValue -> setWidgetValue(widgetValue) }
        val viewHolder = when (viewType) {
            1 -> {//button
                val view =
                    inflater.inflate(R.layout.layout_config_interface_btn_list_item, parent, false)
                ButtonViewHolder(view, parent.context, callBack)
            }

            2 -> {//switch
                val view =
                    inflater.inflate(
                        R.layout.layout_config_interface_switch_list_item,
                        parent,
                        false
                    )
                SwitchViewHolder(view, parent.context, callBack)
            }

            3 -> {//scale
                val view =
                    inflater.inflate(
                        R.layout.layout_config_interface_scale_list_item,
                        parent,
                        false
                    )
                ScaleViewHolder(view, parent.context, callBack)
            }

            4 -> {//list
                val view =
                    inflater.inflate(R.layout.layout_config_interface_list_list_item, parent, false)
                ListViewHolder(view, parent.context, callBack)
            }

            5 -> {//input
                val view =
                    inflater.inflate(
                        R.layout.layout_config_interface_input_list_item,
                        parent,
                        false
                    )
                IntegerInputViewHolder(view, parent.context, callBack)
            }

            else -> {
                val view =
                    inflater.inflate(R.layout.layout_config_interface_btn_list_item, parent, false)
                ButtonViewHolder(view, parent.context, callBack)
            }

        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(position, item)
        }
        when (viewType) {
            1 -> {//button
                (holder as ButtonViewHolder).apply {
                    bind(item)
                    getWidgetValue(item, this)
                }

            }

            2 -> {//switch
                (holder as SwitchViewHolder).apply {
                    bind(item)
                    getWidgetValue(item, this)
                }
            }

            3 -> {//scale
                (holder as ScaleViewHolder).apply {
                    bind(item)
                    getWidgetValue(item, this)
                }
            }

            4 -> {//list
                (holder as ListViewHolder).apply {
                    bind(item)
                    getWidgetValue(item, this)
                }
            }

            5 -> {//input
                (holder as IntegerInputViewHolder).apply {
                    bind(item)
                    getWidgetValue(item, this)
                }
            }

            else -> {
                (holder as ButtonViewHolder).apply {
                    bind(item)
                    getWidgetValue(item, this)
                }
            }
        }
    }


    private fun setWidgetValue(widgetValue: WidgetValue) {
        val droneDevice = getSingleControlDrone()
        if (droneDevice == null) {
            Log.e("PayloadConfigInterfaceAdapter", "setWidgetValue droneDevice is null")
        } else {
            PayloadCenter.get().getPayloadManager()[payloadIndexType]?.setWidgetValue(
                droneDevice,
                widgetValue,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Log.i(
                            "PayloadConfigInterfaceAdapter",
                            "setWidgetValue success $widgetValue ${droneDevice?.toSampleString()}"
                        )
                    }

                    override fun onFailure(code: IAutelCode, msg: String?) {
                        Log.e(
                            "PayloadConfigInterfaceAdapter",
                            "setWidgetValue onFailure $widgetValue ${droneDevice?.toSampleString()}"
                        )
                    }
                })
        }
    }

    private fun getWidgetValue(payloadWidget: PayloadWidget, holder: BaseHolder) {
        val widgetValue = WidgetValue().apply {
            index = payloadWidget.widgetIndex ?: 0
            type = WidgetType.findWidgetType(payloadWidget.widgetType)
            value = 0
        }
        val droneDevice = getSingleControlDrone()
        if (droneDevice == null) {
            Log.e("PayloadConfigInterfaceAdapter", "getWidgetValue droneDevice is null")
        } else {
            PayloadCenter.get().getPayloadManager()[payloadIndexType]?.getWidgetValue(
                droneDevice,
                widgetValue,
                object : CommonCallbacks.CompletionCallbackWithParam<PayloadAckBean> {
                    override fun onSuccess(t: PayloadAckBean?) {
                        Log.i(
                            "PayloadConfigInterfaceAdapter",
                            "getWidgetValue success $widgetValue, $t ${droneDevice?.toSampleString()}"
                        )
                        t?.value?.let { holder.updateData(it) }
                    }

                    override fun onFailure(code: IAutelCode, msg: String?) {
                        Log.e(
                            "PayloadConfigInterfaceAdapter",
                            "getWidgetValue onFailure $widgetValue ${droneDevice?.toSampleString()}"
                        )
                    }
                })
        }
    }

    override fun getItemViewType(position: Int): Int {
        val type = WidgetType.findWidgetType(getItem(position).widgetType).value
        return type
    }

    */
/**
     * 获取单控飞机
     *//*

    private fun getSingleControlDrone(): IAutelDroneDevice? {
        return if (DeviceManager.getDeviceManager().isSingleControl()) {
            DeviceManager.getMultiDeviceOperator().getControlledDroneList().firstOrNull()
        } else {
            null
        }
    }


    //button
    class ButtonViewHolder(
        itemView: View,
        val context: Context,
        val callBack: (WidgetValue) -> Unit
    ) :
        BaseHolder(itemView) {
        private val buttonName: TextView = itemView.findViewById(R.id.tv_button_name)

        fun bind(item: PayloadWidget) {
            buttonName.text = item.widgetName
            buttonName.setOnClickListener {
                val widgetValue = WidgetValue().apply {
                    index = item.widgetIndex ?: 0
                    type = WidgetType.findWidgetType(item.widgetType)
                    value = 1
                }
                callBack.invoke(widgetValue)
            }
        }

        override fun updateData(value: Int) {
            Log.i(
                "PayloadConfigInterfaceAdapter",
                "ButtonViewHolder updateData $value"
            )
        }
    }

    //scale
    class ScaleViewHolder(
        itemView: View,
        val context: Context,
        val callBack: (WidgetValue) -> Unit
    ) :
        BaseHolder(itemView) {
        private val scaleName: TextView = itemView.findViewById(R.id.tv_scale_name)
        private val scaleValue: TextView = itemView.findViewById(R.id.tv_scale_value)
        private val seekbar: SeekBar = itemView.findViewById(R.id.seekbar_scale)

        fun bind(item: PayloadWidget) {
            scaleName.text = item.widgetName
            seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener() {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    super.onProgressChanged(seekBar, progress, fromUser)
                    scaleValue.text = "$progress"

                    val widgetValue = WidgetValue().apply {
                        index = item.widgetIndex ?: 0
                        type = WidgetType.findWidgetType(item.widgetType)
                        value = progress
                    }
                    callBack.invoke(widgetValue)
                }
            })
        }

        override fun updateData(value: Int) {
            scaleValue.text = "$value"
            seekbar.progress = value
            Log.i(
                "PayloadConfigInterfaceAdapter",
                "ScaleViewHolder updateData $value"
            )
        }
    }

    //input
    class IntegerInputViewHolder(
        itemView: View,
        val context: Context,
        val callBack: (WidgetValue) -> Unit
    ) :
        BaseHolder(itemView) {
        private val inputName: TextView = itemView.findViewById(R.id.tv_input_name)
        private val inputHint: TextView = itemView.findViewById(R.id.tv_input_hint)
        private val etInput: EditText = itemView.findViewById(R.id.et_input)

        fun bind(item: PayloadWidget) {
            inputName.text = item.widgetName
            inputHint.text = item.hintMessage

            etInput.addTextChangedListener {
                val content = it?.trim().toString()
                try {
                    val input = content.toIntOrNull()
                    input?.let {
                        val widgetValue = WidgetValue().apply {
                            index = item.widgetIndex ?: 0
                            type = WidgetType.findWidgetType(item.widgetType)
                            value = input
                        }

                        callBack.invoke(widgetValue)
                    }

                } catch (exception: Exception) {
                    Log.i(
                        "PayloadConfigInterfaceAdapter",
                        "IntegerInputViewHolder exception ${exception.message}"
                    )
                }

            }
        }

        override fun updateData(value: Int) {
            etInput.setText(value.toString())
            Log.i(
                "PayloadConfigInterfaceAdapter",
                "IntegerInputViewHolder updateData $value"
            )
        }
    }

    //switch
    class SwitchViewHolder(
        itemView: View,
        val context: Context,
        val callBack: (WidgetValue) -> Unit
    ) :
        BaseHolder(itemView) {
        private val switchName: TextView = itemView.findViewById(R.id.tv_switch_name)
        private val switch: SwitchCompat = itemView.findViewById(R.id.switch_state)

        fun bind(item: PayloadWidget) {
            switchName.text = item.widgetName
            switch.setOnCheckedChangeListener { buttonView, isChecked ->
                val widgetValue = WidgetValue().apply {
                    index = item.widgetIndex ?: 0
                    type = WidgetType.findWidgetType(item.widgetType)
                    value = if (isChecked) 1 else 0
                }
                callBack.invoke(widgetValue)
            }
        }

        override fun updateData(value: Int) {
            switch.isChecked = value == 1
            Log.i(
                "PayloadConfigInterfaceAdapter",
                "SwitchViewHolder updateData $value"
            )
        }
    }

    //list
    class ListViewHolder(
        itemView: View,
        val context: Context,
        val callBack: (WidgetValue) -> Unit
    ) :
        BaseHolder(itemView) {

        private val listName: TextView = itemView.findViewById(R.id.tv_list_name)
        private val spinner: Spinner = itemView.findViewById(R.id.spinner)

        fun bind(item: PayloadWidget) {
            listName.text = item.widgetName

            val adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, getSpannerData(item))

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinner.adapter = adapter
            spinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val widgetValue = WidgetValue().apply {
                        index = item.widgetIndex ?: 0
                        type = WidgetType.findWidgetType(item.widgetType)
                        value = position + 1
                    }
                    callBack.invoke(widgetValue)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }

        }

        override fun updateData(value: Int) {
            if (value - 1 >= 0)
                spinner.setSelection(value - 1)
            Log.i(
                "PayloadConfigInterfaceAdapter",
                "ButtonViewHolder updateData $value"
            )
        }

        private fun getSpannerData(item: PayloadWidget): MutableList<String> {
            val itemList = item.subItemsList
            val result: MutableList<String> = ArrayList()
            if (!itemList.isNullOrEmpty()) {
                itemList.forEach {
                    it.subItemsName?.let { name -> result.add(name) }
                }
            }
            return result
        }
    }


    abstract class BaseHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun updateData(value: Int)
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
