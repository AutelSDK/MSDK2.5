package com.autel.sdk.debugtools.listener

import android.view.View
import android.widget.AdapterView

abstract class OnItemSelectedListener: AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

    }
    override fun onNothingSelected(parent: AdapterView<*>?) {

    }
}