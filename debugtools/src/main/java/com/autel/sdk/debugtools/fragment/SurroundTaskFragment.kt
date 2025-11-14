package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateHFNtfyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkStateEnum
import com.autel.sdk.debugtools.databinding.FragmentSurroundTaskBinding
import com.autel.sdk.debugtools.helper.MissionSurroundVM

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/11/14
 */
class SurroundTaskFragment : AutelFragment() {

    companion object {
        //最小高度超过4.88米方可开始环绕
        const val MIN_HIGH = 5.0

        //大圆比小圆大1.4倍
        const val MAX_HIGH_MULTIPLE = 1.5
    }

    private lateinit var binding: FragmentSurroundTaskBinding

    private var deviceId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSurroundTaskBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenStatus()

        binding.btnEnterMission.setOnClickListener {
            SDKLog.d("SurroundTaskFragment","btnEnterMission")

            deviceId =
                DeviceManager.getFirstDroneDevice()?.getDeviceNumber() ?: return@setOnClickListener

            SDKLog.d("SurroundTaskFragment","btnEnterMission:$deviceId")
            if (deviceId > 0) {
                entrySurround(deviceId = deviceId, success = {
                    //成功了可以调用 startSurround
                })
            }

        }

        binding.btnStartMission.setOnClickListener {
            startSurround()
        }

        binding.btnPauseMission.setOnClickListener {
            pauseSurround()
        }

        binding.btnResumeMission.setOnClickListener {
            resumeSurround()
        }

        binding.btnExitMission.setOnClickListener {
            exitSurround()
        }
    }

    private fun entrySurround(deviceId: Int, success: () -> Unit) {
        MissionSurroundVM.enterSurround({
            success.invoke()
        }, {
            //  dataManager?.clear()
        }, deviceId)
    }

    private fun startSurround() {
        val data = DeviceManager.getFirstDroneDevice()?.getDeviceStateData()?.flightControlData
        data?.let {
            if (it.altitude > MIN_HIGH) {
                val radius = data.altitude.toInt()
                val height = data.altitude.toInt()
                val speed = data.velocityX.toInt()
                val dir = 0

                MissionSurroundVM.startSurround(radius, height, speed, dir, deviceId, {
                    SDKLog.i(TAG, "startSurround success $deviceId")
                }, {
                    SDKLog.e(TAG, "startSurround error:${it} $deviceId")
                })


            }
        }

    }

    private fun resumeSurround() {
        MissionSurroundVM.resumeSurround(deviceId, {
            SDKLog.i(TAG, "resumeSurround success $deviceId")
        }, {
            SDKLog.i(TAG, "resumeSurround error:${it} $deviceId")
        })
    }

    private fun pauseSurround() {
        MissionSurroundVM.pauseSurround(deviceId, {
            SDKLog.i(TAG, "pauseSurround success $deviceId")
        }, {
            SDKLog.i(TAG, "pauseSurround error:${it} $deviceId")
        })
    }

    private fun exitSurround() {
        MissionSurroundVM.exitSurround(deviceId, {
            SDKLog.i(TAG, "exitSurround success $deviceId")
        }, {
            SDKLog.i(TAG, "exitSurround error:${it} $deviceId")
        })
    }

    private fun listenStatus() {
        val key = KeyTools.createKey(CommonKey.KeyDroneSystemStatusHFNtfy)
        DeviceManager.getFirstDroneDevice()?.getKeyManager()?.listen(
            key, object : CommonCallbacks.KeyListener<DroneSystemStateHFNtfyBean> {
                override fun onValueChange(
                    oldValue: DroneSystemStateHFNtfyBean?, newValue: DroneSystemStateHFNtfyBean
                ) {
                    val droneWorkStatus = newValue.droneWorkStatus
                    when (droneWorkStatus) {
                        DroneWorkStateEnum.RUNNING -> {
                            //运行中
                            binding.tvResult.text = "RUNNING"
                        }

                        DroneWorkStateEnum.PAUSE -> {
                            //暂停
                            binding.tvResult.text = "PAUSE"
                        }

                        DroneWorkStateEnum.COMPLETED -> {
                            //完成
                            binding.tvResult.text = "COMPLETED"
                        }

                        else -> {
                            binding.tvResult.text = "UNKNOWN"
                        }
                    }
                }

            })
    }
}