package com.example.smartcalender

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME
import android.provider.CalendarContract.EXTRA_EVENT_END_TIME
import android.provider.CalendarContract.Events
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

object CalenderHelper {

    val formatter= SimpleDateFormat("yyyyMMdd_HH:mm")

    fun addEvent(
        context: Context,
        title: String,
        desc: String,
        location: String,
        begin: Long,
        end: Long
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = Events.CONTENT_URI
            putExtra(Events.TITLE, title)
            putExtra(Events.DESCRIPTION, desc)
            putExtra(Events.EVENT_LOCATION, location)
            putExtra(EXTRA_EVENT_BEGIN_TIME, begin)
            putExtra(EXTRA_EVENT_END_TIME, end)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun addEvent(context: Context, calenderEvent: CalenderEvent) {
        val time = formatter.parse(calenderEvent.date).time
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = Events.CONTENT_URI
            putExtra(Events.TITLE, calenderEvent.title)
            putExtra(Events.DESCRIPTION, calenderEvent.note)
            putExtra(Events.EVENT_LOCATION, calenderEvent.location)
            putExtra(EXTRA_EVENT_BEGIN_TIME, time)
            putExtra(EXTRA_EVENT_END_TIME, time + (3600 * 1000))
        }
        context.startActivity(intent)
    }
}