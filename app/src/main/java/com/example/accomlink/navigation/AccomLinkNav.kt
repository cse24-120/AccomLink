package com.example.accomlink.navigation

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookOnline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.accomlink.chat.ChatScreen
import com.example.accomlink.chat.ChatViewModel
import com.example.accomlink.auth.AuthViewModel
import com.example.accomlink.auth.ForgotPasswordScreen
import com.example.accomlink.auth.LoginScreen
import com.example.accomlink.auth.RegisterScreen
import com.example.accomlink.auth.SplashScreen
import com.example.accomlink.landlord.LandlordDashboardScreen
import com.example.accomlink.landlord.ListingEditorScreen
import com.example.accomlink.listings.ListingsViewModel
import com.example.accomlink.models.UserRole
import com.example.accomlink.payment.PaymentScreen
import com.example.accomlink.payment.PaymentViewModel
import com.example.accomlink.receipt.ReceiptScreen
import com.example.accomlink.student.FavoritesScreen
import com.example.accomlink.student.ListingDetailsScreen
import com.example.accomlink.student.ReservationsScreen
import com.example.accomlink.student.SearchScreen
import com.example.accomlink.student.StudentHomeScreen
import com.example.accomlink.ui.ProfileScreen

@Composable
fun AccomLinkApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authViewModel = remember { AuthViewModel(context.applicationContext) }
    val listingsViewModel = remember { ListingsViewModel() }
    val chatViewModel = remember { ChatViewModel() }
    val paymentViewModel = remember { PaymentViewModel() }
    val profile by authViewModel.profile.collectAsState()
    val uid by authViewModel.authUserId.collectAsState()

    NavHost(navController = navController, startDestination = AccomRoutes.Splash) {
        composable(AccomRoutes.Splash) {
            SplashScreen {
                navController.navigate(if (uid == null) AccomRoutes.Login else homeFor(profile?.role)) {
                    popUpTo(AccomRoutes.Splash) { inclusive = true }
                }
            }
        }
        composable(AccomRoutes.Login) {
            LoginScreen(authViewModel, onRegister = { navController.navigate(AccomRoutes.Register) }, onForgot = { navController.navigate(AccomRoutes.Forgot) })
            if (uid != null) NavigateOnce(navController, homeFor(profile?.role))
        }
        composable(AccomRoutes.Register) {
            RegisterScreen(authViewModel) { navController.popBackStack() }
            if (uid != null) NavigateOnce(navController, homeFor(profile?.role))
        }
        composable(AccomRoutes.Forgot) {
            ForgotPasswordScreen(authViewModel) { navController.popBackStack() }
        }
        composable(AccomRoutes.StudentHome) {
            AppScaffold(navController, profile?.role) {
                StudentHomeScreen(
                    viewModel = listingsViewModel,
                    onOpen = { navController.navigate("${AccomRoutes.Details}/$it") },
                    onSearch = { navController.navigate(AccomRoutes.Search) },
                    onSaved = { navController.navigate(AccomRoutes.Favorites) },
                    onProfile = { navController.navigate(AccomRoutes.Profile) }
                )
            }
        }
        composable(AccomRoutes.Search) {
            AppScaffold(navController, profile?.role) {
                SearchScreen(listingsViewModel) { navController.navigate("${AccomRoutes.Details}/$it") }
            }
        }
        composable(AccomRoutes.Favorites) {
            AppScaffold(navController, profile?.role) {
                FavoritesScreen(listingsViewModel) { navController.navigate("${AccomRoutes.Details}/$it") }
            }
        }
        composable(AccomRoutes.Chat) {
            val previousRoute = navController.previousBackStackEntry?.destination?.route
            val closeRoomOnBack = previousRoute != "${AccomRoutes.Details}/{listingId}"
            AppScaffold(navController, profile?.role) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(homeFor(profile?.role)) {
                                launchSingleTop = true
                            }
                        }
                    },
                    closeRoomOnBack = closeRoomOnBack
                )
            }
        }
        composable("${AccomRoutes.Details}/{listingId}", arguments = listOf(navArgument("listingId") { type = NavType.StringType })) { entry ->
            val listingId = entry.arguments?.getString("listingId").orEmpty()
            val listing by listingsViewModel.listing(listingId).collectAsState(initial = null)
            val favorites by listingsViewModel.favoriteIds.collectAsState()
            listing?.let {
                AppScaffold(navController, profile?.role) {
                    ListingDetailsScreen(
                        listing = it,
                        isFavorite = it.id in favorites,
                        onFavorite = { listingsViewModel.toggleFavorite(it.id) },
                        onContact = {
                            chatViewModel.openListingRoom(it)
                            navController.navigate(AccomRoutes.Chat)
                        },
                        onReserve = { navController.navigate("${AccomRoutes.Payment}/${it.id}") }
                    )
                }
            }
        }
        composable("${AccomRoutes.Payment}/{listingId}", arguments = listOf(navArgument("listingId") { type = NavType.StringType })) { entry ->
            val listingId = entry.arguments?.getString("listingId").orEmpty()
            val listing by listingsViewModel.listing(listingId).collectAsState(initial = null)
            listing?.let {
                PaymentScreen(it, paymentViewModel) { reservationId ->
                    navController.navigate("${AccomRoutes.Receipt}/${reservationId}") { popUpTo("${AccomRoutes.Details}/$listingId") }
                }
            }
        }
        composable("${AccomRoutes.Receipt}/{reservationId}", arguments = listOf(navArgument("reservationId") { type = NavType.StringType })) { entry ->
            val reservationId = entry.arguments?.getString("reservationId").orEmpty()
            LaunchedEffect(reservationId) {
                paymentViewModel.loadReservation(reservationId)
            }
            val reservation by paymentViewModel.reservation.collectAsState()
            val listing by listingsViewModel.listing(reservation?.listingId.orEmpty()).collectAsState(initial = null)
            ReceiptScreen(
                reservation = reservation,
                listing = listing,
                onReservations = { navController.navigate(AccomRoutes.Reservations) },
                onHome = {
                    val homeRoute = homeFor(profile?.role)
                    navController.navigate(homeRoute) {
                        popUpTo(homeRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AccomRoutes.Reservations) {
            val reservations by listingsViewModel.myReservations.collectAsState()
            val allListings by listingsViewModel.allListings.collectAsState()
            AppScaffold(navController, profile?.role) {
                ReservationsScreen(reservations, allListings) { navController.navigate("${AccomRoutes.Details}/$it") }
            }
        }
        composable(AccomRoutes.Landlord) {
            AppScaffold(navController, profile?.role) {
                LandlordDashboardScreen(listingsViewModel, { navController.navigate(AccomRoutes.AddListing) }, { navController.navigate("${AccomRoutes.EditListing}/$it") })
            }
        }
        composable(AccomRoutes.AddListing) {
            AppScaffold(navController, profile?.role) {
                ListingEditorScreen(listingsViewModel, null) { navController.popBackStack() }
            }
        }
        composable("${AccomRoutes.EditListing}/{listingId}", arguments = listOf(navArgument("listingId") { type = NavType.StringType })) { entry ->
            val listingId = entry.arguments?.getString("listingId").orEmpty()
            val listing by listingsViewModel.listing(listingId).collectAsState(initial = null)
            AppScaffold(navController, profile?.role) {
                ListingEditorScreen(listingsViewModel, listing) { navController.popBackStack() }
            }
        }
        composable(AccomRoutes.Profile) {
            AppScaffold(navController, profile?.role) {
                ProfileScreen(profile, listingsViewModel, onReservations = { navController.navigate(AccomRoutes.Reservations) }) {
                    authViewModel.signOut()
                    navController.navigate(AccomRoutes.Login) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigateOnce(navController: NavHostController, route: String) {
    androidx.compose.runtime.LaunchedEffect(route) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

@Composable
private fun AppScaffold(navController: NavHostController, role: UserRole?, content: @Composable () -> Unit) {
    val activity = LocalContext.current as? Activity
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route.orEmpty()
    val homeRoute = homeFor(role)
    val items = if (role == UserRole.Landlord) {
        listOf(
            NavItem(AccomRoutes.Landlord, "Listings", Icons.Outlined.Home),
            NavItem(AccomRoutes.AddListing, "Add", Icons.Outlined.Add),
            NavItem(AccomRoutes.Chat, "Chat", Icons.AutoMirrored.Outlined.Chat),
            NavItem(AccomRoutes.Profile, "Profile", Icons.Outlined.Person)
        )
    } else {
        listOf(
            NavItem(AccomRoutes.StudentHome, "Home", Icons.Outlined.Home),
            NavItem(AccomRoutes.Search, "Search", Icons.Outlined.Search),
            NavItem(AccomRoutes.Reservations, "Bookings", Icons.Outlined.BookOnline),
            NavItem(AccomRoutes.Chat, "Chat", Icons.AutoMirrored.Outlined.Chat)
        )
    }

    BackHandler(enabled = current in items.map { it.route }) {
        if (current == homeRoute) {
            activity?.finish()
        } else {
            navController.navigate(homeRoute) {
                popUpTo(homeRoute) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(homeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding -> androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { content() } }
}

private fun homeFor(role: UserRole?): String =
    if (role == UserRole.Landlord) AccomRoutes.Landlord else AccomRoutes.StudentHome

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
