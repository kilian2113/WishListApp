package es.ulpgc.wishlistapp.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.ulpgc.wishlistapp.data.repository.AuthRepositoryImpl
import es.ulpgc.wishlistapp.data.repository.FriendRepositoryImpl
import es.ulpgc.wishlistapp.data.repository.ProfileRepositoryImpl
import es.ulpgc.wishlistapp.domain.repository.AuthRepository
import es.ulpgc.wishlistapp.domain.repository.FriendRepository
import es.ulpgc.wishlistapp.domain.repository.ProfileRepository
import es.ulpgc.wishlistapp.ui.friends.FriendsViewModel
import es.ulpgc.wishlistapp.ui.login.LoginViewModel
import es.ulpgc.wishlistapp.ui.profile.FriendProfileViewModel
import es.ulpgc.wishlistapp.ui.profile.ProfileViewModel
import es.ulpgc.wishlistapp.ui.register.RegisterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<FriendRepository> { FriendRepositoryImpl(get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { FriendsViewModel(get()) }
    viewModel { FriendProfileViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
}
