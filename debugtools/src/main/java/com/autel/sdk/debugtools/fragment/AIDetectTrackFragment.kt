package com.autel.sdk.debugtools.fragment

import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AIServiceKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AITrackingKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.AIDetectConfigBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.DetectTrackNotifyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.TrackAreaBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.AiDetectSceneTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.AiLensTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aitrack.TrackTargetRectBean
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentAiDetectTrackingBinding
import com.autel.sdk.debugtools.listener.OnItemSelectedListener
import com.autel.sdk.debugtools.tracking.AITrackingView
import com.autel.sdk.debugtools.tracking.AITrackingView.Companion.TRACK_STATUS_TRACKING
import com.autel.sdk.debugtools.tracking.AITrackingView.Companion.TRACK_STATUS_NONE

class AIDetectTrackFragment : AutelFragment() {

    private lateinit var binding: FragmentAiDetectTrackingBinding
    private var isRunning = false
    private var autelPlayer: AutelPlayer? = null
    private var lensType: LensTypeEnum = LensTypeEnum.Zoom
    private var lensId: Int = 0
    private var droneDevice: IAutelDroneDevice? = null

    private val aiDetectKey = KeyTools.createKey(AIServiceKey.KeyAiDetectTarget)
    private var aiDetectCallBack = object : CommonCallbacks.KeyListener<DetectTrackNotifyBean> {
        override fun onValueChange(oldValue: DetectTrackNotifyBean?, newValue: DetectTrackNotifyBean) {
            if (lensId != newValue.lensId) {
                clearAiView()
                return
            }
            if (newValue.infoList.isNotEmpty()) {
                binding.aiTrackingView.refreshTrackableTarget(newValue)
            }
        }
    }
    private val aiTrackKey = KeyTools.createKey(AIServiceKey.KeyTrackingTargetObject)
    private var aiTrackCallBack = object : CommonCallbacks.KeyListener<TrackAreaBean> {

        override fun onValueChange(oldValue: TrackAreaBean?, newValue: TrackAreaBean) {
            if (lensId != newValue.lensId) {
                clearAiView()
                return
            }
            if (newValue.infoList?.isNotEmpty() == true) {
                binding.aiTrackingView.selectTarget(newValue)
                if (binding.aiTrackingView.trackWorkStatus != TRACK_STATUS_TRACKING) {
                    binding.aiTrackingView.trackWorkStatus = TRACK_STATUS_TRACKING
                } else if (droneDevice?.getDeviceStateData()?.flightControlData?.trackWorkStatus?.value != binding.aiTrackingView.trackWorkStatus) {
                    binding.aiTrackingView.trackWorkStatus = droneDevice?.getDeviceStateData()?.flightControlData?.trackWorkStatus?.value ?: 0
                }
            }
        }
    }

