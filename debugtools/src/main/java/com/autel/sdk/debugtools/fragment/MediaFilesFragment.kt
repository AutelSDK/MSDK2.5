package com.autel.sdk.debugtools.fragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.http.BaseRequest
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IAlbumManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.SpeedModeManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.AlbumFolderResultBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.AlbumResultBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.AutelAlbumBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.AutelMediaBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.MediaTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.OrderTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.StorageTypeEnum
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentMideaFilesBinding
import java.io.File


/**
 * all media files status generate with drone
 * Copyright: Autel Robotics
 * @author huangsihua on 2022/12/17.
 */
@SuppressLint("SetTextI18n")
class MediaFilesFragment : AutelFragment() {

    companion object {
        private const val TAG = "MediaFilesFragment"

        val MediaTypes = listOf(MediaTypeEnum.MEDIA_ALL, MediaTypeEnum.MEDIA_PHOTO, MediaTypeEnum.MEDIA_VIDEO)
        val StorageType = listOf(StorageTypeEnum.EMMC, StorageTypeEnum.SD)
        val OrderType = listOf(OrderTypeEnum.NORMAL, OrderTypeEnum.REVERSE)
    }

    private lateinit var binding: FragmentMideaFilesBinding

    private var mediaTypeEnum = MediaTypeEnum.MEDIA_ALL
    private var storageType = StorageTypeEnum.EMMC
    private var orderType = OrderTypeEnum.NORMAL
    private val pageCount = 30   //MAX 500

    private val handler = Handler(Looper.getMainLooper())

