package es.ulpgc.wishlistapp.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.ulpgc.wishlistapp.data.repository.AuthRepositoryImpl
import es.ulpgc.wishlistapp.domain.repository.AuthRepository
import es.ulpgc.wishlistapp.ui.register.RegisterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    viewModel { RegisterViewModel(get()) }
}
