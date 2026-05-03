package com.monochrome.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the "Exit" action broadcast from the persistent notification button.
 * Stops MusicService and kills the app process.
 */
class AppExitReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXIT = "com.monochrome.app.EXIT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_EXIT) {
            context.stopService(Intent(context, MusicService::class.java))
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
