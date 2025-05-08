package pt.ist.cmu.chargist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import pt.ist.cmu.chargist.ui.navigation.ChargISTNavigation
import pt.ist.cmu.chargist.ui.theme.ChargISTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
        // new nav host – no parameters required
        ChargISTNavigation()          // ← this is the only change
    }
}