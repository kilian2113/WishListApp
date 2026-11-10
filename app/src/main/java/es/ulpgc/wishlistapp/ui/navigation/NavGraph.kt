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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.ulpgc.wishlistapp.data.repository.AuthRepositoryImpl
import es.ulpgc.wishlistapp.ui.register.RegisterScreen
import es.ulpgc.wishlistapp.ui.register.RegisterViewModel
import es.ulpgc.wishlistapp.ui.register.RegisterViewModelFactory

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
    startDestination: String = Screen.Register.route
) {
    val authRepository = AuthRepositoryImpl()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Register.route) {
            val registerViewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(authRepository)
            )

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
                    text = "Has completado el registro exitosamente.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        composable(Screen.SignIn.route) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
