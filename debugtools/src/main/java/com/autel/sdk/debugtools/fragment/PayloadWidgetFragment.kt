package com.autel.sdk.debugtools.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.data.PayloadWidgetInfo
import com.autel.drone.sdk.vmodelx.module.payload.widget.PayloadWidget
import com.autel.sdk.debugtools.PayloadWidgetVM
import com.autel.sdk.debugtools.adapter.PayloadWidgetIconAdapter
import com.autel.sdk.debugtools.adapter.PayloadWidgetItem
import com.autel.sdk.debugtools.databinding.FragmentPayloadWidgetBinding

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/15
 */
class PayloadWidgetFragment : AutelFragment() {
    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN

    private lateinit var binding: FragmentPayloadWidgetBinding

    private val payloadOtherWidgetInfo: StringBuilder = StringBuilder()
    private val payloadBasicInfo: StringBuilder = StringBuilder()
    private val payLoadAdapter = PayloadWidgetIconAdapter()
    private val payloadVM: PayloadWidgetVM by viewModels()

    companion object {
        private const val TAG = "PayloadWidgetFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPayloadWidgetBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()

        initWidgetBasicInfo()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        val typeName =
            arguments?.getString(PayloadCenterFragment.KEY_PAYLOAD_INDEX_TYPE) ?: "unknown"

        payloadIndexType = PayloadIndexType.findType(typeName)

        payloadVM.initListener(payloadIndexType)


        binding.tvPayloadTitle.text =
            "${payloadIndexType.name.lowercase()} PayloadManager info page"



        binding.rvMainWidget.adapter = payLoadAdapter

        binding.rvMainWidget.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val dividerItemDecoration =
            DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)

        binding.rvMainWidget.addItemDecoration(dividerItemDecoration)

        payloadVM.payloadWidgetInfo.observe(viewLifecycleOwner) {
            Log.d(TAG, "observe payload widget info:${it.toString()} ")
            showMainInterfaceWidgetInfo(it, payLoadAdapter)
            showPayloadOtherWidgetInfo(it)
        }

        //拉取负载对应widget信息
        binding.btnPullWidgetInfo.setOnClickListener {
            payloadVM.pullWidgetInfo()
        }

    }

    private fun initWidgetBasicInfo() {
        payloadVM.initListener(payloadIndexType)
        payloadVM.payloadBasicInfo.observe(viewLifecycleOwner) {
            it.apply {
                payloadBasicInfo.apply {
                    clear()
                     append("\n").append("PayloadBasicInfo:").append("\n")
                     append("payloadId:${payloadId}").append("\n")
                     append("payloadType:$payloadType").append("\n")
                     append("isOnline:$isOnline").append("\n")
                     append("lastActiveTime:$lastActiveTime").append("\n")
                     append("payloadPosition:$payloadPosition").append("\n")
                     append("psdklibVersion:$psdklibVersion").append("\n")
                     append("payloadVersion:$payloadVersion").append("\n")
                     append("receiveTime:$receiveTime").append("\n")

                }
                binding.tvPayloadBasicInfo.text = payloadBasicInfo.toString()

            }
        }
    }

    private fun showMainInterfaceWidgetInfo(
        payloadWidgetInfo: PayloadWidgetInfo?,
        adapter: PayloadWidgetIconAdapter
    ) {
        payloadWidgetInfo?.run {
            if (mainInterfaceWidgetList.isNullOrEmpty()) {
                resetView()
            }
            mainInterfaceWidgetList?.let { list ->
                val data = arrayListOf<PayloadWidgetItem>()
                for (widget in list) {
                    val subItemsSize = widget?.subItemsList?.size ?: -1
                    if (subItemsSize > 0) {
                        showListWidgetInfo(widget, data)
                    } else {
                        showNotListWidgetInfo(widget, data)
                    }
                }
                adapter.submitList(data)
            }
        }
    }

    private fun showPayloadOtherWidgetInfo(payloadWidgetInfo: PayloadWidgetInfo?) {
        payloadWidgetInfo?.run {
            payloadOtherWidgetInfo.apply {
                clear()
                append("\n").append("PayloadWidgetInfo-Part1:").append("\n")
                append(">>TextInputBoxWidget:$textInputBoxWidget").append("\n")
                append("\n")
                append(">>speakerWidget:$speakerWidget").append("\n")
                append("\n")
                append(">>floatingWindowWidget:$floatingWindowWidget").append("\n")
                append("\n")

            }
            if (configInterfaceWidgetList.isNullOrEmpty()) {
                return@run
            }
            for ((index, configWidget) in configInterfaceWidgetList!!.withIndex()) {
                payloadOtherWidgetInfo.apply {
                    append("configWidget $index-").append("$configWidget").append("\n")
                }
            }
            binding.tvPayloadWidgetInfo.text = payloadOtherWidgetInfo.toString()

        }

    }

    private fun resetView() {
        binding.tvPayloadWidgetInfo.text = ""
        payLoadAdapter.submitList(arrayListOf<PayloadWidgetItem>())
    }

    private fun showListWidgetInfo(widget: PayloadWidget, data: ArrayList<PayloadWidgetItem>) {
        if (widget.subItemsList.isNullOrEmpty())
            return

        //注意：List类型的Widget没有widget.iconFilePath为空，所以只拿subItemsList的IconFilePath
        for (subItem in widget.subItemsList!!) {
            val selectDesc =
                "subItemSelectIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType}(${widget.widgetType})},subItemsName=${subItem?.subItemsName},hitMsg:${widget.hintMessage}"
            val mainSelectIconPath = subItem?.subItemsIconFilePath?.selectedIconPath

            val unselectDesc =
                "subItemUnSelectIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType}(${widget.widgetType}),subItemsName=${subItem?.subItemsName},hitMsg:${widget.hintMessage}"
            val mainUnSelectIconPath = subItem?.subItemsIconFilePath?.unSelectedIconPath

            data.add(PayloadWidgetItem(selectDesc, mainSelectIconPath))
            data.add(PayloadWidgetItem(unselectDesc, mainUnSelectIconPath))
        }
    }

    private fun showNotListWidgetInfo(widget: PayloadWidget, data: ArrayList<PayloadWidgetItem>) {
        val selectDesc =
            "selectedIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType}(${widget.widgetType}),hitMsg:${widget.hintMessage}"
        val mainSelectIconPath = widget?.iconFilePath?.selectedIconPath

        val unselectDesc =
            "unSelectedIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType}(${widget.widgetType}),hitMsg:${widget.hintMessage}"
        val mainUnSelectIconPath = widget?.iconFilePath?.unSelectedIconPath

        data.add(PayloadWidgetItem(selectDesc, mainSelectIconPath))
        data.add(PayloadWidgetItem(unselectDesc, mainUnSelectIconPath))
    }
}