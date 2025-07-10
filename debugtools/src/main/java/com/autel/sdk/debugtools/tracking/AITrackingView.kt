package com.autel.sdk.debugtools.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.DetectTrackNotifyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.TrackAreaBean
import com.autel.sdk.debugtools.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class AITrackingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    View(context, attrs, defStyleAttr) {

    companion object {
        const val TRACK_STATUS_NONE = 0
        const val TRACK_STATUS_DETECTING = 1
        const val TRACK_STATUS_LOCKING = 2
        const val TRACK_STATUS_TRACKING = 3
    }

    private val trackTarget: Bitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.debug_ic_track_target) }

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            color = "#03FEF4".toColorInt()
            strokeWidth = 5f
            style = Paint.Style.STROKE
            textSize = resources.getDimension(R.dimen.debug_sp_15)
        }

    /**
     * targets that can be tracked
     */
    private val trackableTarget = mutableListOf<Target>()

    // radian of the rectangle
    private val radian = 10f

    private var targetX = -1f
    private var targetY = 1f
    private var targetWidth = 1f
    private var targetHeight = 1f

    private val selectTargetRect = RectF()

    private var downX = 0f
    private var downY = 0f

    private var moveX = 0f
    private var moveY = 0f

    private var isDragSelectTarget = false
    var trackWorkStatus: Int = TRACK_STATUS_NONE
        set(value) {
            field = value
            postInvalidate()
        }

    /**
     * lost target flag
     */
    private var isLostTarget = false

    private val lock = Any()

    private var mOnTrackChangeListener: OnTrackChangeListener? = null

    private var refreshTime = SystemClock.elapsedRealtime()

    //Minimum recognition rectangle size
    private val targetLeastSize: Float by lazy { context.resources.getDimension(R.dimen.debug_dimen_dp_48) }

    //Aspect Ratio of video in AIService
    private var aiWh = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        trackableTarget.forEach {
            canvas.drawBitmap(trackTarget, it.x - trackTarget.width / 2, it.y - trackTarget.height / 2, paint)
        }

        if (isDragSelectTarget && !isTrackingTarget() && !isInvalid()) {
            paint.strokeJoin = Paint.Join.ROUND
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.color = "#FF03FEF4".toColorInt()
            canvas.drawRect(downX, downY, moveX, moveY, paint)
        }
        drawTargetRect(canvas)
    }

    private fun drawTargetRect(canvas: Canvas) {
        if (targetX == -1f || targetY == -1f || targetWidth == -1f || targetHeight == -1f) {
            return
        }
        paint.alpha = 255
        paint.color = if (isLostTarget) Color.RED else "#03FEF4".toColorInt()
        paint.style = Paint.Style.STROKE
        val x = targetX + targetWidth
        val y = targetY + targetHeight

        selectTargetRect.set(
            max(0f, min(targetX, x)), max(0f, min(targetY, y)),
            max(targetLeastSize, max(targetX, x)), max(targetLeastSize, max(targetY, y))
        )

        val layer = canvas.saveLayer(
            selectTargetRect.left - paint.strokeWidth, selectTargetRect.top - paint.strokeWidth,
            selectTargetRect.right + paint.strokeWidth, selectTargetRect.bottom + paint.strokeWidth, paint
        )
        paint.strokeWidth = 5f
        canvas.drawRoundRect(selectTargetRect, radian, radian, paint)

        //paint blending mode
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        paint.strokeWidth = 10f
        val w = abs(targetWidth)
        val h = abs(targetHeight)

        // left
        canvas.drawLine(
            selectTargetRect.left,
            selectTargetRect.top + h * 0.2f,
            selectTargetRect.left,
            selectTargetRect.top + h - h * 0.2f,
            paint
        )
        // top
        canvas.drawLine(
            selectTargetRect.left + w * 0.2f,
            selectTargetRect.top,
            selectTargetRect.left + w - w * 0.2f,
            selectTargetRect.top,
            paint
        )
        // right
        canvas.drawLine(
            selectTargetRect.right,
            selectTargetRect.top + h * 0.2f,
            selectTargetRect.right,
            selectTargetRect.top + h - h * 0.2f,
            paint
        )
        // bottom
        canvas.drawLine(
            selectTargetRect.left + w * 0.2f,
            selectTargetRect.bottom,
            selectTargetRect.left + w - w * 0.2f,
            selectTargetRect.bottom,
            paint
        )
        paint.xfermode = null
        canvas.restoreToCount(layer)
    }

    fun refreshTrackableTarget(target: DetectTrackNotifyBean?) {
        if (SystemClock.elapsedRealtime() - refreshTime < 100) return
        refreshTime = SystemClock.elapsedRealtime()

        trackableTarget.clear()
        if (width == 0 || height == 0) {
            postOnAnimation {
                refresh(target)
            }
        } else {
            refresh(target)
        }
    }

    fun selectTarget(target: TrackAreaBean?) {
        if (SystemClock.elapsedRealtime() - refreshTime < 100) return
        refreshTime = SystemClock.elapsedRealtime()

        trackableTarget.clear()
        if (target != null && height != 0 && target.resolutionHeight != 0) {
            aiWh = 1.0f * target.resolutionWidth / target.resolutionHeight
            val screenWh = 1.0f * width / height
            if (target.objNum > 0) {
                val bean = target.infoList?.firstOrNull()
                if (bean != null) {
                    // Convert the coordinates from AIService to screen coordinates
                    val screenX = TransCodecHelper.transOriginXLocationToCrop(bean.startX, aiWh, screenWh)
                    val screenY = TransCodecHelper.transOriginYLocationToCrop(bean.startY, aiWh, screenWh)
                    val screenXSize = TransCodecHelper.transOriginXSizeToCrop(bean.width, aiWh, screenWh)
                    val screenYSize = TransCodecHelper.transOriginYSizeToCrop(bean.height, aiWh, screenWh)

                    isLostTarget = bean.status == 0
                    targetX = screenX * width
                    targetY = screenY * height
                    targetWidth = screenXSize * width
                    targetHeight = screenYSize * height
                } else {
                    resetLockTarget()
                }
            }
        } else {
            resetLockTarget()
        }
        postInvalidate()
    }

    private fun resetLockTarget() {
        isLostTarget = false
        targetX = -1f
        targetY = -1f
        targetWidth = -1f
        targetHeight = -1f
    }

    private val tempRectF = RectF()

    /**
     * Refresh the targets detected by AIService
     */
    private fun refresh(target: DetectTrackNotifyBean?) {
        if (target != null) {
            aiWh = 1.0f * target.resolutionWidth / target.resolutionHeight

            val screenWh = 1.0f * width / height
            target.infoList.forEach {

                // Convert the coordinates from AIService to screen coordinates
                val screenX = TransCodecHelper.transOriginXLocationToCrop(it.startX, aiWh, screenWh)
                val screenY = TransCodecHelper.transOriginYLocationToCrop(it.startY, aiWh, screenWh)
                val screenXSize = TransCodecHelper.transOriginXSizeToCrop(it.width, aiWh, screenWh)
                val screenYSize = TransCodecHelper.transOriginYSizeToCrop(it.height, aiWh, screenWh)
                tempRectF.set(
                    screenX,
                    screenY,
                    (screenX + screenXSize),
                    (screenY + screenYSize)
                )
                val t = Target(
                    tempRectF.centerX() * width,
                    tempRectF.centerY() * height,
                    left = tempRectF.left * width,
                    top = tempRectF.top * height,
                    right = tempRectF.right * width,
                    bottom = tempRectF.bottom * height,
                    it.startX, it.startY,
                    it.width, it.height
                )
                trackableTarget.add(t)
            }
            resetLockTarget()
            postInvalidate()
        }
    }

    private fun isSelectTarget(x: Float, y: Float): Target? {
        synchronized(lock) {
            trackableTarget.forEach {
                if (x >= it.left && x <= it.right && y >= it.top && y <= it.bottom) {
                    return it
                }
            }
            return null
        }
    }

    private fun isTrackingTarget() = trackWorkStatus == TRACK_STATUS_TRACKING

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragSelectTarget = false
                reset()
                downX = x
                downY = y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isTrackingTarget()) {
                    if (isDragSelectTarget || (abs(x - downX) > trackTarget.width && abs(y - downY) > trackTarget.height)) {
                        moveX = x
                        moveY = y
                        isDragSelectTarget = true
                        invalidate()
                    } else {
                        isDragSelectTarget = false
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!isTrackingTarget()) {
                    if (isDragSelectTarget) {
                        val targetX = downX
                        val targetY = downY
                        val targetWidth = moveX - downX
                        val targetHeight = moveY - downY
                        val tx = targetX + targetWidth
                        val ty = targetY + targetHeight

                        val temp = RectF(min(targetX, tx), min(targetY, ty), max(targetX, tx), max(targetY, ty))
                        //坐标还原：

                        val screenLeftX = temp.left / (width)
                        val screenTopY = temp.top / height
                        val screenSizeX = temp.width() / (width)
                        val screenSizeY = temp.height() / height

                        val screenWh = 1.0f * width / height
                        if (aiWh != 0f && screenWh != 0f) {
                            // Convert the coordinates from screen to AIService coordinates
                            val aiLeftX = TransCodecHelper.transCropXLocationToOrigin(screenLeftX, aiWh, screenWh)
                            val aiTopY = TransCodecHelper.transCropYLocationToOrigin(screenTopY, aiWh, screenWh)
                            val aiSizeX = TransCodecHelper.transCropXSizeToOrigin(screenSizeX, aiWh, screenWh)
                            val aiSizeY = TransCodecHelper.transCropYSizeToOrigin(screenSizeY, aiWh, screenWh)
                            mOnTrackChangeListener?.onDragSelectTarget(this, aiLeftX, aiTopY, aiSizeX, aiSizeY)
                        } else {
                            mOnTrackChangeListener?.onDragSelectTarget(this, screenLeftX, screenTopY, screenSizeX, screenSizeY)
                        }
                        isDragSelectTarget = false
                        invalidate()
                    } else {
                        val target = isSelectTarget(x, y)
                        if (target != null) {
                            mOnTrackChangeListener?.onDragSelectTarget(this, target.orgX, target.orgY, target.orgW, target.orgH)
                            reset()
                            return true
                        }
                    }
                }
                reset()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun reset() {
        downX = -1f
        downY = -1f
        moveX = -1f
        moveY = -1f
    }

    fun cleanup() {
        synchronized(lock) {
            trackableTarget.clear()
        }
        reset()
        resetLockTarget()
        postInvalidate()
    }

    private fun isInvalid() = downX == -1f || downY == -1f || moveX == -1f || moveY == -1f

    fun setOnTrackChangeListener(listener: OnTrackChangeListener) {
        this.mOnTrackChangeListener = listener
    }

    interface OnTrackChangeListener {
        fun onDragSelectTarget(view: AITrackingView, x: Float, y: Float, width: Float, height: Float)
    }

    private data class Target(
        val x: Float, val y: Float,
        val left: Float, val top: Float,
        val right: Float, val bottom: Float,
        val orgX: Float, val orgY: Float,
        val orgW: Float, val orgH: Float,
    )
}