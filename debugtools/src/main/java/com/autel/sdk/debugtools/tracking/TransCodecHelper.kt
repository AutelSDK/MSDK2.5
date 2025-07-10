package com.autel.sdk.debugtools.tracking

/**
 * Created by xulc 2024/6/28
 */
object TransCodecHelper {

    /**
     * Convert original coordinates to cropped coordinates, center cropping
     */
    fun transOriginXLocationToCrop(x: Float, originRatioWh: Float, cropRatioWh: Float): Float {
        val originRatio = originRatioWh
        val cropRatio = cropRatioWh
        if (originRatio == 0.0f || cropRatio == 0.0f) {
            return x
        }
        val screenX: Float

        if (originRatio > cropRatio) {
            // The video will be cropped on the left and right to fully fit the screen height
            screenX = (x - 0.5f) * originRatio / cropRatio + 0.5f
        } else {
            // The video will be cropped at the top and bottom to fully fit the screen width
            screenX = x
        }

        return screenX
    }

    
    fun transOriginYLocationToCrop(y: Float, originRatioWh: Float, cropRatioWh: Float): Float {
        val originRatio = originRatioWh
        val cropRatio = cropRatioWh
        if (originRatio == 0.0f || cropRatio == 0.0f) {
            return y
        }
        val screenY: Float

        if (originRatio > cropRatio) {
            // The video will be cropped on the left and right to fully fit the screen height
            screenY = y
        } else {
            // The video will be cropped at the top and bottom to fully fit the screen width
            screenY = (y - 0.5f) * cropRatio / originRatio + 0.5f
        }
        return screenY
    }


    /**
     * Convert cropped coordinates to original coordinates, center cropping
     */
    fun transCropXLocationToOrigin(x: Float, originRatioWh: Float, cropRatioWh: Float): Float {
        val originRatio = originRatioWh
        val cropRatio = cropRatioWh
        if (originRatio == 0.0f || cropRatio == 0.0f) {
            return x
        }
        val cameraX: Float

        if (originRatio > cropRatio) {
            // The video will be cropped on the left and right to fully fit the screen height
            cameraX = (x - 0.5f) * cropRatio / originRatio + 0.5f
        } else {
            // The video will be cropped at the top and bottom to fully fit the screen width
            cameraX = x
        }
        return cameraX
    }


    fun transCropYLocationToOrigin(y: Float, originRatioWh: Float, cropRatioWh: Float): Float {
        val originRatio = originRatioWh
        val cropRatio = cropRatioWh

        if (originRatio == 0.0f || cropRatio == 0.0f) {
            return y
        }
        val cameraY: Float

        if (originRatio > cropRatio) {
            // The video will be cropped on the left and right to fully fit the screen height
            cameraY = y
        } else {
            // The video will be cropped at the top and bottom to fully fit the screen width
            cameraY = (y - 0.5f) * originRatio / cropRatio + 0.5f
        }
        return cameraY
    }

    fun transOriginXSizeToCrop(x: Float, videoRatioHw: Float, screenRatioHw: Float): Float {
        return transOriginXLocationToCrop(x, videoRatioHw, screenRatioHw) - transOriginXLocationToCrop(0f, videoRatioHw, screenRatioHw)
    }

    fun transOriginYSizeToCrop(y: Float, videoRatioHw: Float, screenRatioHw: Float): Float {
        return transOriginYLocationToCrop(y, videoRatioHw, screenRatioHw) - transOriginYLocationToCrop(0f, videoRatioHw, screenRatioHw)
    }

    fun transCropXSizeToOrigin(x: Float, originRatioWh: Float, cropRatioWh: Float): Float {
        return transCropXLocationToOrigin(x, originRatioWh, cropRatioWh) - transCropXLocationToOrigin(0f, originRatioWh, cropRatioWh)
    }

    fun transCropYSizeToOrigin(y: Float, originRatioWh: Float, cropRatioWh: Float): Float {
        return transCropYLocationToOrigin(y, originRatioWh, cropRatioWh) - transCropYLocationToOrigin(0f, originRatioWh, cropRatioWh)
    }

}