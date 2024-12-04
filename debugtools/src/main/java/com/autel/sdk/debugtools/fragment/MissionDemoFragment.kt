package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IMissionManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionKmlGUIDBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionWaypointStatusReportNtfyBean
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentMissionDemoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Mission Sample
 *
 */
class MissionDemoFragment : AutelFragment() {
    companion object {
        private const val TAG = "MissionDemoFragment"
    }

    private var binding: FragmentMissionDemoBinding? = null
    private val handler = Handler(Looper.getMainLooper())
    private var guid: Int? = null

    private val logList: MutableList<String> = arrayListOf()

    private val listener =
        object : CommonCallbacks.KeyListener<MissionWaypointStatusReportNtfyBean> {
            override fun onValueChange(
                oldValue: MissionWaypointStatusReportNtfyBean?,
                newValue: MissionWaypointStatusReportNtfyBean
            ) {
                updateLogInfo(">> ${getString(R.string.mission_status_listener)}: ${newValue.toString()}")
                SDKLog.i(TAG, "$newValue")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMissionDemoBinding.inflate(inflater)
        initView()
        startListenerMissionStatus()
        return binding?.root
    }

    private fun initView() {
        binding?.btnUpload?.setOnClickListener {
            updateLogInfo(">> btnUpload click")
            if (!checkDeviceReady()) {
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val kmzPath = copyAssertToSdcard()
                uploadMission(kmzPath)
            }
        }

        binding?.btnStart?.setOnClickListener {
            updateLogInfo(">> btnStart click")
            if (!checkDeviceReady()) {
                return@setOnClickListener
            }
            startMission()
        }

        binding?.btnPause?.setOnClickListener {
            updateLogInfo(">> btnPause click")
            if (!checkDeviceReady()) {
                return@setOnClickListener
            }
            pauseMission()
        }

        binding?.btnResume?.setOnClickListener {
            updateLogInfo(">> btnResume click")
            if (!checkDeviceReady()) {
                return@setOnClickListener
            }
            resumeMission()
        }

        binding?.btnExit?.setOnClickListener {
            updateLogInfo(">> btnExit click")
            if (!checkDeviceReady()) {
                return@setOnClickListener
            }
            exitMission()
        }

        binding?.btnClearLog?.setOnClickListener {
            clearLogHistory()
        }
    }

    private fun checkDeviceReady(): Boolean {
        if (!isConnected()) {
            ToastUtils.showToast(getString(R.string.drone_disconnected))
            return false
        }
        return true
    }

    private fun startListenerMissionStatus() {
        updateLogInfo(">> ${getString(R.string.mission_start_listener)}")
        getMissionManger()?.addWaypointMissionExecuteStateListener(listener)
    }

    private fun stopListenerMissionStatus() {
        updateLogInfo(">> ${getString(R.string.mission_stop_listener)}")
        getMissionManger()?.removeWaypointMissionExecuteStateListener(listener)
    }

    private fun uploadMission(kmzPath: String) {
        enableOrDisableBtn(binding?.btnUpload, false)
        guid = (System.currentTimeMillis() / 1000).toInt()
        getMissionManger()?.uploadKmzMissionFile(
            kmzPath,
            guid!!,
            object : CommonCallbacks.CompletionCallbackWithProgressAndParam<Long> {
                override fun onFailure(error: IAutelCode, msg: String?) {
                    updateLogInfo(
                        "${getString(R.string.upload_mission_fail)}:[code:${error.code};msg:$msg]",
                        isError = true
                    )
                    enableOrDisableBtn(binding?.btnUpload, true)
                }

                override fun onProgressUpdate(progress: Double) {
                    updateLogInfo("${getString(R.string.upload_mission_progress)}:$progress&percent:${progress * 100}%")
                }

                override fun onSuccess(t: Long?) {
                    updateLogInfo("${getString(R.string.upload_mission_success)} guid=$t")
                    enableOrDisableBtn(binding?.btnUpload, true)
                }
            })
    }