    private val mediaList = mutableListOf<AutelMediaBean>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMideaFilesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initSpinnerOption(binding.sMediaType, MediaTypes.map { it.name })
        initSpinnerOption(binding.sStorageType, StorageType.map { it.name })
        initSpinnerOption(binding.sOrderType, OrderType.map { it.name })
        updateSelection()
    }

    /**
     * Get album service interface.
     */
    private fun getAlbumManager(): IAlbumManager? {
        if (!DeviceManager.getDeviceManager().isSingleControl()) {
            ToastUtils.showToast("It is suggested to switch to the single-control mode to improve the access speed.")
        }
        return DeviceManager.getDeviceManager().getFirstDroneDevice()?.getAlbumManager()
    }

    override fun onStart() {
        super.onStart()
        changeHighSpeedMode()
    }

    override fun onStop() {
        super.onStop()
        changeNormalSpeedMode()
    }

    /**
     * Change to high download speed mode.
     */
    private fun changeHighSpeedMode() {
        DeviceManager.getDeviceManager().getFirstDroneDevice()?.let {
            SpeedModeManager.changeToHighDownload(it, 1) {}
        }
    }

    /**
     * Change to normal speed mode, Otherwise, the quality of the image transmission screen will be reduced.
     */
    private fun changeNormalSpeedMode() {
        DeviceManager.getDeviceManager().getFirstDroneDevice()?.let {
            SpeedModeManager.changeToNormalSpeed(it, 2) {
            }
        }
    }

    private fun initSpinnerOption(spinner: Spinner, data: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (spinner) {
                    binding.sMediaType ->  mediaTypeEnum = MediaTypes[position]
                    binding.sStorageType -> storageType = StorageType[position]
                    binding.sOrderType -> orderType = OrderType[position]
                }
                updateSelection()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateSelection(){
        binding.mediaTypeTxt.text = "MediaType:$mediaTypeEnum"
        binding.storageTxt.text = "StorageType:$storageType"
        binding.orderTxt.text = "OrderType:$orderType"
    }

    private fun initView() {
        binding.tvGetMediaList.setOnClickListener {
            binding.tvGetMediaList.isEnabled =false
            getMediaList()
        }

        binding.tvGetAlbumFolderList.setOnClickListener {
            binding.tvGetAlbumFolderList.isEnabled = false
            getMediaFolderList()
        }

        binding.tvAlbumFileDel.setOnClickListener {
            if(mediaList.isEmpty()){
                ToastUtils.showToast("Please get media file list first!")
                return@setOnClickListener
            }
            binding.tvAlbumFileDel.isEnabled= false
            mediaList.firstOrNull()?.let { delMediaFile(it)}
        }

        binding.tvAlbumFileDownload.setOnClickListener {
            if(mediaList.isEmpty()){
                ToastUtils.showToast("Please get media file list first!")
                return@setOnClickListener
            }
            binding.tvAlbumFileDownload.isEnabled = false
            mediaList.firstOrNull()?.let { downloadFile(it)}
        }

        binding.tvAlbumFileCancelDownload.setOnClickListener {
            cancelDownload()
        }
    }

    private fun getMediaList() {
        getAlbumManager()?.getMediaFileList(mediaTypeEnum, storageType,
            "",   //empty for all file, or specify folder
            0, pageCount, orderType, object : CommonCallbacks.CompletionCallbackWithParam<AlbumResultBean> {
                override fun onFailure(error: IAutelCode, msg: String?) {
                    handler.post {
                        binding.tvGetMediaList.isEnabled = true
                        binding.tvResult.text = "getMediaList fail: $error, $msg"
                    }
                }

                override fun onSuccess(t: AlbumResultBean?) {
                    handler.post {
                        binding.tvGetMediaList.isEnabled = true
                        binding.tvResult.text = "getMediaList success: list size = ${t?.pathlist?.size}"
                        t?.let {
                            val list = it.pathlist ?: emptyList()
                            val adapter = ImageAdapter(list)
                            mediaList.clear()
                            mediaList.addAll(list)
                            binding.listView.setLayoutManager(LinearLayoutManager(requireContext()))
                            binding.listView.adapter = adapter
                        }
                    }
                }
            })
    }

    /**
     * Get media folder list, maybe don't have folder.
     */
    private fun getMediaFolderList() {
        getAlbumManager()?.getMediaFolderList(
            storageType,
            orderType,
            object :CommonCallbacks.CompletionCallbackWithParam<AlbumFolderResultBean>{
                override fun onFailure(error: IAutelCode, msg: String?) {
                    handler.post {
                        binding.tvGetAlbumFolderList.isEnabled = true
                        binding.tvResult.text = "getMediaFolderList fail: $error, $msg"
                    }
                }

                override fun onSuccess(t: AlbumFolderResultBean?) {
                    handler.post {
                        binding.tvGetAlbumFolderList.isEnabled = true
                        binding.tvResult.text = "getMediaFolderList success: result = $t"
                        t?.let {
                            SDKLog.i(TAG, "getMediaFolderList $it")
                            val adapter = ImageAdapter(it.folderList ?: emptyList())
                            binding.listView.setLayoutManager(LinearLayoutManager(requireContext()));
                            binding.listView.adapter = adapter
                        }
                    }
                }
            })
    }

    private fun delMediaFile(mediaBean: AutelMediaBean){
        val fileId = mediaBean.index
        getAlbumManager()?.deleteMediaFile(fileId,object :CommonCallbacks.CompletionCallback{

            override fun onFailure(code: IAutelCode, msg: String?) {
                handler.post {
                    binding.tvAlbumFileDel.isEnabled= true
                    binding.tvResult.text = "delMediaFile fail: $code, $msg"
                }
            }

            override fun onSuccess() {
                handler.post {
                    binding.tvAlbumFileDel.isEnabled= true
                    binding.tvResult.text = "delMediaFile success : $mediaBean"
                }
            }
        })
    }

    private var downloadRequest:BaseRequest? = null
    private fun downloadFile(mediaBean: AutelMediaBean){
        val destFile = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + File.separator +  mediaBean.name
        val sourceFile  = mediaBean.getOriginPath()
        downloadRequest = getAlbumManager()?.downloadMediaFile(sourceFile, destFile, object:CommonCallbacks.DownLoadCallbackWithProgress<Double>{
            override fun onFailure(error: IAutelCode) {
                handler.post {
                    binding.tvAlbumFileDownload.isEnabled = true
                    binding.tvResult.text = "download file fail: $error"
                }
            }

            override fun onSuccess(file: File?) {
                handler.post {
                    binding.tvAlbumFileDownload.isEnabled = true
                    binding.tvResult.text = "download success : ${file?.absolutePath}"
                }
            }

            override fun onProgressUpdate(progress: Double, speed: Double) {
                handler.post {
                    binding.tvResult.text = "download success process=$progress, speed=$progress"
                }
            }

        })
    }

    private fun cancelDownload(){
        downloadRequest?.let { getAlbumManager()?.cancelDownload(it) }
    }
}

@SuppressLint("DefaultLocale", "SetTextI18n")
class ImageAdapter(private val mediaList: List<Any>) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.album_media_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaBean = mediaList[position]
        //Glide.with(this).load(R.drawable.your_image).into(imageView);
        //holder.imageView.setImageURI(Uri.parse(mediaBean.getThumbnailPath()))
        if(mediaBean is AutelMediaBean) {
            holder.name.text = mediaBean.name
            holder.url.text = mediaBean.getOriginPath()
            holder.url1.text = mediaBean.getPreviewPath()
            holder.url2.text = mediaBean.getScreennailPath()
            mediaBean.size?.let {
                val size = String.format("%.2f", it.toDouble().div(1024).div(1024))
                holder.sizeView.text = "$size M"
            }
        } else if(mediaBean is AutelAlbumBean){
            holder.name.text = mediaBean.name
            holder.url.text = mediaBean.thumbnail
            holder.sizeView.text =  mediaBean.count.toString()
        }
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.imageView)
        var name: TextView = itemView.findViewById(R.id.textViewName)
        var url: TextView = itemView.findViewById(R.id.textViewAddress)
        var url1: TextView = itemView.findViewById(R.id.textViewAddress1)
        var url2: TextView = itemView.findViewById(R.id.textViewAddress2)
        var sizeView: TextView = itemView.findViewById(R.id.textViewSize)
    }
}