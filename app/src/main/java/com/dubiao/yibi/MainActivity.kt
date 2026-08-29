package com.dubiao.yibi

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dubiao.yibi.ui.LedgerViewModel
import com.dubiao.yibi.ui.YiBiApp
import com.dubiao.yibi.ui.theme.YiBiTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val chinese = Locale.SIMPLIFIED_CHINESE
        Locale.setDefault(chinese)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(chinese)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YiBiTheme {
                val application = application as YiBiApplication
                val viewModel: LedgerViewModel = viewModel(
                    factory = LedgerViewModel.Factory(application.repository, application.userPreferences),
                )
                YiBiApp(viewModel)
            }
        }
    }
}
