package com.suarezmillan.finanzas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.ui.RootGate
import com.suarezmillan.finanzas.ui.theme.FinanzasTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle()
            val preset by settings.themePreset.collectAsStateWithLifecycle()

            FinanzasTheme(mode = mode, preset = preset) {
                RootGate()
            }
        }
    }
}
