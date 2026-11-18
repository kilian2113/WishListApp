package es.ulpgc.wishlistapp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.ulpgc.wishlistapp.ui.login.LoginScreen
import es.ulpgc.wishlistapp.ui.login.LoginViewModel
import es.ulpgc.wishlistapp.ui.register.RegisterScreen
import es.ulpgc.wishlistapp.ui.register.RegisterViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    data object Register : Screen("register")
    data object Home : Screen("home/{userName}") {
        fun createRoute(userName: String) = "home/$userName"
    }
    data object SignIn : Screen("sign_in")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.SignIn.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SignIn.route) {
            val loginViewModel: LoginViewModel = koinViewModel()

            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { userName ->
                    navController.navigate(Screen.Home.createRoute(userName)) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            val registerViewModel: RegisterViewModel = koinViewModel()

            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route)
                },
                onRegisterSuccess = { userName ->
                    navController.navigate(Screen.Home.createRoute(userName)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: "Usuario"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡Bienvenido/a, $userName!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Has iniciado sesión exitosamente.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
