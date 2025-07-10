package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.autel.sdk.debugtools.MAIN_FRAGMENT_PAGE_TITLE
import com.autel.sdk.debugtools.MSDKInfoVm
import com.autel.sdk.debugtools.R

/**
 * base fragment for debug tools
 * Copyright: Autel Robotics
 * @author huangsihua on 2022/12/17.
 */
open class AutelFragment : Fragment() {

    protected val TAG = this::class.java.simpleName
    protected val handler by lazy {
        Handler(requireContext().mainLooper) {
            return@Handler handleMessage(it)
        }
    }
    protected val msdkInfoVm: MSDKInfoVm by activityViewModels()

    open fun updateTitle() {
        arguments?.let {
            val title = it.getInt(MAIN_FRAGMENT_PAGE_TITLE, R.string.debug_testing_tools)
            msdkInfoVm.mainTitle.value = getString(R.string.debug_testing_tools)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateTitle()
    }

    open fun handleMessage(msg: Message): Boolean {
        return false
    }

    protected fun showToast(msg: String) {
        if (isAdded) {
            handler.post {
                try {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}