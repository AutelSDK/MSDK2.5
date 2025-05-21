package com.autel.sdk.debugtools.fragment

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.PayloadDataVm
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentPayloadDataBinding

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/15
 */
class PayloadDataFragment : AutelFragment() {

    companion object {
        const val MAX_LENGTH_OF_SEND_DATA = 255
    }

    private lateinit var binding: FragmentPayloadDataBinding
    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UP

    //list
    private var msgList: ArrayList<String> = arrayListOf()
    private lateinit var payloadAdapter: ArrayAdapter<String>

    private val payloadDataVm: PayloadDataVm by viewModels()
    private val onKeyListener: View.OnKeyListener = object : View.OnKeyListener {
        override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                //隐藏软键盘
                val inputMethodManager =
                    activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (inputMethodManager.isActive) {
                    inputMethodManager.hideSoftInputFromWindow(v.applicationWindowToken, 0)
                }
                return true
            }
            return false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPayloadDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()

        initListener()
    }

    private fun initView() {
        val typeName = arguments?.getString(PayloadCenterFragment.KEY_PAYLOAD_INDEX_TYPE)?:"unknown"
        payloadIndexType = PayloadIndexType.findType(typeName)

        payloadAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, msgList)

        binding.messageListview.adapter = payloadAdapter

        payloadDataVm.receiveMessageLiveData.observe(viewLifecycleOwner) { data ->
            msgList.add(data)
            payloadAdapter.notifyDataSetChanged()
            binding.messageListview.setSelection(msgList.size - 1)
        }

        binding.tvTitle.text = getString(
            R.string.debug_send_data_to_payload_demo, payloadIndexType.name.lowercase()
        )
    }

    private fun initListener() {
        payloadDataVm.initPayloadDataListener(payloadIndexType)

        binding.etInput.setOnKeyListener(onKeyListener)

        binding.btnSendData.setOnClickListener {
            val byteArray = binding.etInput.text.trim().toString().toByteArray()
            val size = byteArray.size
            val sendText = "The content sent is:${binding.etInput.text},byte length is:$size"
            if (size > MAX_LENGTH_OF_SEND_DATA) {
                ToastUtils.showToast(
                    "The length of the sent content is $size bytes, which exceeds the maximum sending length of $MAX_LENGTH_OF_SEND_DATA bytes," + " and the sending fails!!!"
                )
                return@setOnClickListener
            }
            payloadDataVm.sendMessageToPayloadSdk(byteArray)
            ToastUtils.showToast("send data:$sendText")
        }
    }
}