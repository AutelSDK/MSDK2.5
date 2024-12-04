package com.autel.sdk.debugtools.beans

import com.autel.drone.sdk.v2.bean.AutelDeviceStatus
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice

data class DeviceAlarm(
    val device: IAutelDroneDevice,
    val status: AutelDeviceStatus
)
