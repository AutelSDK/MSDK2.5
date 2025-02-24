package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Handler.Callback
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.v2.enum.PerceptionDirection
import com.autel.drone.sdk.v2.interfaces.RadarInformationListener
import com.autel.drone.sdk.v2.manager.PerceptionManager
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.vision.bean.VisionRadarInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.vision.enums.ObstacleAvoidActionEnum
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragPerceptionPageBinding
import kotlin.math.max

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2024/10/10
 * 感知避障示例
 */
class PerceptionFragment : AutelFragment(), IAutelDroneListener,RadarInformationListener {

    private lateinit var binding: FragPerceptionPageBinding
    private var isConnected: Boolean = false
    private val logList: MutableList<String> = mutableListOf()

    private var breakDistance: Float = 1.0f

    private var warningDistance: Float = 1.0f

    companion object {
        //设置告警距离
        private const val MSG_UPDATE_BREAK_DISTANCE = 0x1000
        private const val MSG_UPDATE_WARNING_DISTANCE = 0x1001
    }

    private val handler: Handler = Handler(Looper.getMainLooper(), Callback {
        when (it.what) {
            MSG_UPDATE_WARNING_DISTANCE -> {
                it.data.getInt("distance").let { distance ->
                    setAllWarningDistance(distance)
                }

            }

            MSG_UPDATE_BREAK_DISTANCE -> {
                it.data.getInt("distance").let { distance ->
                    setAllBreakDistance(distance)
                }

            }
        }
        true
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragPerceptionPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //默认单机情况
        isConnected = DeviceManager.getFirstDroneDevice()?.isConnected() ?: false
        SDKManager.get().getDeviceManager().addDroneListener(this)

        initViews()
    }

    private fun initViews() {
        //避障类型
        getObstacleAvoidanceType()

        binding.rgObstacleAvoidanceType.setOnCheckedChangeListener { _, checkedId ->
            updataLogInfo("切换避障类型：${binding.root.findViewById<RadioButton>(checkedId).text}")
            when (checkedId) {
                R.id.rb_obstacle_close -> changeObstacleAvoidanceType(ObstacleAvoidActionEnum.CLOSE)
                R.id.rb_obstacle_break -> changeObstacleAvoidanceType(ObstacleAvoidActionEnum.STOP)
                R.id.rb_obstacle_bypass -> changeObstacleAvoidanceType(ObstacleAvoidActionEnum.BYPASS)
            }
        }

        //告警距离
        //modelx 默认告警距离范围为1-10，默认情况下为5
        binding.tvWarningDistanceTitle.text =
            getString(R.string.common_text_alarm_distance, "1", "10")
        binding.tvWarningDistanceStart.text = "1"
        binding.tvWarningDistanceEnd.text = "10"

        binding.seekbarWarningDistance.valueFrom = 1f
        binding.seekbarWarningDistance.valueTo = 10f

        //不要频繁设置
        binding.seekbarWarningDistance.addOnChangeListener { _, value, _ ->
            binding.tvWarningDistanceValue.text = value.toInt().toString()
            warningDistance = value

            handler.removeMessages(MSG_UPDATE_WARNING_DISTANCE)
            val msg = handler.obtainMessage(MSG_UPDATE_WARNING_DISTANCE)
            msg.data = bundleOf("distance" to value.toInt())
            handler.sendMessageDelayed(msg, 800)

            updateBreakDistanceRange(value.toInt())
        }

        getWarningDistance()

        //刹停距离
        getBreakDistance()

        binding.seekbarBreakDistance.addOnChangeListener { _, value, _ ->
            binding.tvBreakDistanceValue.text = value.toInt().toString()
            breakDistance = value

            handler.removeMessages(MSG_UPDATE_BREAK_DISTANCE)
            val msg = handler.obtainMessage(MSG_UPDATE_BREAK_DISTANCE)
            msg.data = bundleOf("distance" to value.toInt())
            handler.sendMessageDelayed(msg, 800)

        }

        //雷达
        binding.btOpenRadar.setOnClickListener {
            updataLogInfo("click 开启雷达")
            PerceptionManager.get().getRadarManager()?.addRadarInformationListener(this)
        }

        binding.btCloseRadar.setOnClickListener {
            updataLogInfo("click 关闭雷达")
            PerceptionManager.get().getRadarManager()?.removeRadarInformationListener(this)
        }

        binding.tvClearLog.setOnClickListener {
            logList.clear()
            binding.tvLogInfo.text = ""
        }
    }

    /**
     * 设置刹停距离
     */
    private fun setAllBreakDistance(distance: Int) {
        if (!isConnected) {
            updataLogInfo("setAllBreakDistance 飞机未连接")
            return
        }
        updataLogInfo("setAllBreakDistance distance:$distance")
        PerceptionManager.get().setObstacleAvoidanceBrakingDistance(distance = distance.toDouble(),
            direction = PerceptionDirection.UPWARD,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    updataLogInfo("setAllBreakDistance direction[UPWARD] success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    updataLogInfo("setAllBreakDistance direction[UPWARD] onFailure:[code:$code,msg:$msg]")
                }

            })

        PerceptionManager.get().setObstacleAvoidanceBrakingDistance(distance = distance.toDouble(),
            direction = PerceptionDirection.DOWNWARD,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    updataLogInfo("setAllBreakDistance direction[DOWNWARD] success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    updataLogInfo("setAllBreakDistance direction[DOWNWARD] onFailure:[code:$code,msg:$msg]")
                }

            })

