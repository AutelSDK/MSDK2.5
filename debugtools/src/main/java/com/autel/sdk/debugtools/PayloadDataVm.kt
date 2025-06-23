/*
package com.autel.sdk.debugtools

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.module.payload.PayloadCenter
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.listener.PayloadDataListener
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import java.text.SimpleDateFormat
import java.util.Locale

*/
/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/15
 *//*

class PayloadDataVm : ViewModel() {
    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN
    private val payloadManagerMap = PayloadCenter.get().getPayloadManager()
    val receiveMessageLiveData = MutableLiveData<String>()

    private val payloadDataListener = object : PayloadDataListener {
        override fun onDataFromPayloadUpdate(data: ByteArray) {
            var result = "接受时间：${getTimeNow()}"
            if (data.isNotEmpty()) {
                result += ",接受内容：${String(data)}"
                receiveMessageLiveData.postValue(result)
            } else {
                result += ",接受内容为空"
                receiveMessageLiveData.postValue(result)
            }
        }

    }

    fun sendMessageToPayloadSdk(data: ByteArray) {
        payloadManagerMap[payloadIndexType]?.sendDataToPayload(null, data,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("send message to payload success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    ToastUtils.showToast("send message to payload failure:[code:${code.code};msg:${msg}]")
                }

            })
    }

    fun initPayloadDataListener(payloadType: PayloadIndexType){
        this.payloadIndexType = payloadType
        payloadManagerMap[payloadType]?.addPayloadDataListener(payloadDataListener)
    }

    override fun onCleared() {
        super.onCleared()
        payloadManagerMap[payloadIndexType]?.removePayloadDataListener(payloadDataListener)
    }

    private fun getTimeNow(): String {
        val currentTime = System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA).format(currentTime)
    }
}*/
