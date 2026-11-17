package es.ulpgc.wishlistapp

import android.app.Application
import es.ulpgc.wishlistapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class WishlistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@WishlistApp)
            modules(appModule)
        }
    }
}
