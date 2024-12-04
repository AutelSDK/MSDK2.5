package com.autel.sdk.debugtools.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IUpgradeManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.OTAUpgradeManger
import com.autel.drone.sdk.vmodelx.manager.UpgradeManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.upgrade.bean.UpgradeResultBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.upgrade.enums.UpgradeClientTypeEnum
import com.autel.drone.sdk.vmodelx.manager.upgrade.UpgradeErrorStateEnum
import com.autel.drone.sdk.vmodelx.manager.upgrade.UpgradeFlowEnum
import com.autel.drone.sdk.vmodelx.manager.upgrade.UpgradeListener
import com.autel.drone.sdk.vmodelx.module.fileservice.FileTransmitListener
import com.autel.drone.sdk.vmodelx.module.upgrade.bean.ota.CheckResponseBean
import com.autel.drone.sdk.vmodelx.utils.S3DownloadInterceptor
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.databinding.FragmentOtaFirmwareUpgradeBinding
import java.io.File
import java.util.Locale

/**
 * A simple [Fragment] subclass.
 * Use the [OTAFirmwareUpgradeFragment] factory method to
 * create an instance of this fragment.
 */
@SuppressLint("SetTextI18n")
class OTAFirmwareUpgradeFragment : AutelFragment() {

    companion object {
        private const val TAG = "OTA Test"
    }

    private lateinit var binding: FragmentOtaFirmwareUpgradeBinding
    private var map: HashMap<String, CheckResponseBean.Data>? = null

    private var s3DownloadInterceptor: S3DownloadInterceptor? = null

