package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AIServiceKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AITrackingKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.AIDetectConfigBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.DetectTrackNotifyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.AiDetectSceneTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.DetectTargetEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentAiRecognizeBinding
import com.autel.sdk.debugtools.uploadMsg.FlightControlUploadMsgManager

class AIRecognizeFragment : AutelFragment() {

    companion object {
        private const val TAG = "AIRecognizeFragment"
    }

    private lateinit var binding: FragmentAiRecognizeBinding
    private var isRunning = false

    private val trackListener = object : CommonCallbacks.KeyListener<DetectTrackNotifyBean> {
        override fun onValueChange(oldValue: DetectTrackNotifyBean?, newValue: DetectTrackNotifyBean) {
            val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
            val builder = StringBuilder()
            if (drone?.getDeviceStateData()?.flightControlData?.aiEnableFunc == AiServiceStatueEnum.AI_RECOGNITION) {
                builder.append("timestamp: ${newValue.timestamp} lensId:${newValue.lensId} objNum:${newValue.objNum}\n")
                newValue.infoList.forEach {
                    val type = DetectTargetEnum.getEnglishName(DetectTargetEnum.findEnum(it.type))
                    builder.append("Type:$type status:${it.status} objectId: ${it.objectId} position:(${it.startX}, ${it.startY}, ${it.width}, ${it.height})\n")
                }
            }
            binding.tvResult.text = builder.toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAiRecognizeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStartRecognize.setOnClickListener {
            if (isRunning) {
                stopRecognize()
                binding.btnStartRecognize.text = getString(R.string.debug_recognition_start)
                isRunning = false
            } else {
                startRecognize()
                binding.btnStartRecognize.text = getString(R.string.debug_recognition_stop)
                isRunning = true
            }
        }
    }

    private fun startRecognize() {
        val listenKey = KeyTools.createKey(AIServiceKey.KeyAiDetectTarget)
        val key = KeyTools.createKey(AITrackingKey.KeySecurityIntelligentDetectEnter)

        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
        //LensType IR or Visible
        val lens = drone?.getAbilitySetManager()?.getCameraAbilitySetManager()?.getLensList(drone.getGimbalDeviceType())
        val lensType = if (binding.rbVisible.isChecked) {
            if (lens?.contains(LensTypeEnum.TeleZoom) == true)  LensTypeEnum.TeleZoom else LensTypeEnum.Zoom
        } else {
            if (lens?.contains(LensTypeEnum.TeleThermal) == true) LensTypeEnum.TeleThermal else LensTypeEnum.Thermal
        }
        val droneLensId = drone?.getAbilitySetManager()?.getCameraAbilitySetManager()?.getLenId(lensType, drone.getGimbalDeviceType())
        val bean = AIDetectConfigBean().apply {

            sceneType = AiDetectSceneTypeEnum.SCENE_TYPE_UNIVERSAL
            //Detect target type
            val targetList = arrayListOf<Int>()
            targetList.add(DetectTargetEnum.ANIMAL.value)
            targetList.add(DetectTargetEnum.BOAT.value)
            targetList.add(DetectTargetEnum.CAR.value)
            targetList.add(DetectTargetEnum.PERSON.value)
            targetList.add(DetectTargetEnum.RIDER.value)
            targetList.add(DetectTargetEnum.VEHICLE.value)

            targetTypeList = targetList
            lensId = droneLensId ?: 0
        }

        //listen the recognition result
        drone?.getKeyManager()?.listen(listenKey, trackListener)
        //start recognition
        drone?.getKeyManager()?.performAction(key, bean, object: CommonCallbacks.CompletionCallbackWithParam<Void> {
            override fun onSuccess(t: Void?) {
                SDKLog.d(TAG, "start recognize success")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                SDKLog.d(TAG, "start recognize failed $error $msg")
            }
        })
    }

    private fun stopRecognize() {
        // Stop recognition
        val listenKey = KeyTools.createKey(AIServiceKey.KeyAiDetectTarget)
        val key = KeyTools.createKey(AITrackingKey.KeySecurityIntelligentDetectExit)
        DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()?.let {
            it.cancelListen(listenKey, trackListener)
            it.performAction(key, callback = object: CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {

                }

                override fun onFailure(error: IAutelCode, msg: String?) {

                }
            })
        }
    }
}