        PerceptionManager.get().setObstacleAvoidanceBrakingDistance(distance = distance.toDouble(),
            direction = PerceptionDirection.HORIZONTAL,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    updataLogInfo("setAllBreakDistance direction[HORIZONTAL] success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    updataLogInfo("setAllBreakDistance direction[HORIZONTAL] onFailure:[code:$code,msg:$msg]")
                }

            })
    }

    private fun updateBreakDistanceRange(maxValue: Int) {
        if (!isConnected) {
            updataLogInfo("updateBreakDistance 飞机未连接")
            return
        }
        binding.seekbarBreakDistance.valueFrom = 1.0f
        binding.tvBreakDistanceStart.text = "1"

        if (maxValue > 7) {
            binding.seekbarBreakDistance.valueTo = 7f
            binding.tvBreakDistanceEnd.text = "7"
            binding.tvBreakDistanceTitle.text =
                getString(R.string.common_text_safety_distance, "1", "7")
        } else {
            if (maxValue>1){
                binding.seekbarBreakDistance.valueTo = maxValue.toFloat()
                binding.tvBreakDistanceEnd.text = maxValue.toString()
                binding.tvBreakDistanceTitle.text =
                    getString(R.string.common_text_safety_distance, "1", maxValue.toString())
                if (maxValue <= breakDistance) {
                    binding.seekbarBreakDistance.value = maxValue.toFloat()
                }
            }

        }

        binding.layoutBreakDistance.isVisible = isConnected
    }


    /**
     * 获取避障刹停距离
     */
    private fun getBreakDistance() {
        if (!isConnected) {
            updataLogInfo("getBreakDistance 飞机未连接")
            return
        }

        PerceptionManager.get()
            .getObstacleAvoidanceBrakingDistance(direction = PerceptionDirection.HORIZONTAL,
                object : CommonCallbacks.CompletionCallbackWithParam<Double> {
                    override fun onSuccess(t: Double?) {
                        updataLogInfo("getBreakDistance success distance:$t")
                        binding.seekbarBreakDistance.value = t?.toFloat() ?: 1.0f
                        binding.tvBreakDistanceValue.text = t?.toInt().toString()
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {
                        updataLogInfo("getBreakDistance onFailure:[code:$error,msg:$msg]")
                    }

                })
    }

    /**
     * 获取当前告警距离
     */
    private fun getWarningDistance() {
        if (!isConnected) {
            updataLogInfo("getWarningDistance 飞机未连接")
            return
        }

        PerceptionManager.get()
            .getObstacleAvoidanceWarningDistance(direction = PerceptionDirection.HORIZONTAL,
                object : CommonCallbacks.CompletionCallbackWithParam<Double> {
                    override fun onSuccess(t: Double?) {
                        updataLogInfo("getWarningDistance success, distance:$t")
                        binding.tvWarningDistanceValue.text = t?.toInt().toString()
                        binding.seekbarWarningDistance.value = t?.toFloat() ?: 1f
                        updateBreakDistanceRange(t?.toInt() ?: 7)
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {
                        updataLogInfo("getWarningDistance onFailure:[code:$error,msg:$msg]")
                    }

                })
    }

    /**
     * 设置告警距离
     * 目前推荐上下水平一起进行设置
     */
    private fun setAllWarningDistance(distance: Int) {
        if (!isConnected) {
            updataLogInfo("setAllWarningDistance 飞机未连接")
            return
        }
        updataLogInfo("setAllWarningDistance distance:$distance")
        PerceptionManager.get().setObstacleAvoidanceWarningDistance(distance = distance.toDouble(),
            direction = PerceptionDirection.UPWARD,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    updataLogInfo("setAllWarningDistance direction[UPWARD] success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    updataLogInfo("setAllWarningDistance direction[UPWARD] onFailure:[code:$code,msg:$msg]")
                }

            })

        PerceptionManager.get().setObstacleAvoidanceWarningDistance(distance = distance.toDouble(),
            direction = PerceptionDirection.DOWNWARD,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    updataLogInfo("setAllWarningDistance direction[DOWNWARD] success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    updataLogInfo("setAllWarningDistance direction[DOWNWARD] onFailure:[code:$code,msg:$msg]")
                }

            })

        PerceptionManager.get().setObstacleAvoidanceWarningDistance(distance = distance.toDouble(),
            direction = PerceptionDirection.HORIZONTAL,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    updataLogInfo("setAllWarningDistance direction[HORIZONTAL] success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    updataLogInfo("setAllWarningDistance direction[HORIZONTAL] onFailure:[code:$code,msg:$msg]")
                }

            })
    }

    /**
     * 设置避障类型
     */
    private fun changeObstacleAvoidanceType(type: ObstacleAvoidActionEnum) {
        if (!isConnected) {
            updataLogInfo("changeObstacleAvoidanceType 飞机未连接")
            return
        }

        PerceptionManager.get()
            .setObstacleAvoidanceType(type, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    updataLogInfo("changeObstacleAvoidanceType[${type.name}] success")
                    updateObstacleAvoidanceType(type)
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    updataLogInfo("changeObstacleAvoidanceType onFailure:[code:$code,msg:$msg]")
                    updateObstacleAvoidanceType(type, enable = false)
                }
            })
    }

    /**
     * 获取飞机避障行为类型
     */
    private fun getObstacleAvoidanceType() {
        if (!isConnected) {
            updataLogInfo("getObstacleAvoidanceType 飞机未连接")
            return
        }
        PerceptionManager.get().getObstacleAvoidanceType(object :
            CommonCallbacks.CompletionCallbackWithParam<ObstacleAvoidActionEnum> {
            override fun onSuccess(t: ObstacleAvoidActionEnum?) {
                updataLogInfo("getObstacleAvoidanceType onSuccess")
                t?.let {
                    updateObstacleAvoidanceType(it)
                }
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                updataLogInfo("getObstacleAvoidanceType onFailure:[code:$error,msg:$msg]")
            }

        })
    }

    /**
     * 更新避障类型
     */
    private fun updateObstacleAvoidanceType(type: ObstacleAvoidActionEnum, enable: Boolean = true) {
        when (type) {
            ObstacleAvoidActionEnum.CLOSE -> binding.rbObstacleClose.isChecked = enable
            ObstacleAvoidActionEnum.STOP -> binding.rbObstacleBreak.isChecked = enable
            ObstacleAvoidActionEnum.BYPASS -> binding.rbObstacleBypass.isChecked = enable
        }
    }

    private fun updataLogInfo(msg: String) {
        logList.add(msg)

        val sb = StringBuilder()
        logList.forEach {
            sb.append(it).append("\n")
        }
        binding.tvLogInfo.text = sb.toString()
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        super.onDroneChangedListener(connected, drone)
        isConnected = connected
        updataLogInfo("onDroneChangedListener connected:$connected")
        if (isConnected) {
            getObstacleAvoidanceType()
            getWarningDistance()

            getBreakDistance()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SDKManager.get().getDeviceManager().removeDroneListener(this)
        PerceptionManager.get().getRadarManager()?.destroy()
    }

    override fun onValueChange(
        oldValue: List<VisionRadarInfoBean>?,
        newValue: List<VisionRadarInfoBean>
    ) {
        updataLogInfo("接收到雷达数据：$newValue")
    }

}