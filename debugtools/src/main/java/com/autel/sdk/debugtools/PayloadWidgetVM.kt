package com.autel.sdk.debugtools

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IPayloadManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.PayloadInfoBean
import com.autel.drone.sdk.vmodelx.module.payload.PayloadCenter
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetValue
import com.autel.drone.sdk.vmodelx.module.payload.data.PayloadBasicInfo
import com.autel.drone.sdk.vmodelx.module.payload.data.PayloadWidgetInfo
import com.autel.drone.sdk.vmodelx.module.payload.listener.PayloadBasicInfoListener
import com.autel.drone.sdk.vmodelx.module.payload.listener.PayloadWidgetInfoListener
import com.autel.drone.sdk.vmodelx.utils.ToastUtils

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/15
 */
class PayloadWidgetVM : ViewModel() {
    companion object {
        private const val TAG = "PayloadWidgetVM"
    }

    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN

    private val payloadManagerMap = PayloadCenter.get().getPayloadManager()

    //负载信息
    val payloadBasicInfo = MutableLiveData<PayloadInfoBean>()

    //负载 widget信息
    val payloadWidgetInfo = MutableLiveData<PayloadWidgetInfo>()

    //负载信息
    private val payloadBasicInfoListener: IPayloadManager.IPayloadBasicInfoListener =
        IPayloadManager.IPayloadBasicInfoListener { device, basicInfo ->
            payloadBasicInfo.postValue(
                basicInfo
            )
        }

    private val payloadWidgetInfoListener: PayloadWidgetInfoListener =
        object : PayloadWidgetInfoListener {
            override fun onPayloadWidgetInfoUpdate(
                device: IAutelDroneDevice,
                widgetInfo: PayloadWidgetInfo
            ) {
                payloadWidgetInfo.postValue(widgetInfo)
            }

        }


    /**
     * 设置飞机对应负载的Widget value
     */
    fun setWidgetValue(
        value: WidgetValue,
        onSuccess: () -> Unit,
        onFailure: (code: IAutelCode, msg: String?) -> Unit
    ) {
        for ((key, manager) in payloadManagerMap) {
            if (key == payloadIndexType) {
                manager.setWidgetValue(
                    getSingleControlDrone(),
                    value,
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            onSuccess.invoke()
                        }

                        override fun onFailure(code: IAutelCode, msg: String?) {
                            onFailure.invoke(code, msg)
                        }

                    })
            }
        }
    }

    /**
     * 发送数据给对应负载
     */
    fun sendDataToPayload(
        byteArray: ByteArray,
        onSuccess: () -> Unit,
        onFailure: (code: IAutelCode, msg: String?) -> Unit
    ) {
        for ((key, manager) in payloadManagerMap) {
            if (key == payloadIndexType) {
                manager.sendDataToPayload(
                    getSingleControlDrone(), byteArray,
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            onSuccess.invoke()
                        }

                        override fun onFailure(code: IAutelCode, msg: String?) {
                            onFailure.invoke(code, msg)
                        }

                    })
            }
        }
    }

    fun initListener(payloadIndexType: PayloadIndexType) {
        this.payloadIndexType = payloadIndexType

        val manager = payloadManagerMap[payloadIndexType]
        manager?.addPayloadBasicInfoListener(payloadBasicInfoListener)
        manager?.addPayloadWidgetInfoListener(payloadWidgetInfoListener)

    }

    /**
     * 拉取widget数据
     */
    fun pullWidgetInfo() {
        val manager = payloadManagerMap[payloadIndexType]
        Log.d(TAG, "pullWidgetInfo->manager:$manager")
        manager?.pullWidgetInfoFromPayload(getSingleControlDrone())
    }

    /**
     * 获取单控飞机
     */
    fun getSingleControlDrone(): IAutelDroneDevice? {
        return if (DeviceManager.getDeviceManager().isSingleControl()) {
            DeviceManager.getMultiDeviceOperator().getControlledDroneList().firstOrNull()
        } else {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        val manager = payloadManagerMap[payloadIndexType]
        manager?.removePayloadWidgetInfoListener(payloadWidgetInfoListener)

    }

}