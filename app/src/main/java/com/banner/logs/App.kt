package com.banner.logs

import android.app.Application

class App : Application() {
    val logViewModel: LogViewModel by lazy { LogViewModel(this) }
}
