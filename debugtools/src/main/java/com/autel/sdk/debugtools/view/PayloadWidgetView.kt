package com.autel.sdk.debugtools.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.widget.AppCompatImageView
import com.autel.drone.sdk.vmodelx.module.payload.WidgetType
import com.autel.drone.sdk.vmodelx.module.payload.data.bean.IconFilePath
import com.autel.drone.sdk.vmodelx.module.payload.widget.PayloadWidget
import com.autel.sdk.debugtools.R

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/17
 */
class PayloadWidgetView : AppCompatImageView {

    private var payloadWidget: PayloadWidget? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setPayloadWidget(widget: PayloadWidget) {
        payloadWidget = widget

        val widgetType = WidgetType.findWidgetType(widget.widgetType)
        val iconFilePath = widget.iconFilePath
        if (iconFilePath == null || widgetType == WidgetType.LIST || !widget.subItemsList.isNullOrEmpty())
            return

        totalIconResource()
    }

    fun totalIconResource() {
        val iconFilePath = payloadWidget?.iconFilePath
        if (payloadWidget == null || iconFilePath == null)
            return

        val resId = if (isSelected) {
            isSelected = false
            findIconRes(iconFilePath.unSelectedIconPath)
        } else {
            isSelected = true
            findIconRes(iconFilePath.selectedIconPath)
        }

        if (resId != -1)
            setImageResource(resId)
    }

    private fun findIconRes(resStr: String?): Int {
        return when (resStr) {
            "icon_button1.png" -> {
                R.mipmap.icon_button1
            }

            "icon_list_item1.png" -> {
                R.mipmap.icon_list_item1
            }

            "icon_list_item2.png" -> {
                R.mipmap.icon_list_item2
            }

            "icon_switch_select.png" -> {
                R.mipmap.icon_switch_select
            }

            "icon_switch_unselect.png" -> {
                R.mipmap.icon_switch_unselect
            }

            "icon_scale.png" -> {
                R.mipmap.icon_scale
            }

            else -> {
                -1
            }
        }
    }
}