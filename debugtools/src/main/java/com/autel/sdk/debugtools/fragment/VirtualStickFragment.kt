package com.autel.sdk.debugtools.fragment


import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.NestModelManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.converter.SingleValueConverter
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightMissionKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.AutelKeyInfo
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.VideoTransMissionModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.VirtualControlMessageBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RcOperateModeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentVirtualStickBinding
import com.autel.sdk.debugtools.listener.OnItemSelectedListener
import com.autel.sdk.debugtools.listener.OnSeekBarChangeListener
import com.autel.sdk.debugtools.view.virtualstick.JoystickView
import com.autel.sdk.debugtools.view.virtualstick.OnJoystickListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 虚拟摇杆示例
 */
class VirtualStickFragment : AutelFragment() {

    private var leftPlayer: AutelPlayer? = null
    private lateinit var leftVideoView: AutelPlayerView

    private val deviation: Double = 0.02
    private var currentLevel: GearLevelEnum = GearLevelEnum.UNKNOWN
    private var stickMode = RcOperateModeEnum.AMERICA_HAND
    private var binding: FragmentVirtualStickBinding? = null

    private var lensType = LensTypeEnum.Zoom

    override val handler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            1 -> {
                updateLocationInfo()
            }
        }
        return@Handler true
    }

    /**
     * 舒适档：满杆量（水平速度）前后10，左右10，（垂直速度）上升5，下降4. 偏航 90度
     * 标准档：满杆量（水平速度）前后15，左右10，（垂直速度）上升6，下降6.  偏航 90度
     * 狂暴档：满杆量（水平速度）前23后18，左右20，（垂直速度）上升6，下降6. 偏航 120度
     * 低速档：满杆量（水平速度）前3后3，左右3，（垂直速度）上升3，下降3. 偏航 90度
     */
    private val speedLevel = arrayOf(
        arrayOf(10, 10, 10, 10, 5, 4, 90),
        arrayOf(15, 15, 10, 10, 6, 6, 90),
        arrayOf(23, 18, 20, 20, 6, 6, 120),
        arrayOf(3, 3, 3, 3, 3, 3, 90)
    )

    private val stickValue = arrayOf(0f, 0f, 0f, 0f)

    private val joystickUpdate: MutableStateFlow<Long> = MutableStateFlow(0)

    private var fullLevelInfo: Array<String>? = null

    private val scope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val ioDispatcher = Dispatchers.IO + SupervisorJob()

    private var sequence: Int = 1
    private var keyManager: IKeyManager? = null
    private val keyVirtualRCControl = KeyTools.createKey(FlightMissionKey.KeyUAVVirtualRCControl)
    companion object {
        private const val TAG = "VirtualStickFragment"
    }

    private var isNewVersion = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVirtualStickBinding.inflate(inflater)
        isNewVersion = arguments?.getBoolean("isNewVersion") ?: false
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        SDKLog.i(TAG, "initView...")
//        setFloatValue(FightParamKey.SIM_LON, -122.3321f) //113.99777f
//        setFloatValue(FightParamKey.SIM_LAT, 47.6062f) //22.595598f
        initVideo()

        fullLevelInfo = resources.getStringArray(R.array.debug_stick_level_speed)

        getGearLevel()
        getStickMode()

        binding?.btnStartMotor?.setOnClickListener {
            keyManager?.performAction(
                KeyTools.createKey(FlightControlKey.KeyStartStopMotor),
                true,
                object: CommonCallbacks.CompletionCallbackWithParam<Void> {
                    override fun onSuccess(t: Void?) {
                        SDKLog.i(TAG, "start motor success")
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {
                        SDKLog.i(TAG, "start motor fail: $error $msg")
                    }
                })
        }

        binding?.layoutGimbal?.isVisible = isNewVersion
        if (isNewVersion) {
            binding?.levelFullInfo?.visibility = View.INVISIBLE
            binding?.tvGearLevel?.visibility = View.INVISIBLE
            binding?.spinner?.visibility = View.INVISIBLE

            binding?.sbGimbalPitch?.progress = 100
            binding?.sbGimbalPitch?.setOnSeekBarChangeListener(object: OnSeekBarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        if (sequence > 1000000) sequence = 1 else sequence++
                        val bean = VirtualControlMessageBean(
                            sequence = sequence,
                            gimbalPitch = progress - 100
                        )

                        SDKLog.d(TAG, "sendVirtualJoystickData2: $bean")
                        keyManager =  keyManager ?: DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
                        keyManager?.setFrequencyReport(keyVirtualRCControl, bean)
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress = 100
                }
            })
        }

        //摇杆
        binding?.leftStickView?.setJoystickListener(object : OnJoystickListener {
            override fun onTouch(joystick: JoystickView?, pX: Float, pY: Float) {
                stickValue[0] = 0F
                stickValue[1] = 0F
                if (abs(pX) >= deviation) {
                    stickValue[0] = pX
                }
                if (abs(pY) >= deviation) {
                    stickValue[1] = pY
                }
                joystickUpdate.tryEmit(System.currentTimeMillis())
                SDKLog.d(TAG, "leftStickView: ${stickValue[0]}, ${stickValue[1]}")
            }
        })

        binding?.rightStickView?.setJoystickListener(object : OnJoystickListener {
            override fun onTouch(joystick: JoystickView?, pX: Float, pY: Float) {
                stickValue[2] = 0F
                stickValue[3] = 0F
                if (abs(pX) >= deviation) {
                    stickValue[2] = pX
                }
                if (abs(pY) >= deviation) {
                    stickValue[3] = pY
                }
                joystickUpdate.tryEmit(System.currentTimeMillis())
                SDKLog.d(TAG, "rightStickView: ${stickValue[2]}, ${stickValue[3]}")
            }
        })

        scope.launch(ioDispatcher + CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }) {
            joystickUpdate.collect {
                SDKLog.d(TAG, "collect: ${stickValue.joinToString(",")}")
                if (isNewVersion) {
                    sendVirtualJoystickData2(stickValue[0], stickValue[1], stickValue[2], stickValue[3])
                } else {
                    sendVirtualJoystickData(stickValue[0], stickValue[1], stickValue[2], stickValue[3])
                }
            }
        }
        handler.sendEmptyMessageDelayed(1, 2000L)
    }

    private fun remarkStickMode() {

        val leftStick = getString(R.string.debug_text_controller_model_left_remote)
        val rightStick = getString(R.string.debug_text_controller_model_right_remote)
        val ascend = getString(R.string.debug_text_controller_model_up_title)
        val descend = getString(R.string.debug_text_controller_model_down_title)
        val turnRight = getString(R.string.debug_text_controller_model_turn_right_title)
        val turnLeft = getString(R.string.debug_text_controller_model_turn_left_title)
        val forward = getString(R.string.debug_text_controller_model_forward_title)
        val backward = getString(R.string.debug_text_controller_model_back_title)
        val goRight = getString(R.string.debug_text_controller_model_right_title)
        val goLeft = getString(R.string.debug_text_controller_model_left_title)

        val remark = when(stickMode) {
            RcOperateModeEnum.AMERICA_HAND -> {
                "$leftStick X: $turnLeft/$turnRight ; Y: $ascend/$descend; $rightStick X: $goLeft/$goRight Y: $forward/$backward"
            }
            RcOperateModeEnum.JAPANESE_HAND -> {
                "$leftStick X: $turnLeft/$turnRight ; Y: $forward/$backward; $rightStick X: $goLeft/$goRight Y: $ascend/$descend"
            }
            RcOperateModeEnum.CHINESE_HAND -> {
                "$leftStick X: $goLeft/$goRight ; Y: $forward/$backward; $rightStick X: $turnLeft/$turnRight Y: $ascend/$descend"
            }
        }
        binding?.tvStickModeRemark?.text = remark
    }

    private fun getGearLevel() {
        keyManager =  keyManager ?: DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
        keyManager?.getValue(
            KeyTools.createKey(FlightPropertyKey.KeyGearLever),
            object: CommonCallbacks.CompletionCallbackWithParam<GearLevelEnum> {
                override fun onSuccess(t: GearLevelEnum?) {
                    SDKLog.i(TAG, "get KeyGearLever onSuccess: $t")
                    currentLevel = if (t == null || t == GearLevelEnum.UNKNOWN) GearLevelEnum.NORMAL else t
                    handler.post { initGearLevel() }
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.i(TAG, "get KeyGearLever onFailure: $error $msg")
                    currentLevel = GearLevelEnum.NORMAL
                    handler.post { initGearLevel() }
                }
            }
        )
    }

    private fun getStickMode() {
        val remoteKeyManager = DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager()
        remoteKeyManager.getValue(
            KeyTools.createKey(RemoteControllerKey.KeyRCRockerControlMode),
            object: CommonCallbacks.CompletionCallbackWithParam<RcOperateModeEnum> {
                override fun onSuccess(t: RcOperateModeEnum?) {
                    SDKLog.i(TAG, "get KeyRCRockerControlMode onSuccess: $t")
                    stickMode = t ?: RcOperateModeEnum.AMERICA_HAND
                    handler.post { initStickMode() }
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.i(TAG, "get KeyRCRockerControlMode onFailure: $error $msg")
                    stickMode = RcOperateModeEnum.AMERICA_HAND
                    handler.post { initStickMode() }
                }
            }
        )
    }

    private fun initGearLevel() {
        binding?.spinner?.setSelection(currentLevel.value - 1)
        binding?.levelFullInfo?.text = fullLevelInfo?.get(currentLevel.value - 1) ?: ""
        binding?.spinner?.onItemSelectedListener = object : OnItemSelectedListener() {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != currentLevel.value - 1) {
                    setGearLevel(position)
                }
            }
        }
    }

    private fun initStickMode() {
        remarkStickMode()
        binding?.spinnerMode?.setSelection(stickMode.value)
        binding?.spinnerMode?.onItemSelectedListener = object : OnItemSelectedListener() {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                SDKLog.i(TAG, "onItemSelected: $position  $stickMode")
                if (stickMode.value != position) {
                    setStickMode(position)
                }
            }
        }
    }

    private fun setStickMode(position: Int) {
        val value =  RcOperateModeEnum.values()[position]
        val remoteKeyManager = DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager()
        remoteKeyManager.setValue(
            KeyTools.createKey(RemoteControllerKey.KeyRCRockerControlMode),
            value,
            object: CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    stickMode = value
                    handler.post { remarkStickMode() }
                    SDKLog.i(TAG, "set KeyRCRockerControlMode onSuccess: $stickMode")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    SDKLog.e(TAG, "set KeyRCRockerControlMode onFailure: $code $msg")
                    handler.post {
                        binding?.spinnerMode?.setSelection(stickMode.value)
                        Toast.makeText(requireContext(), "onFailure: $code $msg", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun setGearLevel(position: Int) {
        val level = when(position) {
            3 -> GearLevelEnum.LOW_SPEED
            0 -> GearLevelEnum.SMOOTH
            1 -> GearLevelEnum.NORMAL
            2 -> GearLevelEnum.SPORT
            else -> GearLevelEnum.UNKNOWN
        }
        if (level == GearLevelEnum.UNKNOWN) return

        keyManager =  keyManager ?: DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
        keyManager?.setValue(
            KeyTools.createKey(FlightPropertyKey.KeyGearLever),
            level,
            object: CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    SDKLog.i(TAG, "set KeyGearLever onSuccess: $level")
                    currentLevel = level
                    handler.post {
                        binding?.levelFullInfo?.text = fullLevelInfo?.get(currentLevel.value - 1) ?: ""
                    }
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    SDKLog.e(TAG, "set KeyGearLever onFailure: $code $msg")
                    handler.post {
                        binding?.spinner?.setSelection(currentLevel.value - 1)
                        Toast.makeText(requireContext(), "onFailure: $code $msg", Toast.LENGTH_SHORT).show()
                    }
                }
        })
    }


    @SuppressLint("DefaultLocale")
    private fun sendVirtualJoystickData2(leftX: Float, leftY: Float, rightX: Float, rightY: Float) {

        //左转/右转, 上升/下降, 向左/向右, 前进/后退
        val values = dealWithControlMode(leftX, leftY, rightX, rightY)
        if (sequence > 1000000) sequence = 1 else sequence++
        val bean = VirtualControlMessageBean(
            sequence = sequence,
            turnYaw = (values[0] * 100f).toInt(),
            forwardOrBackward = (values[3] * 100f).toInt(),
            raiseOrFall = (values[1] * 100f).toInt(),
            leftOrRight = (values[2] * 100f).toInt(),
            gimbalPitch = 0
        )

        handler.post {
            binding?.dataSend?.text = String.format("upOrDown: %d\nturnLeftOrRight: %d\nforwardOrBackward: %d\ngoLeftOrRight: %d",
                bean.raiseOrFall, bean.turnYaw, bean.forwardOrBackward, bean.leftOrRight)
        }

        SDKLog.i(TAG, "sendVirtualJoystickData2: $bean")
        keyManager =  keyManager ?: DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
        keyManager?.setFrequencyReport(keyVirtualRCControl, bean)
    }

    @SuppressLint("DefaultLocale")
    private fun sendVirtualJoystickData(leftX: Float, leftY: Float, rightX: Float, rightY: Float) {
        if (currentLevel == GearLevelEnum.UNKNOWN) return
        //左转/右转, 上升/下降, 向左/向右, 前进/后退
        val index = currentLevel.value - 1
        val values = dealWithControlMode(leftX, leftY, rightX, rightY)
        val raiseOrDownValue = if (values[1] > 0) values[1] * speedLevel[index][4] else values[1] * speedLevel[index][5]
        val turnYawValue = values[0] * speedLevel[index][6]
        val forwardOrBackwardValue = if (values[3] > 0) values[3] * speedLevel[index][0] else values[3] * speedLevel[index][1]
        val leftOrRightValue = if (values[2] > 0) values[2] * speedLevel[index][2] else values[2] * speedLevel[index][3]

        handler.post {
            binding?.dataSend?.text = String.format("upOrDown: %.4f\nturnLeftOrRight: %.2f\nforwardOrBackward: %.4f\ngoLeftOrRight: %.4f",
                raiseOrDownValue, turnYawValue, forwardOrBackwardValue, leftOrRightValue)
        }

        NestModelManager.getInstance().updateVirtualJoystickByAtService(
            raiseOrDownValue.toInt(),
            turnYawValue.toInt(),
            forwardOrBackwardValue.toInt(),
            leftOrRightValue.toInt(),
            0
        )
    }

    /**
     * 跟据不同的操控模式处理：左转/右转(yaw), 上升/下降(pitch), 向左/向右(roll), 前进/后退(thrust)
     */
    private fun dealWithControlMode(leftX: Float, leftY: Float, rightX: Float, rightY: Float): List<Float> {
        return when(stickMode) {
            RcOperateModeEnum.AMERICA_HAND -> listOf(leftX, leftY, rightX, rightY)
            RcOperateModeEnum.CHINESE_HAND -> listOf(rightX, rightY, leftX, leftY)
            RcOperateModeEnum.JAPANESE_HAND -> listOf(leftX, rightY, rightX, leftY)
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updateLocationInfo() {
        val device = DeviceManager.getDeviceManager().getFirstDroneDevice()
        if (device == null) {
            handler.sendEmptyMessageDelayed(1, 5000L)
            return
        }

        val stateData = device.getDeviceStateData()
        val altitude = stateData.flightControlData.altitude
        val distance = stateData.flightControlData.distance
        val xSpeed = stateData.flightControlData.velocityX
        val ySpeed = stateData.flightControlData.velocityY
        val zSpeed = stateData.flightControlData.velocityZ
        val speed = sqrt((xSpeed * xSpeed + ySpeed * ySpeed)) * 10 / 10f

        //val startAngle = roll + pitch
        //val endAngle = 180f + roll - pitch * 2
        val dronePitch = stateData.flightControlData.droneAttitudePitch //俯仰角
        val droneRoll = stateData.flightControlData.droneAttitudeRoll   //翻滚角/横滚角
        val droneYam = stateData.flightControlData.droneAttitudeYaw     //偏航角

        val gimbalPitch = stateData.flightControlData.gimbalAttitudePitch
        val gimbalRoll = stateData.flightControlData.gimbalAttitudeRoll
        val gimbalYaw = stateData.flightControlData.gimbalAttitudeYaw

        val latitude = stateData.flightControlData.droneLatitude
        val longitude = stateData.flightControlData.droneLongitude

        val info = String.format("latitude: %.08f, longitude: %.08f, altitude: %.02f\n" +
                "distance: %.02f, hSpeed: %.02f, vSpeed: %.02f\n" +
                "dronePitch: %.04f, droneRoll: %.04f, droneYam: %.04f\n" +
                "gimbalPitch: %.02f, gimbalRoll: %.02f, gimbalYaw: %.02f",
            latitude, longitude, altitude, distance, speed, zSpeed, dronePitch, droneRoll, droneYam, gimbalPitch, gimbalRoll, gimbalYaw)

        binding?.locationInfo?.text = info
        handler.sendEmptyMessageDelayed(1, 1000)
    }
    private fun setFloatValue(keyName: String, value: Float) {
        val keyInfo: AutelKeyInfo<Float> = AutelKeyInfo(0, keyName, SingleValueConverter.FloatConverter).canGet(true).canSet(true)
        val autelKey = KeyTools.createKey(keyInfo)
        DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()?.setValue(autelKey,
            value, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                }
                override fun onFailure(code: IAutelCode, msg: String?) {
                    ToastUtils.showToast("${code.code}$msg")
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        leftPlayer?.removeVideoView()
        leftPlayer?.releasePlayer()
    }

    private fun initVideo() {

        val droneDevice = DeviceManager.getDeviceManager().getFirstDroneDevice() ?: return
        droneDevice.getKeyManager().setValue(
            KeyTools.createKey(AirLinkKey.KeyALinkTransmissionMode),
            VideoTransMissionModeEnum.HIGH_QUALITY, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    SDKLog.e(TAG, "onFailure: code = $code, msg = $msg")
                }
            }
        )

        leftVideoView = createAutelCodecView()
        binding?.leftVideoLayout?.addView(leftVideoView)
        leftPlayer = AutelPlayer(getChannelId())
        leftPlayer?.addVideoView(leftVideoView)

        val gimbalType = droneDevice.getGimbalDeviceType()
        val lens = droneDevice.getCameraAbilitySetManger().getLensList(gimbalType)
        val lensName = lens?.map { it.value } ?: listOf()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, lensName)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinnerLens?.adapter = adapter
        var index = lens?.indexOf(lensType) ?: -1
        if (index == -1) index = 0
        binding?.spinnerLens?.setSelection(index)
        binding?.spinnerLens?.onItemSelectedListener = object: OnItemSelectedListener() {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val posType = lens?.get(position) ?: LensTypeEnum.Zoom
                if (lensType != posType) {
                    lensType = posType
                    switchVideoSource()
                }
            }
        }
    }

    private fun switchVideoSource() {

        leftPlayer?.removeVideoView()

        leftPlayer = AutelPlayer(getChannelId())
        leftPlayer?.addVideoView(leftVideoView)
    }

    private fun getChannelId(): Int {
        return when(lensType) {
            LensTypeEnum.Zoom, LensTypeEnum.TeleZoom -> SDKConstants.getZoomChancelId()
            LensTypeEnum.WideAngle -> SDKConstants.getWideAngleChannelId()
            LensTypeEnum.Thermal, LensTypeEnum.TeleThermal -> SDKConstants.getInfraredChannelId()
            LensTypeEnum.NightVision -> SDKConstants.getNightVisionChannelId()
            else -> SDKConstants.getZoomChancelId()
        }
    }

    private fun createAutelCodecView(): AutelPlayerView {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }
}
