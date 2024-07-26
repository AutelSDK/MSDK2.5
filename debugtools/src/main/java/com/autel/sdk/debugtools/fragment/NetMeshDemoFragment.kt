package com.autel.sdk.debugtools.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelRemoteDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.interfaces.IControlDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IGroupMeshApi
import com.autel.drone.sdk.vmodelx.interfaces.IMeshDeviceChangedListener
import com.autel.drone.sdk.vmodelx.interfaces.IMultiDeviceOperator
import com.autel.drone.sdk.vmodelx.interfaces.INetMeshManager
import com.autel.drone.sdk.vmodelx.interfaces.IWatchDroneListener
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.data.MeshModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.CreateDeviceNetworkReq
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.CreateDeviceNetworkResp
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.DeviceNetworkInfo
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.DeviceNetworkStatusType
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.adapter.NetMeshDemoAdapter
import com.autel.sdk.debugtools.databinding.FragmentNetMeshDemoBinding

/**
 * 演示如何组网
 */
class NetMeshDemoFragment : AutelFragment(){
    private lateinit var binding: FragmentNetMeshDemoBinding

    private var dataList: MutableList<IBaseDevice> = mutableListOf()

    private lateinit var adapter: NetMeshDemoAdapter

    private var netMeshResponse: CreateDeviceNetworkResp? = null

    private fun getMultiDeviceOperator():IMultiDeviceOperator{ return  DeviceManager.getMultiDeviceOperator() }

    //组网接口
    private fun getNetMeshManger():INetMeshManager{ return getMultiDeviceOperator().getNetMeshManager() }