    private var trackViewListener = object : AITrackingView.OnTrackChangeListener {
        override fun onDragSelectTarget(view: AITrackingView, x: Float, y: Float, width: Float, height: Float) {
            SDKLog.d(TAG, "selected: x:$x, $y, $width, $height")
            startLock(RectF(x, y, width, height))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAiDetectTrackingBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        droneDevice = DeviceManager.getDeviceManager().getFirstDroneDevice()

        initLensSpinner()
        initPlayer()
        listenResult()

        binding.btnStartRecognize.setOnClickListener {
            if (isRunning) {
                exitIntelligentLock()
                binding.btnStartRecognize.text = getString(R.string.debug_start)
                isRunning = false
            } else {
                enterIntelligentLock()
                binding.btnStartRecognize.text = getString(R.string.debug_stop)
                isRunning = true
            }
        }
        binding.aiTrackingView.setOnTrackChangeListener(trackViewListener)
    }

    private fun listenResult() {
        droneDevice?.getKeyManager()?.listen(aiDetectKey, aiDetectCallBack)
        droneDevice?.getKeyManager()?.listen(aiTrackKey, aiTrackCallBack)
    }

    private fun startLock(rect: RectF) {
        val createKey = KeyTools.createKey(AITrackingKey.KeyIntelligentLockStart)
        val bean = TrackTargetRectBean(rect.left,rect.top, rect.right, rect.bottom, lensId, AiDetectSceneTypeEnum.SCENE_TYPE_SECURITY)
        droneDevice?.getKeyManager()?.performAction(createKey, bean, object : CommonCallbacks.CompletionCallbackWithParam<Void> {
            override fun onSuccess(t: Void?) {
                SDKLog.i(TAG, "startLock success, rect: $rect lensId: $lensId")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                SDKLog.e(TAG, "startLock failed, rect: $rect lensId: $lensId， err:$msg")

            }
        }) ?: kotlin.run {
            SDKLog.e(TAG, "startLock failed, droneDevice is null")
        }
    }

    private fun initLensSpinner() {
        val cameraAbilitySetManager = droneDevice?.getAbilitySetManager()?.getCameraAbilitySetManager()
        val lens = cameraAbilitySetManager?.getLensList(droneDevice?.getGimbalDeviceType())?.filter {
            cameraAbilitySetManager.getCameraSupport2()?.getAiServiceEnabled(it) == true
        } ?: listOf()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, lens.map { it.value })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLens.adapter = adapter

        lensType = lens.firstOrNull() ?: LensTypeEnum.Zoom
        binding.spinnerLens.setSelection(0)
        binding.spinnerLens.onItemSelectedListener = object: OnItemSelectedListener() {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val posType = lens[position]
                if (lensType != posType) {
                    lensType = posType
                    switchPlaySource()
                }
            }
        }
    }

    private fun initPlayer() {
        lensId = droneDevice?.getAbilitySetManager()?.getCameraAbilitySetManager()
            ?.getLenId(lensType, droneDevice?.getGimbalDeviceType()) ?: 0
        val channelId = getChannelId()
        autelPlayer = AutelPlayer(channelId)
        AutelPlayerManager.getInstance().addAutelPlayer(autelPlayer)
        autelPlayer!!.addVideoView(binding.renderView)
    }

    private fun switchPlaySource() {
        //stop and release player
        autelPlayer?.removeVideoView()
        autelPlayer?.releasePlayer()
        clearAiView()

        //create new player
        lensId = droneDevice?.getAbilitySetManager()?.getCameraAbilitySetManager()
            ?.getLenId(lensType, droneDevice?.getGimbalDeviceType()) ?: 0
        val channelId = getChannelId()
        autelPlayer = AutelPlayer(channelId)
        AutelPlayerManager.getInstance().addAutelPlayer(autelPlayer)
        autelPlayer!!.addVideoView(binding.renderView)
    }

    override fun onDestroy() {
        super.onDestroy()
        droneDevice?.getKeyManager()?.let {
            it.cancelListen(aiDetectKey, aiDetectCallBack)
            it.cancelListen(aiTrackKey, aiTrackCallBack)
        }

        autelPlayer?.removeVideoView()
        autelPlayer?.releasePlayer()
    }

    private fun getChannelId(): Int {
        droneDevice ?: return SDKConstants.getZoomChancelId()
        val lens = droneDevice?.getAbilitySetManager()?.getCameraAbilitySetManager()
            ?.getLensList(droneDevice?.getGimbalDeviceType()) ?: emptyList()
        return when (lensType) {
            LensTypeEnum.Zoom -> if (lens.contains(LensTypeEnum.WideAngle)) SDKConstants.getTelZoomChancelId() else SDKConstants.getZoomChancelId()
            LensTypeEnum.TeleZoom -> SDKConstants.getTelZoomChancelId()
            LensTypeEnum.WideAngle -> SDKConstants.getWideAngleChannelId()
            LensTypeEnum.Thermal, LensTypeEnum.TeleThermal -> SDKConstants.getInfraredChannelId()
            LensTypeEnum.NightVision -> SDKConstants.getNightVisionChannelId()
            else -> SDKConstants.getZoomChancelId()
        }
    }

    private fun enterIntelligentLock() {
        val cameraManager = droneDevice?.getAbilitySetManager()?.getCameraAbilitySetManager()
        //Check if the lens supports AI recognition
        if (cameraManager?.getCameraSupport2()?.getAiServiceEnabled(lensType) != true) {
            SDKLog.e(TAG, "The lens[${lensType.name}] does not support AI recognition")
            showToast("The lens[${lensType.name}] does not support AI recognition")
            return
        }

        val droneLensId = cameraManager.getLenId(lensType, droneDevice?.getGimbalDeviceType())
        val bean = AIDetectConfigBean().apply {
            sceneType = AiDetectSceneTypeEnum.SCENE_TYPE_SECURITY
            targetTypeList = null
            // 1.8.x version
            lensId = droneLensId ?: 0
            //compatible with 1.7.x version and 1.8.x version before 2024/10/15
            aiLensTypeEnum = when {
                lensType.isThermal() -> AiLensTypeEnum.INFRARED
                lensType == LensTypeEnum.NightVision -> AiLensTypeEnum.Night
                else -> AiLensTypeEnum.Visible
            }
        }

        SDKLog.i(TAG, "startRecognize lensType:$lensType lensId:${bean.lensId}")
        val key = KeyTools.createKey(AITrackingKey.KeyIntelligentLockEnter)
        droneDevice?.getKeyManager()?.performAction(key, bean, object : CommonCallbacks.CompletionCallbackWithParam<Void> {
            override fun onSuccess(t: Void?) {
                SDKLog.d(TAG, "start recognize success")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                SDKLog.d(TAG, "start recognize failed $error $msg")
            }
        })
    }

    private fun exitIntelligentLock() {
        val key = KeyTools.createKey(AITrackingKey.KeyIntelligentLockExit)
        droneDevice?.getKeyManager()?.performAction(key, callback = object : CommonCallbacks.CompletionCallbackWithParam<Void> {
            override fun onSuccess(t: Void?) {

            }

            override fun onFailure(error: IAutelCode, msg: String?) {

            }
        })
        handler.postDelayed({ clearAiView() }, 500)
    }

    private fun clearAiView() {
        binding.aiTrackingView.cleanup()
        binding.aiTrackingView.trackWorkStatus = TRACK_STATUS_NONE
    }
}