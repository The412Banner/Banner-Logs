package com.banner.logs

import android.app.Application
import com.banner.logs.viewmodel.LogViewModel

class App : Application() {
    val logViewModel: LogViewModel by lazy { LogViewModel(this) }
}
