package com.kaajjo.libresudoku

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kaajjo.libresudoku.di.ACRA_SHARED_PREFS_NAME
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import java.time.LocalDate
import javax.inject.Inject

@HiltAndroidApp
class LibreSudokuApp : Application(), Configuration.Provider {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        if (!BuildConfig.DEBUG) {
            // Only user can send a crash report
            initAcra {
                buildConfigClass = BuildConfig::class.java
                reportFormat = StringFormat.KEY_VALUE_LIST

                dialog {
                    title = getString(R.string.app_name)
                    text = getString(R.string.dialog_crash_report_text)
                    negativeButtonText = getString(R.string.dialog_no)
                    positiveButtonText = getString(R.string.dialog_yes)
                    resTheme = android.R.style.Theme_DeviceDefault_Dialog
                }

                mailSender {
                    mailTo = "crashreport.libresudoku@gmail.com"
                    reportFileName = "Report_${LocalDate.now()}_v${BuildConfig.VERSION_NAME}.txt"
                    subject = "LibreSudoku crash report"
                    reportAsFile = true
                }
                sharedPreferencesName = ACRA_SHARED_PREFS_NAME
            }
        }
    }

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

}