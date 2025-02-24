package com.autel.drone.demo

import android.app.Application
import com.autel.drone.sdk.vmodelx.SDKInitConfig
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.SDKLogManager

class DemoApplicationEx : Application() {

    override fun onCreate() {
        super.onCreate()
        //SDKManager.get().init(applicationContext, true)


        /**
         * MSDK init parameter setting
         */
        val sdkInitConfig = SDKInitConfig().apply {
            /**
             * debug mode
             */
            debug = false

            /**
             * app type: Reserved field, do not set
             */
            appType = null


            /**
             * video render thread switch
             * if nest mode, can close render thread, Improve performance
             */
            bRender = true 



            /**
             * handle log by implementation of IAutelLog
             */
            log = null

            /**
             * true : single mode , false: mesh mode
             * can auto config when sdk initialize, if not success ,you can manually config
             */
            single = false

            /**
             * handle data storage by implementation of IAutelStorage
             */
            storage = null

        }
        SDKManager.get().init(applicationContext, sdkInitConfig)

        SDKLogManager.get().setSDKLogPath(applicationContext)

        println("SDKManager V=${SDKManager.get().getSDKVersion()}")
    }
}