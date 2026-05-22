package com.example.accomlink.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import com.example.accomlink.models.Listing
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.abs

@Composable
fun AccomMap(
    listing: Listing,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(13.5)
            }
        },
        update = { map ->
            val listingPoint = GeoPoint(
                listing.latitude.takeIf { it.isFinite() } ?: Listing.BAC_LATITUDE,
                listing.longitude.takeIf { it.isFinite() } ?: Listing.BAC_LONGITUDE
            )
            val bacPoint = GeoPoint(Listing.BAC_LATITUDE, Listing.BAC_LONGITUDE)
            map.overlays.clear()
            map.overlays.add(
                Marker(map).apply {
                    position = listingPoint
                    title = "House"
                    snippet = listing.title.ifBlank { listing.location }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
            map.overlays.add(
                Marker(map).apply {
                    position = bacPoint
                    title = "BAC"
                    snippet = "Botswana Accountancy College"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
            map.post {
                runCatching {
                    if (listingPoint.isNear(bacPoint)) {
                        map.controller.setZoom(15.0)
                        map.controller.setCenter(bacPoint)
                    } else {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPoints(listOf(listingPoint, bacPoint)), true, 80)
                    }
                }.onFailure {
                    map.controller.setZoom(13.5)
                    map.controller.setCenter(listingPoint)
                }
            }
            map.invalidate()
        }
    )
}

@Composable
fun LocationPickerMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    onLocationPicked: (Double, Double) -> Unit
) {
    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(latitude, longitude))
            }
        },
        update = { map ->
            val selectedPoint = GeoPoint(
                latitude.takeIf { it.isFinite() } ?: Listing.BAC_LATITUDE,
                longitude.takeIf { it.isFinite() } ?: Listing.BAC_LONGITUDE
            )
            map.overlays.clear()
            map.overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                        onLocationPicked(point.latitude, point.longitude)
                        return true
                    }

                    override fun longPressHelper(point: GeoPoint): Boolean {
                        onLocationPicked(point.latitude, point.longitude)
                        return true
                    }
                })
            )
            map.overlays.add(
                Marker(map).apply {
                    position = selectedPoint
                    title = "Selected location"
                    snippet = "Tap the map to move this pin"
                    infoWindow = null
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
            map.controller.setCenter(selectedPoint)
            map.invalidate()
        }
    )
}

private fun GeoPoint.isNear(other: GeoPoint): Boolean =
    abs(latitude - other.latitude) < 0.00001 && abs(longitude - other.longitude) < 0.00001
