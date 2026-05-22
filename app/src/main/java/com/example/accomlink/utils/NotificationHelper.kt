package com.example.accomlink.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.accomlink.R
import com.example.accomlink.models.Listing
import com.example.accomlink.models.ListingFilter

/** Creates local listing-match alert notifications. */
object NotificationHelper {
    const val ChannelId = "accomlink_alerts"

    /** Ensures the notification channel exists on Android O and newer. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(ChannelId, "AccomLink alerts", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
    }

    /** Fires a notification when notification permission is available. */
    fun notifyMatch(context: Context, listing: Listing) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New match found!")
            .setContentText("${listing.title} in ${listing.location}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(listing.id.hashCode(), notification)
    }

    /** Checks whether a listing satisfies a saved filter. */
    fun matches(filter: ListingFilter, listing: Listing): Boolean =
        listing.price in filter.minPrice..filter.maxPrice &&
            (filter.location.isBlank() || listing.location.contains(filter.location, true)) &&
            (filter.roomType.isBlank() || listing.roomType.equals(filter.roomType, true)) &&
            (filter.amenities.isEmpty() || filter.amenities.all { selected -> listing.amenities.any { it.equals(selected, true) } })
}
