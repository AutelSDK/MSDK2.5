package com.autel.sdk.debugtools.fragment

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.interfaces.IUpgradeManager
import com.autel.drone.sdk.vmodelx.interfaces.OTAFirmwareUpgradeProcessStateListener
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.OTAFirmwareUpgradeManager
import com.autel.drone.sdk.vmodelx.manager.OTAUpgradeManger
import com.autel.drone.sdk.vmodelx.manager.UpgradeManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.NetMeshKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.UpgradeServiceKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.NotifyDeviceToUpgradeBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.upgrade.bean.UpgradeResultBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.upgrade.enums.UpgradeClientTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.upgrade.enums.UpgradeStateEnum
import com.autel.drone.sdk.vmodelx.manager.upgrade.UpgradeErrorStateEnum
import com.autel.drone.sdk.vmodelx.manager.upgrade.UpgradeListener
import com.autel.drone.sdk.vmodelx.module.fileservice.FileTransmitListener
import com.autel.drone.sdk.vmodelx.module.upgrade.bean.OTAUpgradeModel
import com.autel.drone.sdk.vmodelx.module.upgrade.bean.ota.CheckResponseBean
import com.autel.drone.sdk.vmodelx.module.upgrade.bean.ota.ObservableMap
import com.autel.drone.sdk.vmodelx.module.upgrade.enums.OTAUpgradeProgressState
import com.autel.drone.sdk.vmodelx.utils.MicroFtpUtil
import com.autel.drone.sdk.vmodelx.utils.S3DownloadInterceptor
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.databinding.FragmentOtaFirmwareUpgradeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * A simple [Fragment] subclass.
 * Use the [OTAFirmwareUpgradeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OTAFirmwareUpgradeFragment : AutelFragment(), OTAFirmwareUpgradeProcessStateListener {

    private val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val requestCode = 1

    private lateinit var binding: FragmentOtaFirmwareUpgradeBinding

    private var map = ObservableMap<String, CheckResponseBean.Data>()

    private var s3DownloadInterceptor: S3DownloadInterceptor? = null

    val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentOtaFirmwareUpgradeBinding.inflate(layoutInflater)
        initView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOtaFirmwareUpgradeBinding.inflate(inflater)
        initView()

        /* OTAUpgradeManger.getInstance().registerRemoterAutoUpgradeListener(object : RemoterAutoUpgradeWrapper.RemoterAutoUpgradeListener{
             override fun onUpgradeProgress(progress: Int) {
                 SDKLog.i("OTA-", "onUpgradeStateChange $progress")
             }

             override fun onUpgradeResult(resultBean: UpgradeResultBean) {
                 SDKLog.i("OTA-", "onUpgradeResult $resultBean")
             }
         })*/

        SDKLog.i("OTAFirmwareUpgradeFragment", "path=${MicroFtpUtil.getDroneFilePath(SDKManager.get().sContext, "APPLE")}")
        return binding.root
    }

    companion object {
        private const val TAG = "OTA Test"
    }


    private fun downloadTest(){
        val path = "https://autel-cc-media.obs.cn-south-1.myhuaweicloud.com/ota/2024-07-15-7081313544470528.uav?AccessKeyId=TJDEDEFZZAH7WEQNJ7RR&Expires=1721273599&Signature=RQW52ixw%2BDygmQw89X5LabxVRDw%3D"
        OTAUpgradeManger.getInstance().downloadFile(path, 0, object:FileTransmitListener<File>{
            override fun onSuccess(result: File?) {
                SDKLog.i("OTAFirmwareUpgradeFragment", "checkProduct: $result")
            }

            override fun onProgress(sendLength: Long, totalLength: Long, speed: Long) {
                SDKLog.i("OTAFirmwareUpgradeFragment", "sendLength: $sendLength")
            }

            override fun onFailed(code: IAutelCode?, msg: String?) {
                SDKLog.i("OTAFirmwareUpgradeFragment", "onFailed: $code")
            }
        })
    }

    private fun initView() {
        OTAFirmwareUpgradeManager.getInstance().addListener(this)
        binding.check.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                try {
                    map = OTAFirmwareUpgradeManager.getInstance().checkProduct()
                    SDKLog.i("OTAFirmwareUpgradeFragment", "checkProduct: $map")
                } catch (e: Exception) {
                    SDKLog.i("OTAFirmwareUpgradeFragment", "checkProduct fair: ${e.message}")
                }

            }
        }

        binding.downLoad.setOnClickListener {

            val modex = map.get("ModelX")
            modex?.let {
                if (it.isNeed_upgrade) {
                    s3DownloadInterceptor = OTAFirmwareUpgradeManager.getInstance().downLoadProductFile(it.pkg_url, object :
                        FileTransmitListener<File> {
                        override fun onSuccess(result: File?) {
                            SDKLog.d("OTAFirmwareUpgradeFragment", "downLoad success: ")
                        }

                        override fun onProgress(sendLength: Long, totalLength: Long, speed: Long) {

                            SDKLog.d("OTAFirmwareUpgradeFragment", "downLoad size: $sendLength")
                        }

                        override fun onFailed(code: IAutelCode, message: String?) {
                            SDKLog.d("OTAFirmwareUpgradeFragment", "downLoad failed: $message")
                        }
                    })
                }
            }

        }

        binding.cancel.setOnClickListener {
            s3DownloadInterceptor?.cancel()
        }

        binding.startUpgrade.setOnClickListener {
            /*var filePath = SDKManager.get().sContext?.getExternalFilesDir(null)?.absolutePath
            val gndFilePath = filePath + "/RC79.Autel-V1.5.0.9-20230708165411.uav"
            val skyFilePath = filePath + "/ModelX-V1.5.0.75-20230706142034.autel"
            var gndFile =  File(gndFilePath)
            var skyFile =  File(skyFilePath)
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                try {
                    val gndMode = OTAUpgradeModel(UpgradeClientTypeEnum.CLIENT_TYPE_GND,gndFile)
                    val skyMode = OTAUpgradeModel(UpgradeClientTypeEnum.CLIENT_TYPE_SKY,skyFile)
                    val list = arrayOf(skyMode, gndMode);
                    OTAFirmwareUpgradeManager.getInstance().startFirmwareUpgrade(list)
                }catch (e: Exception) {
                    SDKLog.i("OTAFirmwareUpgradeFragment","upgrade fair: ${e.message}")
                }
            }*/

            testDroneNew()

        }

        binding.btnRemoteEnterUpgade.setOnClickListener {
            testRemoteNew()
        }

        binding.btnEnterUpgade.setOnClickListener {
            OTAUpgradeManger.getInstance().switchUpgradeMode(true)
        }
        binding.btnExitUpgade.setOnClickListener {
            OTAUpgradeManger.getInstance().switchUpgradeMode(false)
        }

        binding.btnTestDrone.setOnClickListener {
            ToastUtils.showToast("Drones=${DeviceManager.getDeviceManager().getDroneDevices().size}")
            val upgradeKeyManager = DeviceManager.getDeviceManager().getDroneDevices().firstOrNull()?.getUpgradeKeyManager()
            if(upgradeKeyManager==  null){
                ToastUtils.showToast("keymanager null")
                return@setOnClickListener
            }

            ToastUtils.showToast("keymanager send success")
            upgradeKeyManager?.performAction(
                    KeyTools.createKey(UpgradeServiceKey.KeyUpgradeStateQuery), null,
                    object : CommonCallbacks.CompletionCallbackWithParam<UpgradeStateEnum> {
                        override fun onSuccess(t: UpgradeStateEnum?) {
                            ToastUtils.showToast("success")
                        }

                        override fun onFailure(error: IAutelCode, msg: String?) {
                            ToastUtils.showToast("onFailure =$error")
                        }
                    }, retryCount = 0
                )
        }

        binding.btnSingleUpgrade.setOnClickListener {
            val key = KeyTools.createKey(NetMeshKey.keySetDeviceToUpgrade)
            val atKeyManager = DeviceManager.getDeviceManager().getLocalRemoteDevice().getATKeyManager()

            var nodeId = -1
            try {
                nodeId = binding.noteIdTxt.text.toString().toInt()
            } catch (e: Exception) {
                ToastUtils.showToast("keyNotifyDeviceToUpgrade input nodeId error")
                val deviceId = DeviceManager.getFirstDroneDevice()?.deviceNumber()
                nodeId = DeviceManager.getDeviceManager().getDroneDeviceById(deviceId!!)?.getNodeId() ?: -1
            }

            if (nodeId < 0) {
                ToastUtils.showToast("keyNotifyDeviceToUpgrade nodeId is null")
                return@setOnClickListener
            }
            val bean = NotifyDeviceToUpgradeBean(nodeId)
            atKeyManager.performAction(key, bean, object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    ToastUtils.showToast("keyNotifyDeviceToUpgrade success")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    ToastUtils.showToast("keyNotifyDeviceToUpgrade onFailure")
                }
            })
        }
    }

    /**
     * 升级飞机
     */
   private fun testDroneNew(){
        val droneFilePath = "/mnt/media_rw/sdcard1/ModelX-V1.8.0.178-20240521025146.Encrypt.uav"

        val deviceId = DeviceManager.getFirstDroneDevice()?.deviceNumber() ?: return
        val skyUpgradeManager: IUpgradeManager = UpgradeManager(deviceId).init(UpgradeClientTypeEnum.CLIENT_TYPE_SKY)
        skyUpgradeManager.registerUpgradeListener(object : UpgradeListener {
            override fun onUpgradeStateChange(deviceId: Int, state: UpgradeErrorStateEnum) {
                SDKLog.i("OTA-", "onUpgradeStateChange $state")
            }

            override fun onUploadPackageProgress(deviceId: Int, totalLength: Long, sendLenght: Long, progress: Int, speed: Long) {
                SDKLog.i("OTA-", "onUploadPackageProgress $progress")
                var speedText: String = if (speed < 1000) {
                    String.format(Locale.ENGLISH, "%.2f KB/s ", speed / 1024f)
                } else {
                    String.format(Locale.ENGLISH, "%.2f MB/s ", speed / 1024f / 1024f)
                }
                binding.speedText.text = "   $progress%,  $speedText"
            }

            override fun onUpgradeProgress(deviceId: Int, progress: Int) {
                SDKLog.i("OTA-", "onUpgradeProgress $progress")
                binding.speedText.text = "   onUpgradeProgress $progress%"
            }

            override fun onUpgradeResult(deviceId: Int, resultBean: UpgradeResultBean) {
                SDKLog.i("OTA-", "onUpgradeResult $resultBean")
            }

        })
        skyUpgradeManager.startUpgradeFlow(File(droneFilePath), null)

    }

    /**
     * 升级遥控器
     */

    private fun testRemoteNew(){
        val gndFilePath = "/mnt/media_rw/sdcard1/RC79.Autel-V1.8.0.177-20240521003729.Encrypt.uav"
        val gndUpgradeManager :IUpgradeManager = UpgradeManager(0).init(UpgradeClientTypeEnum.CLIENT_TYPE_GND)
        gndUpgradeManager.registerUpgradeListener(object : UpgradeListener{

            override fun onUpgradeStateChange(deviceId: Int, state: UpgradeErrorStateEnum) {
                SDKLog.i("OTA-", "onUpgradeStateChange $state")
            }

            override fun onUploadPackageProgress(deviceId: Int, totalLength: Long, sendLenght: Long, progress: Int, speed: Long) {
                SDKLog.i("OTA-", "onUploadPackageProgress $progress")
                var speedText: String = if (speed < 1000) {
                    String.format(Locale.ENGLISH, "%.2f KB/s ", speed / 1024f)
                } else {
                    String.format(Locale.ENGLISH, "%.2f MB/s ", speed / 1024f / 1024f)
                }
                binding.speedText.text = "   $progress%,  $speedText"
            }

            override fun onUpgradeProgress(deviceId: Int, progress: Int) {
                SDKLog.i("OTA-", "onUpgradeProgress $progress")

                binding.speedText.text = "   onUpgradeProgress $progress%"
            }

            override fun onUpgradeResult(deviceId: Int, resultBean: UpgradeResultBean) {
                SDKLog.i("OTA-", "onUpgradeResult $resultBean")
            }

        })
        gndUpgradeManager.startUpgradeFlow(File(gndFilePath), null)

    }

    override fun onOTAFirmwareUpgradeProcessState(
        model: OTAUpgradeModel,
        status: OTAUpgradeProgressState
    ) {

        lifecycleScope.launch(Dispatchers.Main) {
            val typeEnum = model.clientType
            if (status == OTAUpgradeProgressState.UPGRADING || status == OTAUpgradeProgressState.UPLOADING) {
                binding.upgradeState.text =
                    "upgrade state: " + (if (typeEnum == UpgradeClientTypeEnum.CLIENT_TYPE_GND) "gnd  " else "sky ") + "state: ${status.name}" + " progress: ${status.progress}"
            } else if (status == OTAUpgradeProgressState.OPERATION_FAILED) {
                binding.upgradeState.text =
                    "upgrade state: " + (if (typeEnum == UpgradeClientTypeEnum.CLIENT_TYPE_GND) "gnd  " else "sky  ") + "upgrade fair" + " error: ${status.errorMessage?.message}"
            } else if (status == OTAUpgradeProgressState.UPGRADE_SUCCESS) {
                binding.upgradeState.text = "upgrade state: success"
            } else {
                binding.upgradeState.text =
                    "upgrade state: " + (if (typeEnum == UpgradeClientTypeEnum.CLIENT_TYPE_GND) "gnd  " else "sky  ") + "state: ${status.name}"
            }
        }
    }

    fun calculateMD5(file: File): String {
        val md5Digest = MessageDigest.getInstance("MD5")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md5Digest.update(buffer, 0, bytesRead)
        }

        inputStream.close()

        val md5Bytes = md5Digest.digest()
        val md5String = StringBuilder()

        for (byte in md5Bytes) {
            md5String.append(String.format("%02x", byte))
        }

        return md5String.toString()
    }

}