    private fun startMission() {
        enableOrDisableBtn(binding?.btnStart, false)
        guid?.let {
            getMissionManger()?.startMission(
                MissionKmlGUIDBean(it),
                object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                    override fun onFailure(error: IAutelCode, msg: String?) {
                        updateLogInfo(
                            "start mission fail:[code:${error.code};msg:$msg]",
                            isError = true
                        )
                        enableOrDisableBtn(binding?.btnStart, true)
                    }

                    override fun onSuccess(t: Void?) {
                        updateLogInfo("start mission success")
                        enableOrDisableBtn(binding?.btnStart, true)
                    }
                })
        }
    }

    private fun pauseMission() {
        enableOrDisableBtn(binding?.btnPause, false)
        guid?.let {
            getMissionManger()?.pauseMission(object :
                CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onFailure(error: IAutelCode, msg: String?) {
                    updateLogInfo(
                        "pause mission fail:[code:${error.code};msg:$msg]",
                        isError = true
                    )
                    enableOrDisableBtn(binding?.btnPause, true)
                }

                override fun onSuccess(t: Void?) {
                    updateLogInfo("pause mission success")
                    enableOrDisableBtn(binding?.btnPause, true)
                }
            }, isKml = true)
        }
    }

    private fun resumeMission() {
        enableOrDisableBtn(binding?.btnResume, false)
        guid?.let {
            getMissionManger()?.resumeMission(
                MissionKmlGUIDBean(it),
                object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                    override fun onFailure(error: IAutelCode, msg: String?) {
                        updateLogInfo("resume mission fail:[code:${error.code};msg:$msg]",isError = true)
                        enableOrDisableBtn(binding?.btnResume, true)
                    }

                    override fun onSuccess(t: Void?) {
                        updateLogInfo("resume mission success")
                        enableOrDisableBtn(binding?.btnResume, true)
                    }
                })
        }
    }


    private fun exitMission() {
        enableOrDisableBtn(binding?.btnExit, false)
        guid?.let {
            getMissionManger()?.exitMission(object :
                CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onFailure(error: IAutelCode, msg: String?) {
                    updateLogInfo("exit mission fail:[code:${error.code};msg:$msg]",isError = true)
                    enableOrDisableBtn(binding?.btnExit, true)
                }

                override fun onSuccess(t: Void?) {
                    updateLogInfo("exit mission success")
                    enableOrDisableBtn(binding?.btnExit, true)
                }
            }, isKml = true)
        }
    }

    /**
     * single controlled drone or first controlled drone
     * also can upload /control one by one for all drones
     */
    private fun getCurrentDroneDevice(): IAutelDroneDevice? {
        return DeviceManager.getMultiDeviceOperator().getControlledDroneList().firstOrNull()
    }

    private fun getMissionManger(): IMissionManager? {
        return getCurrentDroneDevice()?.getWayPointMissionManager()
    }

    private fun isConnected(): Boolean {
        return getCurrentDroneDevice()?.isConnected() == true
    }

    /**
     * 更新日志
     */
    private fun updateLogInfo(log: String, isError: Boolean = false) {
        logList.add(log)

        if (isError) {
            SDKLog.e(TAG, log)
        } else {
            SDKLog.d(TAG, log)
        }

        val stringBuffer = StringBuffer()
        logList.forEach {
            stringBuffer.append(it).append("\n")
        }

        handler.post {
            binding?.tvLogInfo?.text = stringBuffer.toString()
        }

    }

    private fun clearLogHistory(){
        if (logList.isNotEmpty())
            logList.clear()

        handler.post {
            binding?.tvLogInfo?.text = ""
        }
    }

    private fun copyAssertToSdcard(): String {
        val sourceFilePath = "test.kmz"
        val targetDir = "${context?.externalCacheDir}/mission_test/"
        val dir = File(targetDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        SDKLog.i(TAG, "targetDir=$targetDir")
        val targetFilePath = targetDir + sourceFilePath
        if (File(targetFilePath).exists()) {
            SDKLog.i(TAG, "targetDir already exist")
            return targetFilePath
        }

        try {
            val inputStream: InputStream = requireActivity().assets.open(sourceFilePath)
            val outputStream: OutputStream = FileOutputStream(targetFilePath)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return targetFilePath
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListenerMissionStatus()
    }

    private fun enableOrDisableBtn(btn: Button?, enable: Boolean) {
        handler.post { btn?.isEnabled = enable }
    }

}