    private val logList: MutableList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentOtaFirmwareUpgradeBinding.inflate(layoutInflater)
        initView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOtaFirmwareUpgradeBinding.inflate(inflater)
        initUpgradeListener()
        initView()
        return binding.root
    }

    private fun initUpgradeListener() {
        OTAUpgradeManger.getInstance()
            .addUpgradeVersionListener(object : OTAUpgradeManger.UpgradeVersionListener {
                override fun onEnterUpgradeMode(success: Boolean) {
                    showLog("onEnterUpgradeMode success=$success")
                    ToastUtils.showToast("Enter Upgrade Mode success =$success")
                }

                override fun onExitUpgradeMode(success: Boolean) {
                    showLog("onExitUpgradeMode success=$success")
                    ToastUtils.showToast("Exit Upgrade Mode success =$success")
                }

                override fun onDeviceUpgrade(beanMap: HashMap<String, CheckResponseBean.Data>) {
                    showLog("onDeviceUpgrade=${beanMap.size}, $beanMap")
                    map = beanMap
                }
            })
    }


    private fun initView() {
        binding.check.setOnClickListener {
            //Check sever has new firmware,only for internal use.
            OTAUpgradeManger.getInstance().detectDeviceUpdateInfo()
            showLog(">>click check")
            ToastUtils.showToast("start to check upgrade info(several minutes).")
        }

        binding.downLoad.setOnClickListener {
            showLog(">>click downLoad")
            val remoteDeviceId =
                DeviceManager.getDeviceManager().getLocalRemoteDevice().deviceNumber()
            val remoterBean = map?.get(remoteDeviceId.toString())
            if (remoterBean != null && remoterBean.isNeed_upgrade) {
                s3DownloadInterceptor = OTAUpgradeManger.getInstance()
                    .downloadFile(remoterBean.pkg_url, 0, object : FileTransmitListener<File> {
                        override fun onSuccess(result: File?) {
                            showLog("downloadFile onSuccess $result")
                        }

                        override fun onProgress(sendLength: Long, totalLength: Long, speed: Long) {
                            showLog("downloadFile onProgress $speed")
                        }

                        override fun onFailed(code: IAutelCode?, msg: String?) {
                            showLog("downloadFile onFailed $code, $msg", isError = true)

                        }
                    })
                return@setOnClickListener
            } else {
                ToastUtils.showToast("Remoter no need to upgrade")
            }

            DeviceManager.getDeviceManager().getDroneDevices().forEach {
                val deviceId = it.deviceNumber()
                val bean = map?.get(deviceId.toString())
                if (bean != null && bean.isNeed_upgrade) {
                    s3DownloadInterceptor = OTAUpgradeManger.getInstance()
                        .downloadFile(bean.pkg_url, 0, object : FileTransmitListener<File> {
                            override fun onSuccess(result: File?) {
                                showLog("downloadFile onSuccess $result")
                            }

                            override fun onProgress(
                                sendLength: Long,
                                totalLength: Long,
                                speed: Long
                            ) {
                                showLog("downloadFile onProgress $speed")
                            }

                            override fun onFailed(code: IAutelCode?, msg: String?) {
                                showLog("downloadFile onFailed $code, $msg", isError = true)
                            }
                        })
                    return@setOnClickListener
                } else {
                    ToastUtils.showToast("Drone no need to upgrade(${it.toSampleString()})")
                }
            }
        }

        binding.cancel.setOnClickListener {
            showLog(">>click cancel")
            s3DownloadInterceptor?.cancel()
        }

        binding.btnRemoteEnterUpgade.setOnClickListener {
            showLog(">>click btnRemoteEnterUpgade")
            testRemoterUpgrade()
        }

        binding.btnDroneUpgrade.setOnClickListener {
            showLog(">>click btnDroneUpgrade")
            testDroneUpgrade()
        }

        binding.btnEnterUpgade.setOnClickListener {
            showLog(">>click btnEnterUpgade")
            OTAUpgradeManger.getInstance().switchUpgradeMode(true)
        }
        binding.btnExitUpgade.setOnClickListener {
            showLog(">>click btnExitUpgade")
            OTAUpgradeManger.getInstance().switchUpgradeMode(false)
        }

        binding.tvClearLogo.setOnClickListener {
            binding.tvLogInfo.text = ""
            logList.clear()
        }
    }

    /**
     * upgrade drone firmware, we must upgrade drone first
     */
    private fun testDroneUpgrade() {
        val droneFilePath = "/mnt/media_rw/sdcard1/ModelX-V1.8.0.178-20240521025146.Encrypt.uav"
        val deviceId = DeviceManager.getFirstDroneDevice()?.deviceNumber() ?: return
        var skyUpgradeManager: IUpgradeManager? =
            UpgradeManager(deviceId).init(UpgradeClientTypeEnum.CLIENT_TYPE_SKY)

        skyUpgradeManager?.registerUpgradeListener(object : UpgradeListener {

            override fun onUpgradeFlowChange(deviceId: Int, flowState: UpgradeFlowEnum) {
                showLog("onUpgradeFlowChange $flowState")
            }

            override fun onUpgradeStateChange(deviceId: Int, state: UpgradeErrorStateEnum) {
                showLog("onUpgradeStateChange $state")
                skyUpgradeManager?.unInit()
                skyUpgradeManager = null
            }

            override fun onUploadPackageProgress(
                deviceId: Int,
                totalLength: Long,
                sendLength: Long,
                progress: Int,
                speed: Long
            ) {
                val speedText: String = if (speed < 1000) {
                    String.format(Locale.ENGLISH, "%.2f KB/s ", speed / 1024f)
                } else {
                    String.format(Locale.ENGLISH, "%.2f MB/s ", speed / 1024f / 1024f)
                }
                binding.speedText.text = "progress=$progress%,  speed=$speedText"
                showLog("onUploadPackageProgress $progress, speed=$speedText")
            }

            override fun onUpgradeProgress(deviceId: Int, progress: Int) {
                showLog("onUpgradeProgress $progress")
                binding.speedText.text = "   onUpgradeProgress $progress%"
            }

            override fun onUpgradeResult(deviceId: Int, resultBean: UpgradeResultBean) {
                showLog("onUpgradeResult $resultBean")
                skyUpgradeManager?.unInit()
                skyUpgradeManager = null
            }
        })
        skyUpgradeManager?.startUpgradeFlow(File(droneFilePath), null)
    }

    /**
     * Upgrade remoter firmware
     */
    private fun testRemoterUpgrade() {
        val gndFilePath = "/mnt/media_rw/sdcard1/RC79.Autel-V1.8.0.177-20240521003729.Encrypt.uav"
        val deviceId = DeviceManager.getDeviceManager().getLocalRemoteDevice().deviceNumber()
        var gndUpgradeManager: IUpgradeManager? =
            UpgradeManager(deviceId).init(UpgradeClientTypeEnum.CLIENT_TYPE_GND)

        gndUpgradeManager?.registerUpgradeListener(object : UpgradeListener {

            override fun onUpgradeFlowChange(deviceId: Int, flowState: UpgradeFlowEnum) {
                showLog("onUpgradeFlowChange $flowState")
            }

            override fun onUpgradeStateChange(deviceId: Int, state: UpgradeErrorStateEnum) {
                showLog("onUpgradeStateChange $state")
                gndUpgradeManager?.unInit()
                gndUpgradeManager = null
            }

            override fun onUploadPackageProgress(
                deviceId: Int,
                totalLength: Long,
                sendLength: Long,
                progress: Int,
                speed: Long
            ) {
                showLog("onUploadPackageProgress $progress")
            }

            override fun onUpgradeProgress(deviceId: Int, progress: Int) {
                showLog("onUpgradeProgress $progress")
                binding.speedText.text = "onUpgradeProgress $progress%"
            }

            override fun onUpgradeResult(deviceId: Int, resultBean: UpgradeResultBean) {
                showLog("onUpgradeResult $resultBean")
                gndUpgradeManager?.unInit()
                gndUpgradeManager = null
            }
        })
        gndUpgradeManager?.startUpgradeFlow(File(gndFilePath), null)
    }


    private fun downloadTest() {
        val path =
            "https://autel-cc-media.obs.cn-south-1.myhuaweicloud.com/ota/2024-07-15-7081313544470528.uav?AccessKeyId=TJDEDEFZZAH7WEQNJ7RR&Expires=1721273599&Signature=RQW52ixw%2BDygmQw89X5LabxVRDw%3D"
        OTAUpgradeManger.getInstance().downloadFile(path, 0, object : FileTransmitListener<File> {
            override fun onSuccess(result: File?) {
                showLog("downloadTest: $result")
            }

            override fun onProgress(sendLength: Long, totalLength: Long, speed: Long) {
                showLog("downloadTest: $sendLength")
            }

            override fun onFailed(code: IAutelCode?, msg: String?) {
                showLog("downloadTest onFailed: $code $msg")
            }
        })
    }

    private fun showLog(log: String, isError: Boolean = false) {
        logList.add(log)

        if (isError) {
            SDKLog.e(TAG, log)
        } else {
            SDKLog.i(TAG, log)
        }

        val stringBuffer = StringBuffer()
        logList.forEach {
            stringBuffer.append(it).append("\n")
        }

        binding.tvLogInfo.text = stringBuffer.toString()
    }
}