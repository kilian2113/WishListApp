package es.ulpgc.wishlistapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private data class BottomNavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavigationItems = listOf(
    BottomNavigationItem(Screen.Home.route, "Inicio", Icons.Default.Home),
    BottomNavigationItem(Screen.MyLists.route, "Mis listas", Icons.Default.CardGiftcard),
    BottomNavigationItem(Screen.Friends.route, "Amigos", Icons.Default.Group),
    BottomNavigationItem(Screen.Profile.route, "Perfil", Icons.Default.Person)
)

@Composable
fun BottomNavigationMenu(
    selectedRoute: String?,
    onDestinationSelected: (String) -> Unit
) {
    NavigationBar {
        bottomNavigationItems.forEach { item ->
            NavigationBarItem(
                selected = selectedRoute == item.route,
                onClick = { onDestinationSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
