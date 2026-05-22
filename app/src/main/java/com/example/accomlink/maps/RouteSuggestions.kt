package com.example.accomlink.maps

import com.example.accomlink.models.Listing
import com.example.accomlink.utils.DistanceUtils

object RouteSuggestions {
    private const val RouteThresholdKm = 2.5

    fun distanceToBac(listing: Listing): String {
        val distance = DistanceUtils.distanceKm(
            listing.latitude,
            listing.longitude,
            Listing.BAC_LATITUDE,
            Listing.BAC_LONGITUDE
        )
        return DistanceUtils.formatKm(distance)
    }

    fun nearestRoute(listing: Listing): RouteMatch? =
        routes.asSequence()
            .flatMap { route ->
                route.waypoints.asSequence().map { waypoint ->
                    val distance = DistanceUtils.distanceKm(
                        listing.latitude,
                        listing.longitude,
                        waypoint.latitude,
                        waypoint.longitude
                    )
                    RouteMatch(route.name, waypoint.name, distance)
                }
            }
            .minByOrNull { it.distanceKm }
            ?.takeIf { it.distanceKm <= RouteThresholdKm }

    fun suggestionsFor(listing: Listing): List<String> {
        val match = nearestRoute(listing)
        return if (match == null) {
            listOf("No BAC route within 2.5 km of this house.")
        } else {
            listOf("${match.routeName} - nearest stop: ${match.waypointName} (${DistanceUtils.formatKm(match.distanceKm)} away)")
        }
    }

