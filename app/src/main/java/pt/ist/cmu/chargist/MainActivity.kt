package pt.ist.cmu.chargist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pt.ist.cmu.chargist.ui.navigation.ChargISTNavigation
import pt.ist.cmu.chargist.ui.theme.ChargISTTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep splash screen visible until app is ready
        splashScreen.setKeepOnScreenCondition { true }
        enableEdgeToEdge()
        setContent {
            splashScreen.setKeepOnScreenCondition { false }
            ChargISTTheme {
                ChargISTApp()
            }
        }
    }
}

@Composable
fun ChargISTApp() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        ChargISTNavigation()
    }
}