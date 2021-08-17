package com.masiuk.extraedgebackgesture

import de.robv.android.xposed.XposedHelpers

class Classes {
    companion object {
        var enabled = true
        var initialized = false
        const val ModPackageName = "com.android.systemui"

        private const val String_NavigationBarEdgePanel = "com.android.systemui.statusbar.phone.NavigationBarEdgePanel"
        private const val String_EdgeBackGestureHandler = "com.android.systemui.statusbar.phone.EdgeBackGestureHandler"

        var NavigationBarEdgePanel: Class<*>? = null
        var EdgeBackGestureHandler: Class<*>? = null

        fun initClasses(classLoader: ClassLoader?) {
            NavigationBarEdgePanel = XposedHelpers.findClass(String_NavigationBarEdgePanel, classLoader)
            EdgeBackGestureHandler = XposedHelpers.findClass(String_EdgeBackGestureHandler, classLoader)
        }
    }
}