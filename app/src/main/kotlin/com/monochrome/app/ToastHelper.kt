package com.monochrome.app

import android.content.Context
import android.widget.Toast

object ToastHelper {
    fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
