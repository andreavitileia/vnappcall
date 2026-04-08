package com.vnsas.vnappcall

import android.app.Application
import com.vnsas.vnappcall.data.AppDatabase

class VNApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
