package ir.filternet.cfscanner

import android.app.Application
import android.content.Context
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import dagger.hilt.android.HiltAndroidApp
import go.Seq
import ir.filternet.cfscanner.db.CFSDatabase
import ir.filternet.cfscanner.db.dao.CIDRDao
import ir.filternet.cfscanner.db.dao.ConfigDao
import ir.filternet.cfscanner.db.dao.ConnectionDao
import ir.filternet.cfscanner.db.dao.ISPDao
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.utils.AppConfig
import ir.filternet.cfscanner.utils.userAssetPath
import libv2ray.Libv2ray
import okhttp3.OkHttpClient
import timber.log.Timber

@HiltAndroidApp
class CFScannerApplication : Application() {

    companion object {
        init {
            System.loadLibrary("nativelib")
        }
    }

    external fun DisableFDSAN()

    override fun onCreate() {
        super.onCreate()

        DisableFDSAN()

        // init V2ray
        Seq.setContext(this)
        Libv2ray.initV2Env(userAssetPath(this))


        // init Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val config = YandexMetricaConfig.newConfigBuilder(AppConfig.YANDEX_METRICA_KEY).build()
        YandexMetrica.activate(this, config)
        YandexMetrica.enableActivityAutoTracking(this)
    }
}