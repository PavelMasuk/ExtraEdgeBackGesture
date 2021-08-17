package com.masiuk.extraedgebackgesture

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.VibrationEffect
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.masiuk.extraedgebackgesture.Classes.Companion.EdgeBackGestureHandler
import com.masiuk.extraedgebackgesture.Classes.Companion.ModPackageName
import com.masiuk.extraedgebackgesture.Classes.Companion.NavigationBarEdgePanel
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ExtraEdgeBackGesture : IXposedHookLoadPackage {
    private val debug = "masiuk.DEBUG"
    private var screenWidth = 0
    private var screenHeight = 0

    private var didReachCenter = false
    private var didVibrate = false
    private var startFromLeft = false
    private var triggerBack = true
    private var backupArrowColor = 0
    private var vibrationHelper: Any? = null
    private var arrowColorAnimator: ValueAnimator? = null

    private var navBarEdgePanel: View? = null

    @SuppressLint("StaticFieldLeak")
    var context: Context? = null

    @Suppress("DEPRECATION")
    private fun killForegroundApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        val packageManager: PackageManager = context?.packageManager!!
        var defaultHomePackage = "com.android.launcher"
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(intent, 0)
        if (res?.activityInfo?.packageName != "android")
            defaultHomePackage = res?.activityInfo?.packageName!!

        val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val apps = activityManager.getRunningTasks(1)
        var targetKilled: String? = null
        if (apps.size > 0) {
            val componentName = apps[0].topActivity
            if (componentName!!.packageName != "com.android.systemui" && !componentName.packageName.startsWith(defaultHomePackage)) {
                targetKilled = componentName.packageName
                try {
                    val service = callMethod(activityManager, "getService")
                    callMethod(service, "removeTask", apps[0].taskId)
                } catch (ignore: Throwable) {
                }
            }
        }
        if (targetKilled != null)
            try {
                targetKilled = packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetKilled, 0)) as String
                Toast.makeText(context, "$targetKilled killed", Toast.LENGTH_SHORT).show()
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
        else
            Toast.makeText(context, "Nothing to kill", Toast.LENGTH_SHORT).show()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        Log.d(debug, "packageName: ${lpparam!!.packageName}")
        //Self hook checks if xposed module is enabled
        if (lpparam.packageName.equals("com.masiuk.extraedgebackgesture"))
            findAndHookMethod("com.masiuk.extraedgebackgesture.MainActivity", lpparam.classLoader, "isModuleEnabled", XC_MethodReplacement.returnConstant(true))

        if (lpparam.packageName == ModPackageName
//            && !Classes.initialized
        ) {
            Log.d(debug, "VERSION 4")

            Log.d(debug, "after if: initialized: ${Classes.initialized}")
            Classes.initClasses(lpparam.classLoader)   //  CLASS NOT FOUND ERROR
            Log.d(debug, "after init classes")
            initBackGestureMod(Classes.enabled)
            Log.d(debug, "after init classes")
        }
    }

    private fun initBackGestureMod(enable: Boolean) {
        Log.d(debug, "enabled: $enable")
        findAndHookMethod(NavigationBarEdgePanel, "handleMoveEvent", MotionEvent::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d(debug, "handleMoveEvent: afterHookedMethod")
                navBarEdgePanel = param.thisObject as View
                vibrationHelper = getObjectField(param.thisObject, "mVibratorHelper")
                triggerBack = getObjectField(param.thisObject, "mTriggerBack") as Boolean
                arrowColorAnimator = getObjectField(param.thisObject, "mArrowColorAnimator") as ValueAnimator
            }
        })
        findAndHookMethod(NavigationBarEdgePanel, "updateIsDark", Boolean::class.javaPrimitiveType, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                Log.d(debug, "updateIsDark: beforeHookedMethod, didReachCenter: $didReachCenter")
                if (didReachCenter) param.result = 0
            }
        })
        findAndHookMethod(EdgeBackGestureHandler, "onMotionEvent", MotionEvent::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                Log.d(debug, "onMotionEvent: beforeHookedMethod, enable: $enable")
                if (!enable) return

                val event = param.args[0] as MotionEvent
                if (context == null) {
                    context = getObjectField(param.thisObject, "mContext") as Context
                    val size = Point()
                    context?.display?.getRealSize(size)
                    screenWidth = size.x.coerceAtMost(size.y)
                    screenHeight = size.x.coerceAtLeast(size.y)
                }
                val mAllowGesture = getObjectField(param.thisObject, "mAllowGesture") as Boolean
                val orientation: Int = context?.resources?.configuration?.orientation!!
                val currentWidth: Int = if (orientation == Configuration.ORIENTATION_PORTRAIT) screenWidth else screenHeight
                val divider = if (orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3
                val threshold = currentWidth / divider
                if (event.action == MotionEvent.ACTION_DOWN) startFromLeft = event.x < currentWidth / 2
                if (event.action == MotionEvent.ACTION_MOVE && mAllowGesture) {
                    val condition = if (startFromLeft) event.x > threshold else event.x < currentWidth - threshold
                    if (condition && !didReachCenter) {
                        didReachCenter = true
                        changeColor()
                        if (!didVibrate) {
                            vibrate()
                            didVibrate = true
                        }
                    }
                    if (!condition && didReachCenter) {
                        didReachCenter = false
                        revertColor()
                    }
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    didVibrate = false
                    if (didReachCenter) {
                        if (triggerBack) killForegroundApp() else triggerBack = true
                        didReachCenter = false
                    }
                }
            }
        })
        Classes.initialized = true
    }

    private fun changeColor() {
        backupArrowColor = getIntField(navBarEdgePanel, "mCurrentArrowColor")
        setArrowColor(Color.RED)
    }

    private fun revertColor() {
        setArrowColor(backupArrowColor)
    }

    private fun setArrowColor(color: Int) {
        arrowColorAnimator?.cancel()
        setIntField(navBarEdgePanel, "mCurrentArrowColor", color)
        (getObjectField(navBarEdgePanel, "mPaint") as Paint).color = color
        navBarEdgePanel?.invalidate()
    }

    private fun vibrate() = callMethod(vibrationHelper, "vibrate", VibrationEffect.EFFECT_TICK)
}