    private val routes = listOf(
        RouteDefinition("Tlokweng Route 1", listOf(RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007), RouteWaypoint("Station Rd", -24.663513, 25.906507), RouteWaypoint("Khama Cres", -24.658002, 25.912735), RouteWaypoint("Kaunda RD", -24.666909, 25.917479), RouteWaypoint("Tlokweng Road main corridor", -24.665, 25.935), RouteWaypoint("Riverwalk Mall / Riverwalk corridor", -24.6768, 25.9358), RouteWaypoint("Engen Tlokweng area", -24.675259, 25.965052), RouteWaypoint("Residential Connector A", -24.673933, 25.963981), RouteWaypoint("Residential Connector B", -24.672371, 25.962252), RouteWaypoint("Residential Connector C", -24.67122, 25.961355), RouteWaypoint("Matlala Road", -24.6662, 25.946), RouteWaypoint("Seitshiro Road", -24.6674, 25.951), RouteWaypoint("Kentse Road", -24.668312, 25.97179), RouteWaypoint("Mmaseroka Road", -24.669466, 25.981892), RouteWaypoint("Tlokweng Road reconnect", -24.6648, 25.946), RouteWaypoint("Riverwalk Mall / Riverwalk corridor", -24.6768, 25.9358), RouteWaypoint("Kaunda RD", -24.666909, 25.917479), RouteWaypoint("Khama Cres", -24.658002, 25.912735), RouteWaypoint("Station Rd", -24.663513, 25.906507), RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007))),
        RouteDefinition("Tlokweng Route 2", listOf(RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007), RouteWaypoint("Station Rd", -24.663513, 25.906507), RouteWaypoint("Khama Cres", -24.658002, 25.912735), RouteWaypoint("Kaunda RD", -24.666909, 25.917479), RouteWaypoint("Tlokweng Road main corridor", -24.665, 25.935), RouteWaypoint("Riverwalk Mall / Riverwalk corridor", -24.6768, 25.9358), RouteWaypoint("Builders Home Tlokweng vicinity", -24.676063, 25.970381), RouteWaypoint("Boitekanelo College", -24.6675, 25.9521), RouteWaypoint("Boitekanelo residential connector", -24.667, 25.949), RouteWaypoint("Tlokweng Road reconnect", -24.666, 25.946), RouteWaypoint("Riverwalk Mall / Riverwalk corridor", -24.6768, 25.9358), RouteWaypoint("Kaunda RD", -24.666909, 25.917479), RouteWaypoint("Khama Cres", -24.658002, 25.912735), RouteWaypoint("Station Rd", -24.663513, 25.906507), RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007))),
        RouteDefinition("Tlokweng Route 3", listOf(RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007), RouteWaypoint("Station Rd", -24.663513, 25.906507), RouteWaypoint("Khama Cres", -24.658002, 25.912735), RouteWaypoint("Kaunda RD", -24.666909, 25.917479), RouteWaypoint("Tlokweng Road main corridor", -24.665, 25.935), RouteWaypoint("Riverwalk Mall / Riverwalk corridor", -24.6768, 25.9358), RouteWaypoint("Send foods Head Office", -24.678705, 25.94642), RouteWaypoint("ratumelo RD", -24.676911, 25.956229), RouteWaypoint("Tlokweng main kgotla", -24.679181, 25.953203), RouteWaypoint("batlokwa national school", -24.677038, 25.956637), RouteWaypoint("UCCSA Tlokweng", -24.6707, 25.9477), RouteWaypoint("Ratumelo Road / Kentse Road junction", -24.6671, 25.954), RouteWaypoint("Choppies Tlokweng-3", -24.675777, 25.966489), RouteWaypoint("Saverite Tlokweng", -24.654969, 25.97039), RouteWaypoint("Tlokweng College Of Education", -24.651906, 25.975693), RouteWaypoint("Dithuto English Medium School", -24.645418, 25.978914), RouteWaypoint("Royal Aria Convention Centre", -24.63898, 25.979342), RouteWaypoint("Tlokweng Road reconnect", -24.6648, 25.946), RouteWaypoint("Riverwalk Mall / Riverwalk corridor", -24.6768, 25.9358), RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007))),
        RouteDefinition("Tlokweng Route 6 (Express Outbound)", listOf(RouteWaypoint("Snack Munchies BW", -24.667355, 26.001522), RouteWaypoint("Connector 1", -24.66669, 26.001271), RouteWaypoint("Connector 2", -24.665474, 26.000354), RouteWaypoint("Connector 3", -24.664145, 25.999211), RouteWaypoint("Connector 4", -24.658258, 25.994259), RouteWaypoint("Madikwe Road", -24.661142, 25.987233), RouteWaypoint("Tlokweng Cjss", -24.661216, 25.985989), RouteWaypoint("Serenity Rehabilitation Centre", -24.662675, 25.984051), RouteWaypoint("Border Gate Mall", -24.664723, 25.981898), RouteWaypoint("Tlokweng Police Station", -24.668162, 25.981215), RouteWaypoint("Tlokweng Medical Center", -24.66798, 25.976362), RouteWaypoint("NIC29 DESIGNS", -24.668478, 25.973519), RouteWaypoint("Sefalana Quick", -24.667837, 25.969763), RouteWaypoint("Spiritual Healing Church Tlokweng", -24.666884, 25.965761), RouteWaypoint("ratumelo RD", -24.676911, 25.956229), RouteWaypoint("UCCSA Tlokweng", -24.6707, 25.9477), RouteWaypoint("batlokwa national school", -24.677038, 25.956637), RouteWaypoint("Tlokweng main kgotla", -24.679181, 25.953203), RouteWaypoint("Tlokweng Road citybound corridor", -24.665, 25.935), RouteWaypoint("Riverwalk", -24.6768, 25.9358), RouteWaypoint("Tlokweng / Mobuto / Samora Junction", -24.6802, 25.915), RouteWaypoint("Samora Machel", -24.679584, 25.917039), RouteWaypoint("Old Lobatse RD", -24.680584, 25.906069), RouteWaypoint("Game City", -24.6869, 25.88))),
        RouteDefinition("Tlokweng Route 6 (Express Return)", listOf(RouteWaypoint("Game City", -24.6869, 25.88), RouteWaypoint("Old Lobatse RD", -24.680584, 25.906069), RouteWaypoint("Samora Machel", -24.679584, 25.917039), RouteWaypoint("Tlokweng / Mobuto / Samora Junction", -24.6802, 25.915), RouteWaypoint("Riverwalk", -24.6768, 25.9358), RouteWaypoint("Tlokweng Road eastbound corridor", -24.665, 25.935), RouteWaypoint("Matlala JSS", -24.671847, 25.969931), RouteWaypoint("Border Gate Mall", -24.664723, 25.981898), RouteWaypoint("Madikwe Road", -24.661142, 25.987233), RouteWaypoint("Connector 4", -24.658258, 25.994259), RouteWaypoint("Connector 3", -24.664145, 25.999211), RouteWaypoint("Connector 2", -24.665474, 26.000354), RouteWaypoint("Connector 1", -24.66669, 26.001271), RouteWaypoint("Snack Munchies BW", -24.667355, 26.001522))),
        RouteDefinition("Broadhurst Route 5", listOf(RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007), RouteWaypoint("Station Rd", -24.663513, 25.906507), RouteWaypoint("Old Lobatse Road", -24.680584, 25.906069), RouteWaypoint("Samora Machel Drive", -24.679584, 25.917039), RouteWaypoint("Fairgrounds/Shell Ext 7", -24.6802, 25.915), RouteWaypoint("Turn near BAC Main Campus", -24.679633, 25.925492), RouteWaypoint("Turn near Botswana Insurance Company", -24.677505, 25.925628), RouteWaypoint("Loops back to Fairgrounds/Shell", -24.6802, 25.915), RouteWaypoint("Samora Machel Dr", -24.679584, 25.917039), RouteWaypoint("Mobuto Drive", -24.671, 25.904), RouteWaypoint("Chuma Drive / AVANI area", -24.673, 25.905), RouteWaypoint("Metsemasewa Road", -24.638502, 25.939248), RouteWaypoint("Bellwethers Int'l", -24.639959, 25.939237), RouteWaypoint("Broadhurst Drive", -24.66, 25.902), RouteWaypoint("Segoditshane Way", -24.626121, 25.935571), RouteWaypoint("Gaborone Railway Station", -24.663424, 25.904007)))
    )
}

data class RouteMatch(
    val routeName: String,
    val waypointName: String,
    val distanceKm: Double
)

private data class RouteDefinition(
    val name: String,
    val waypoints: List<RouteWaypoint>
)

private data class RouteWaypoint(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
