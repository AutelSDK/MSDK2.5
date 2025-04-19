package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.autel.drone.sdk.SDKConfig
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IMissionManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionKmlGUIDBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionWaypointGUIDBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionWaypointStatusReportNtfyBean
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.internal.mission.v2.MissionInfoJNI
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentMissionDemoBinding
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * 任务示例
 * 1. 本Demo仅用于演示目的。
 * 2. 无论是KMZ格式还是AUTEL格式的任务文件，均不适合直接用于实际飞行。
 * 3. 用户需要自行规划任务，并根据实际情况设置合适的飞行高度和高度模式
 */

/**
 * Mission Sample
 * Notes:
 * 1. This Demo is for demonstration purposes only.
 * 2. Neither KMZ nor AUTEL format mission files are suitable for actual flight operations.
 * 3. Users need to plan their missions independently and set appropriate flight altitudes and height modes based on real-world conditions.
 */
class MissionDemoFragment : AutelFragment() {
    companion object {
        private const val TAG = "MissionDemoFragment"

        //1.7 old version drone only support autel mission format mission
        private val Autel_Mission_Test_Data = " {\"Action_Default\":{\"Action_Time\":0,\"Action_Type\":0,\"Action_Yaw_Ref\":0.0,\"Gimbal_Pitch\":0.0,\"Gimbal_Roll\":0.0,\"Shoot_Dis_Interval\":2000.0,\"Shoot_Time_Interval\":0,\"Zoom_Rate\":0,\"reserved\":[0,0]},\"Altitude_type\":0,\"Finish_Action\":0,\"GUID\":1742009974,\"Gimbal_Pitch_Mapping\":0.0,\"Gride_Enable_Mapping\":0,\"Min_OA_Dist\":0,\"Mission_ID\":0,\"Mission_Length\":34954,\"Mission_Time\":8254,\"Mission_type\":0,\"Obstacle_Mode\":0,\"Overlap_Mapping\":0,\"POI_Num\":0,\"RC_Lost_Action\":2,\"VFOV_Mapping\":30.0,\"Waypoint_Num\":3,\"Waypoints\":[{\"Action_Num\":1,\"Actions\":[{\"Action_Time\":10,\"Action_Type\":2,\"Action_Yaw_Ref\":0.0,\"Gimbal_Pitch\":0.0,\"Gimbal_Roll\":0.0,\"Shoot_Dis_Interval\":5000.0,\"Shoot_Time_Interval\":2,\"Zoom_Rate\":0,\"reserved\":[0]}],\"Altitude_Priority\":0,\"Center_Altitude\":60000,\"Center_Latitude\":226165011,\"Center_Longitude\":1140171942,\"Cur_Altitude\":60000,\"Cur_Latitude\":226165011,\"Cur_Longitude\":1140171942,\"FP_Length\":16195,\"FP_Time\":3749,\"Heading_Mode\":1,\"POI\":{\"Altitude\":0,\"Latitude\":0,\"Longitude\":0},\"POI_Valid\":-1,\"Prev_Altitude\":60000,\"Prev_Latitude\":226179025,\"Prev_Longitude\":1140167436,\"Velocity_Ref\":5.0,\"Velocity_Ref_Next\":5.0,\"Waypoint_Type\":1,\"lineType\":0,\"reserved\":[3,0]},{\"Action_Num\":2,\"Actions\":[{\"Action_Time\":10,\"Action_Type\":11,\"Action_Yaw_Ref\":30.0,\"Gimbal_Pitch\":31.0,\"Gimbal_Roll\":0.0,\"Shoot_Dis_Interval\":5000.0,\"Shoot_Time_Interval\":2,\"Zoom_Rate\":0,\"reserved\":[0]},{\"Action_Time\":10,\"Action_Type\":2,\"Action_Yaw_Ref\":0.0,\"Gimbal_Pitch\":0.0,\"Gimbal_Roll\":0.0,\"Shoot_Dis_Interval\":5000.0,\"Shoot_Time_Interval\":2,\"Zoom_Rate\":0,\"reserved\":[0]}],\"Altitude_Priority\":0,\"Center_Altitude\":60000,\"Center_Latitude\":226180065,\"Center_Longitude\":1140180311,\"Cur_Altitude\":60000,\"Cur_Latitude\":226180065,\"Cur_Longitude\":1140180311,\"FP_Length\":18759,\"FP_Time\":4345,\"Heading_Mode\":1,\"POI\":{\"Altitude\":0,\"Latitude\":0,\"Longitude\":0},\"POI_Valid\":-1,\"Prev_Altitude\":60000,\"Prev_Latitude\":226165011,\"Prev_Longitude\":1140171942,\"Velocity_Ref\":5.0,\"Velocity_Ref_Next\":5.0,\"Waypoint_Type\":1,\"lineType\":0,\"reserved\":[3,0]},{\"Action_Num\":1,\"Actions\":[{\"Action_Time\":10,\"Action_Type\":2,\"Action_Yaw_Ref\":0.0,\"Gimbal_Pitch\":0.0,\"Gimbal_Roll\":0.0,\"Shoot_Dis_Interval\":5000.0,\"Shoot_Time_Interval\":2,\"Zoom_Rate\":0,\"reserved\":[0]}],\"Altitude_Priority\":0,\"Center_Altitude\":60000,\"Center_Latitude\":226180065,\"Center_Longitude\":1140180311,\"Cur_Altitude\":60000,\"Cur_Latitude\":226180065,\"Cur_Longitude\":1140180311,\"FP_Length\":0,\"FP_Time\":160,\"Heading_Mode\":1,\"POI\":{\"Altitude\":0,\"Latitude\":0,\"Longitude\":0},\"POI_Valid\":-1,\"Prev_Altitude\":60000,\"Prev_Latitude\":226180065,\"Prev_Longitude\":1140180311,\"Velocity_Ref\":5.0,\"Velocity_Ref_Next\":0.0,\"Waypoint_Type\":1,\"lineType\":0,\"reserved\":[3,0]}],\"Yaw_Ref_Mapping\":0.0,\"reserved\":[0,0]}\n"
    }

