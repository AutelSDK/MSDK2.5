package com.autel.sdk.debugtools.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.DialogRemoterRoleSettingBinding

@SuppressLint("SetTextI18n")
class RemoterRoleSettingDialog : DialogFragment() {

    private lateinit var binding: DialogRemoterRoleSettingBinding
    private var onBtnClick: ((Boolean) -> Unit)? = null
    private var isRemoterMaster = true //whether it is the main remote control

    init {
        val bundle = Bundle()
        arguments = bundle
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogRemoterRoleSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
    }

    private fun initView() {
        binding.ivRemoteMaster.setOnClickListener {
            isRemoterMaster = !isRemoterMaster
            refreshCheckView()
        }

        binding.ivRemoteRelay.setOnClickListener {
            isRemoterMaster = !isRemoterMaster
            refreshCheckView()
        }

        binding.tvConfirm.setOnClickListener {
            saveRoleInfo()
            onBtnClick?.invoke(isRemoterMaster)
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private fun saveRoleInfo() {
        dismiss()
    }

    private fun refreshCheckView() {
        binding.ivRemoteMaster.setImageResource(if (isRemoterMaster) R.drawable.icon_item_checked else R.drawable.icon_item_unchecked)
        binding.ivRemoteRelay.setImageResource(if (isRemoterMaster) R.drawable.icon_item_unchecked else R.drawable.icon_item_checked)
    }

    fun setOnConfirmListener(listener: (Boolean) -> Unit) {
        this.onBtnClick = listener
    }
}