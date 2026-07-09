package xyz.larkzhh.lime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import xyz.larkzhh.lime.navigation.AppNavGraph
import xyz.larkzhh.lime.ui.theme.LimeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LimeTheme {
                AppNavGraph()
            }
        }
    }
}