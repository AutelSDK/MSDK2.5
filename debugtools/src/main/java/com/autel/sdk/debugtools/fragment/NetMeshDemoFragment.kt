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
import com.autel.sdk.debugtools.R
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
                override fun onSuccess(t: Int?) { ToastUtils.showToast(getString(R.string.finish_mesh)) }
                override fun onFailure(error: IAutelCode, msg: String?) { ToastUtils.showToast(getString(R.string.finish_mesh_fail, msg)) }
            })
        }

        binding.btnNetMeshDisband.setOnClickListener{
            var groupId: Long = netMeshResponse?.info?.groupId ?: 0
            getNetMeshManger().disbandNetMesh(groupId, object : CommonCallbacks.CompletionCallbackWithParam<Int>{
                override fun onSuccess(t: Int?) {
                    ToastUtils.showToast(getString(R.string.diss_succuss))
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    ToastUtils.showToast(getString(R.string.diss_fail, msg))
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
                    ToastUtils.showToast(getString(R.string.stream_craft, it.deviceNumber().toString()))
                }

            }
        })

        DeviceManager.getMultiDeviceOperator().addControlChangeListener(object : IControlDroneListener{
            override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
                ToastUtils.showToast(getString(R.string.control_mode, mode, droneList.map { it.deviceNumber() }))
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
                        ToastUtils.showToast(getString(R.string.diss_mesh))
                    }
                    DeviceNetworkStatusType.TIMEOUT->{
                        ToastUtils.showToast(getString(R.string.mesh_timeout))
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
            override fun onSuccess(t: Int?) { ToastUtils.showToast(getString(R.string.relay_setting_success, deviceId.toString())) }
            override fun onFailure(error: IAutelCode, msg: String?) { ToastUtils.showToast(getString(R.string.relay_setting_fail, msg)) }
        })
    }

    /**
     * 设置watch码流的飞机
     */
    private fun setWatchDrone(deviceId: Int){
        getNetMeshManger().setWatchDevice(arrayListOf(deviceId), object: CommonCallbacks.CompletionCallbackWithParam<Int>{
            override fun onSuccess(t: Int?) { ToastUtils.showToast(getString(R.string.steam_setting_success, deviceId.toString())) }
            override fun onFailure(error: IAutelCode, msg: String?) { ToastUtils.showToast(getString(R.string.stream_setting_fail, msg)) }
        })
    }

    /**
     * id 为 NodeId 或 GroupId
     *
     */
    private fun setControlMode(mode: ControlMode, id:Int){
        var log: String = ""
        if(mode == ControlMode.ALL){ //全控：Id被忽略
            log = getString(R.string.all_control)
        } else if(mode == ControlMode.GROUP){
            log = getString(R.string.group_control, id.toString())
        } else if(mode == ControlMode.SINGLE){
            log = getString(R.string.single_congrol, id.toString())
        }

        getNetGroupManger().switchControlMode(mode,id,object : CommonCallbacks.CompletionCallbackWithParam<Void>{
            override fun onSuccess(t: Void?) {
                ToastUtils.showToast(getString(R.string.setting_success, log))
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast(getString(R.string.setting_fail, log, msg))
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
                ToastUtils.showToast(getString(R.string.start_mesh))
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast(getString(R.string.start_mesh_fail, msg))
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