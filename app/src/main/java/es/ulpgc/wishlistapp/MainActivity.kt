package es.ulpgc.wishlistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import es.ulpgc.wishlistapp.ui.navigation.NavGraph
import es.ulpgc.wishlistapp.ui.theme.WishListAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WishListAppTheme {
                NavGraph()
            }
        }
    }
}
