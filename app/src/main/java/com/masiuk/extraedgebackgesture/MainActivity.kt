package com.masiuk.extraedgebackgesture

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {
    private var mainSwitch: SwitchMaterial? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        mainSwitch = findViewById(R.id.enable_module)
        mainSwitch?.isChecked = true
        mainSwitch?.visibility = if (isModuleEnabled()) View.VISIBLE else View.GONE
        mainSwitch?.setOnCheckedChangeListener { _, isChecked ->
            Classes.enabled = isChecked
            Classes.initialized = false
        }
        findViewById<MaterialTextView>(R.id.module_not_enabled_message).visibility = if (isModuleEnabled()) View.GONE else View.VISIBLE
    }

    private fun isModuleEnabled(): Boolean = false
}