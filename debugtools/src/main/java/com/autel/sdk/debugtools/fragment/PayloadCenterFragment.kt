package com.autel.sdk.debugtools.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.findNavController
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentPayloadCenterBinding

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/15
 */
class PayloadCenterFragment : AutelFragment(), OnClickListener {

    private lateinit var binding: FragmentPayloadCenterBinding
    companion object {
        const val KEY_PAYLOAD_INDEX_TYPE = "payload_index_type"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPayloadCenterBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.btnDataPage.setOnClickListener(this)
        binding.btnWidgetPage.setOnClickListener(this)
        binding.btnOperatePage.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_data_page -> {
                showSelectListDialog {
                    val bundle = Bundle()
                    bundle.putString(KEY_PAYLOAD_INDEX_TYPE, it.name)
                    v.findNavController().navigate(R.id.action_open_payload_data_page, bundle)
                }
            }

            R.id.btn_widget_page -> {
                showSelectListDialog {
                    val bundle = Bundle()
                    bundle.putString(KEY_PAYLOAD_INDEX_TYPE, it.name)
                    v.findNavController().navigate(R.id.action_open_payload_widget_page, bundle)
                }
            }
            R.id.btn_operate_page->{
                showSelectListDialog {
                    val bundle = Bundle()
                    bundle.putString(KEY_PAYLOAD_INDEX_TYPE, it.name)
                    v.findNavController().navigate(R.id.action_open_payload_operate_page, bundle)
                }
            }
        }
    }

    private fun showSelectListDialog(callback: (PayloadIndexType) -> Unit) {
        var items = PayloadIndexType.values()
        items = (items.toMutableList().subList(0, items.size - 1)).toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)

        AlertDialog.Builder(requireContext())
            .setTitle("Select an Payload")
            .setAdapter(adapter) { _, which ->
                val selectedItem = items[which]
                callback.invoke(selectedItem)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}