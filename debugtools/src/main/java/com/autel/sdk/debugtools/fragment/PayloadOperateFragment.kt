package com.autel.sdk.debugtools.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.module.payload.PayloadCenter
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetValue
import com.autel.drone.sdk.vmodelx.module.payload.data.PayloadWidgetInfo
import com.autel.drone.sdk.vmodelx.module.payload.widget.PayloadWidget
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.PayloadWidgetVM
import com.autel.sdk.debugtools.adapter.PayloadConfigInterfaceAdapter
import com.autel.sdk.debugtools.adapter.PayloadMainInterfaceAdapter
import com.autel.sdk.debugtools.databinding.FragmentPayloadOperateBinding
import com.autel.sdk.debugtools.fragment.PayloadDataFragment.Companion.MAX_LENGTH_OF_SEND_DATA

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/17
 * 单一位置负载Widget功能展示以及通信实例
 */
class PayloadOperateFragment : AutelFragment() {

    private lateinit var binding: FragmentPayloadOperateBinding
    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN
    private val payloadVM: PayloadWidgetVM by viewModels()

    //main
    private var mainAdapter: PayloadMainInterfaceAdapter? = null

    //config
    private var configAdapter: PayloadConfigInterfaceAdapter? = null


    companion object {
        private const val TAG = "PayloadOperateFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPayloadOperateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        val typeName =
            arguments?.getString(PayloadCenterFragment.KEY_PAYLOAD_INDEX_TYPE) ?: "unknown"

        payloadIndexType = PayloadIndexType.findType(typeName)

        payloadVM.initListener(payloadIndexType)

        binding.tvPayloadType.text =  "${payloadIndexType.name.lowercase()} Payload Operate page"

        //main
        mainAdapter = PayloadMainInterfaceAdapter(onItemClick = { position, item ->
            ToastUtils.showToast("click on:${position}")
            val droneDevice = payloadVM.getSingleControlDrone()
            if (droneDevice != null) {
                val widgetValue = WidgetValue().apply {
                    type = WidgetType.findWidgetType(item.widgetType)
                    index = item.widgetIndex ?: 0
                    value = 1
                }
                payloadVM.setWidgetValue(
                    widgetValue, onSuccess = {
                        ToastUtils.showToast("onSuccess=$widgetValue")
                    }, onFailure = {code,msg->
                        ToastUtils.showToast("onFailure=$widgetValue, $code")
                    })
            }
        })
        val dividerItemDecoration =
            DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        binding.recyclerviewMain.adapter = mainAdapter
        binding.recyclerviewMain.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.recyclerviewMain.addItemDecoration(dividerItemDecoration)

        //config
        configAdapter = PayloadConfigInterfaceAdapter(payloadIndexType)
        binding.recyclerviewConfig.adapter = configAdapter
        binding.recyclerviewConfig.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerviewConfig.addItemDecoration(dividerItemDecoration)

        payloadVM.payloadWidgetInfo.observe(viewLifecycleOwner) {
            Log.d(TAG, "observe payload widget info:${it.toString()} ")
            setMainPage(it)
            setConfigPage(it)
        }

        payloadVM.pullWidgetInfo()

        binding.btnInputSend.setOnClickListener {
            sendTextInputBoxContent()
        }
    }

    //主界面
    private fun setMainPage(widgetInfo: PayloadWidgetInfo) {
        if (!widgetInfo.mainInterfaceWidgetList.isNullOrEmpty()) {
            mainAdapter?.submitList(widgetInfo.mainInterfaceWidgetList)
        }
    }

    //配置界面
    private fun setConfigPage(widgetInfo: PayloadWidgetInfo) {
        //输入
        val inputWidget = widgetInfo.textInputBoxWidget
        binding.layoutTextInput.isVisible = inputWidget != null
        widgetInfo.textInputBoxWidget?.let {
            binding.tvTextInputBoxTitle.text = it.widgetName
            binding.tvTextInputBoxHint.text = it.placeHolderText
        }

        if (!widgetInfo.configInterfaceWidgetList.isNullOrEmpty()) {
            configAdapter?.submitList(widgetInfo.configInterfaceWidgetList)
        }
    }

    private fun sendTextInputBoxContent(){
        val input = binding.etInput.text.trim().toString()
        if (TextUtils.isEmpty(input)){
            ToastUtils.showToast("Please input something~")
            return
        }

        val byteArray = input.toByteArray()
        val size = byteArray.size
        val sendText = "The content sent is:${binding.etInput.text},byte length is:$size"
        if (size > MAX_LENGTH_OF_SEND_DATA) {
            ToastUtils.showToast(
                "The length of the sent content is $size bytes, which exceeds the maximum sending length of $MAX_LENGTH_OF_SEND_DATA bytes, and the sending fails!!!"
            )
            return
        }
        payloadVM.sendDataToPayload(byteArray, onSuccess = {
            ToastUtils.showToast("send data:$sendText onSuccess")
        }, onFailure = {code, msg ->
            ToastUtils.showToast("send data:$sendText onFailure,code:$code,msg:$msg")
        })

    }
}