    //群组接口：群组是构建在组网上的一种关系，可以创建一个群加入不同飞机
    private fun getNetGroupManger():IGroupMeshApi{ return getMultiDeviceOperator().getGroupMeshApi() }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNetMeshDemoBinding.inflate(layoutInflater)
        setAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewListener()
        initNetMeshListener()
        updateData()
    }

    private fun initViewListener(){
        binding.btnNetMeshStart.setOnClickListener{ startNetMesh(false) }

        binding.btnNetMeshComplete.setOnClickListener{
            getNetMeshManger().completeNetMeshMatching(object : CommonCallbacks.CompletionCallbackWithParam<Int>{
                override fun onSuccess(t: Int?) { ToastUtils.showToast("完成组网") }
                override fun onFailure(error: IAutelCode, msg: String?) { ToastUtils.showToast("完成组网失败:$msg") }
            })
        }

        binding.btnNetMeshDisband.setOnClickListener{
            var groupId: Long = netMeshResponse?.info?.groupId ?: 0
            getNetMeshManger().disbandNetMesh(groupId, object : CommonCallbacks.CompletionCallbackWithParam<Int>{
                override fun onSuccess(t: Int?) {
                    ToastUtils.showToast("解算成功")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    ToastUtils.showToast("解算失败:$msg")
                }
            })
        }

        binding.btnControlMode.setOnClickListener {
            setControlMode(ControlMode.ALL, 0)
        }

        binding.btnChangeWatch.setOnClickListener {

        }

        binding.btnChangeCenterDrone.setOnClickListener {

        }
    }

    /**
     * 组网监听
     */
    private fun initNetMeshListener(){
        //码流飞机变化上报
        DeviceManager.getMultiDeviceOperator().addWatchChangeListener(object: IWatchDroneListener{
            override fun onWatchChange(droneList: List<IAutelDroneDevice>) {
                droneList.firstOrNull()?.let {
                    ToastUtils.showToast("码流飞机:${it.deviceNumber()}")
                }

            }
        })

        DeviceManager.getMultiDeviceOperator().addControlChangeListener(object : IControlDroneListener{
            override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
                ToastUtils.showToast("控制模式:${mode}： ${droneList.map { it.deviceNumber()}}")
                updateData()
            }
        })

        //组网状态上报
        getMultiDeviceOperator().addNetMeshChangeListener(object : IMeshDeviceChangedListener{
            override fun onDroneDeviceChanged(newDroneList: MutableList<IAutelDroneDevice>?, oldDroneList: MutableList<IAutelDroneDevice>?) {
                updateData() //飞机更新

                //当前有无中继飞机
                if(newDroneList?.filter { it.isCenter() }?.isNullOrEmpty() == true){
                    newDroneList.firstOrNull()?.let { setCenterDrone(it.deviceNumber()) }
                }

                //当前有无Watch飞机
                if(newDroneList?.filter { it.isWatched() }?.isNullOrEmpty() == true){
                    newDroneList.firstOrNull()?.let { setWatchDrone(it.deviceNumber()) }
                }
            }

            override fun onRemoteDeviceChanged(newRCList: MutableList<IAutelRemoteDevice>?, oldRCList: MutableList<IAutelRemoteDevice>?) {
                updateData() //遥控器更新
            }

            override fun onMeshStateChanged(status: DeviceNetworkStatusType) {
                when(status){
                    DeviceNetworkStatusType.STARTED->{
                    }
                    DeviceNetworkStatusType.SUCCESS->{
                    }
                    DeviceNetworkStatusType.DISMISS->{
                        ToastUtils.showToast("组网解算")
                    }
                    DeviceNetworkStatusType.TIMEOUT->{
                        ToastUtils.showToast("组网超时，重新开始")
                        startNetMesh(true)
                    }
                    else -> {

                    }
                }
            }
        })
    }


    /**
     * 设置中继飞机
     */
    private fun setCenterDrone(deviceId: Int){
        getNetMeshManger().setCenterNode(deviceId, object : CommonCallbacks.CompletionCallbackWithParam<Int>{
            override fun onSuccess(t: Int?) { ToastUtils.showToast("中继设置成功：$deviceId") }
            override fun onFailure(error: IAutelCode, msg: String?) { ToastUtils.showToast("中继设置失败：$msg") }
        })
    }

    /**
     * 设置watch码流的飞机
     */
    private fun setWatchDrone(deviceId: Int){
        getNetMeshManger().setWatchDevice(arrayListOf(deviceId), object: CommonCallbacks.CompletionCallbackWithParam<Int>{
            override fun onSuccess(t: Int?) { ToastUtils.showToast("码流设置成功：$deviceId") }
            override fun onFailure(error: IAutelCode, msg: String?) { ToastUtils.showToast("码流设置失败：$msg") }
        })
    }

    /**
     * id 为 NodeId 或 GroupId
     *
     */
    private fun setControlMode(mode: ControlMode, id:Int){
        var log: String = ""
        if(mode == ControlMode.ALL){ //全控：Id被忽略
            log = "全控"
        } else if(mode == ControlMode.GROUP){
            log = "群控(groupId=$id)"
        } else if(mode == ControlMode.SINGLE){
            log = "单控(nodeId=$id)"
        }

        getNetGroupManger().switchControlMode(mode,id,object : CommonCallbacks.CompletionCallbackWithParam<Void>{
            override fun onSuccess(t: Void?) {
                ToastUtils.showToast("$log 设置成功")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast("$log 设置失败：$msg")
            }
        })
    }

    /**
     * 启动组网
     */
    private fun startNetMesh(continues: Boolean){
        val timeOut = 5 * 60  //设备发现5分钟超时
        val continueFlag  = if(continues ) 1 else 0 //超时继续组网设置为1
        val netWorkInfo = DeviceNetworkInfo(System.currentTimeMillis(), "demo")
        val bean = CreateDeviceNetworkReq(timeOut, continueFlag, netWorkInfo)
        getNetMeshManger().startNetMeshMatching(bean, MeshModeEnum.STANDARD, object: CommonCallbacks.CompletionCallbackWithParam<CreateDeviceNetworkResp>{
            override fun onSuccess(t: CreateDeviceNetworkResp?) {
                netMeshResponse = t
                ToastUtils.showToast("开始组网")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast("开始组网失败:$msg")
            }
        })
    }

    private fun setAdapter() {
        adapter = NetMeshDemoAdapter()
        adapter.setOnClickListener {
           val nodeId = getMultiDeviceOperator().getDroneDeviceById(it)?.getNodeId()
            nodeId?.let { setControlMode(ControlMode.SINGLE, it) }

        }
        adapter.dataList = dataList
        binding.listView.adapter = adapter
    }

    private fun updateData() {
        dataList.clear()

        //飞机列表
        val drones = getMultiDeviceOperator().getDroneDevices()
        //本地遥控器
        val localRemoteDevice = getMultiDeviceOperator().getLocalRemoteDevice()
        //非本地遥控器
        val remotes =  getMultiDeviceOperator().getRemoteDevices()

        dataList.addAll(drones)
        dataList.add(localRemoteDevice)
        dataList.addAll(remotes)

        adapter.notifyDataSetChanged()
    }

}