package es.ulpgc.wishlistapp.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.ulpgc.wishlistapp.ui.friends.FriendsScreen
import es.ulpgc.wishlistapp.ui.friends.FriendsViewModel
import es.ulpgc.wishlistapp.ui.login.LoginScreen
import es.ulpgc.wishlistapp.ui.login.LoginViewModel
import es.ulpgc.wishlistapp.ui.profile.FriendProfileScreen
import es.ulpgc.wishlistapp.ui.profile.FriendProfileViewModel
import es.ulpgc.wishlistapp.ui.profile.ProfileScreen
import es.ulpgc.wishlistapp.ui.profile.ProfileViewModel
import es.ulpgc.wishlistapp.ui.register.RegisterScreen
import es.ulpgc.wishlistapp.ui.register.RegisterViewModel
import es.ulpgc.wishlistapp.ui.wishlistdetail.WishListDetailScreen
import es.ulpgc.wishlistapp.ui.wishlistdetail.WishListDetailViewModel
import es.ulpgc.wishlistapp.ui.wishlists.WishListsScreen
import es.ulpgc.wishlistapp.ui.wishlists.WishListsViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    data object Register : Screen("register")
    data object Home : Screen("home/{userName}") {
        fun createRoute(userName: String) = "home/$userName"
    }
    data object SignIn : Screen("sign_in")
    data object MyLists : Screen("my_lists")
    data object Friends : Screen("friends")
    data object Profile : Screen("profile")
    data object FriendProfile : Screen("friend_profile/{friendUid}") {
        fun createRoute(friendUid: String) = "friend_profile/${Uri.encode(friendUid)}"
    }
    data object WishListDetail : Screen("wishlist_detail/{listId}") {
        fun createRoute(listId: String) = "wishlist_detail/${Uri.encode(listId)}"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.SignIn.route
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val mainRoutes = setOf(
        Screen.Home.route,
        Screen.MyLists.route,
        Screen.Friends.route,
        Screen.Profile.route,
        Screen.FriendProfile.route,
        Screen.WishListDetail.route
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in mainRoutes) {
                BottomNavigationMenu(
                    selectedRoute = when (currentRoute) {
                        Screen.FriendProfile.route -> Screen.Friends.route
                        Screen.WishListDetail.route -> Screen.MyLists.route
                        else -> currentRoute
                    },
                    onDestinationSelected = { route ->
                        if (route != currentRoute) {
                            val destinationRoute = if (route == Screen.Home.route) {
                                val homeEntry = navController.getBackStackEntry(Screen.Home.route)
                                val userName = homeEntry.arguments?.getString("userName") ?: "Usuario"
                                Screen.Home.createRoute(userName)
                            } else {
                                route
                            }

                            navController.navigate(destinationRoute) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
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
            ) { homeBackStackEntry ->
                val userName = homeBackStackEntry.arguments?.getString("userName") ?: "Usuario"
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

            composable(Screen.MyLists.route) {
                val wishListsViewModel: WishListsViewModel = koinViewModel()
                WishListsScreen(
                    viewModel = wishListsViewModel,
                    onWishListClick = { listId ->
                        navController.navigate(Screen.WishListDetail.createRoute(listId))
                    }
                )
            }

            composable(
                route = Screen.WishListDetail.route,
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) { backStackEntry ->
                val listId = requireNotNull(backStackEntry.arguments?.getString("listId"))
                val wishListDetailViewModel: WishListDetailViewModel = koinViewModel()
                WishListDetailScreen(
                    listId = listId,
                    viewModel = wishListDetailViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Friends.route) {
                val friendsViewModel: FriendsViewModel = koinViewModel()
                FriendsScreen(
                    viewModel = friendsViewModel,
                    onFriendClick = { friendUid ->
                        navController.navigate(Screen.FriendProfile.createRoute(friendUid))
                    }
                )
            }

            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = koinViewModel()
                ProfileScreen(viewModel = profileViewModel)
            }

            composable(
                route = Screen.FriendProfile.route,
                arguments = listOf(navArgument("friendUid") { type = NavType.StringType })
            ) { friendProfileEntry ->
                val friendUid = requireNotNull(
                    friendProfileEntry.arguments?.getString("friendUid")
                )
                val friendProfileViewModel: FriendProfileViewModel = koinViewModel()
                FriendProfileScreen(
                    friendUid = friendUid,
                    viewModel = friendProfileViewModel
                )
            }
        }
    }
}
