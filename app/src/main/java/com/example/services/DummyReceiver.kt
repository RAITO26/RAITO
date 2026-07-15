package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DummyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Do nothing, used strictly as a dummy component to trigger package-changed cascades
    }
}