    // use Kmz mission  or old autel mission , drone only > 1.7 version support kmz
    private var useOldMission = SDKConfig.isSingle()

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

    var missionTypeTxt =  if(useOldMission) "Aut mission" else "KMZ mission"

    private fun initView() {

        binding?.btnMissionType?.text = missionTypeTxt

        binding?.btnMissionType?.setOnClickListener{
            useOldMission = !useOldMission
            missionTypeTxt =  if(useOldMission) "Aut mission" else "KMZ mission"
            binding?.btnMissionType?.text = missionTypeTxt
        }

        binding?.btnUpload?.setOnClickListener {
            updateLogInfo(">> btnUpload click： $missionTypeTxt")
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
        if(!useOldMission) {
            getMissionManger()?.uploadKmzMissionFile(
                kmzPath,
                guid!!,
                object : CommonCallbacks.CompletionCallbackWithProgressAndParam<Long> {
                    override fun onFailure(error: IAutelCode, msg: String?) {
                        updateLogInfo(
                            "upload kmz mission fail:[code:${error.code};msg:$msg]",
                            isError = true
                        )
                        enableOrDisableBtn(binding?.btnUpload, true)
                    }

                    override fun onProgressUpdate(progress: Double) {
                        updateLogInfo("upload kmz mission progress:$progress&percent:${progress * 100}%")
                    }

                    override fun onSuccess(t: Long?) {
                        updateLogInfo("upload kmz mission success  guid=$t")
                        enableOrDisableBtn(binding?.btnUpload, true)
                    }
                })
        } else {
            val missionJni =  Gson().fromJson(Autel_Mission_Test_Data, MissionInfoJNI::class.java)
            getMissionManger()?.uploadMissionFile(missionJni,object :CommonCallbacks.CompletionCallbackWithProgressAndParam<Long>{
                override fun onProgressUpdate(progress: Double) {
                    updateLogInfo("upload aut mission progress:$progress&percent:${progress * 100}%")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    updateLogInfo(
                        "upload aut mission fail:[code:${error.code};msg:$msg]",
                        isError = true
                    )
                    enableOrDisableBtn(binding?.btnUpload, true)
                }

                override fun onSuccess(t: Long?) {
                    t?.let { guid = it.toInt() }
                    updateLogInfo("upload aut mission success  guid=$t")
                    enableOrDisableBtn(binding?.btnUpload, true)
                }
            })
        }
    }

    private fun startMission() {
        enableOrDisableBtn(binding?.btnStart, false)
        guid?.let {
            val bean  = if(useOldMission){
                MissionWaypointGUIDBean(guid= it)
            } else {
                MissionKmlGUIDBean(kmlMissionId = it)
            }
            getMissionManger()?.startMission(
                bean,
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
            }, isKml = !useOldMission)
        }
    }

    private fun resumeMission() {
        enableOrDisableBtn(binding?.btnResume, false)
        guid?.let {
            val bean  = if(useOldMission){
                MissionWaypointGUIDBean(guid= it)
            } else {
                MissionKmlGUIDBean(kmlMissionId = it)
            }
            getMissionManger()?.resumeMission(
                bean,
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
            }, isKml = !useOldMission)
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
