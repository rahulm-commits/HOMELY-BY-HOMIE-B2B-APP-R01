package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RoundaboutLeft
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.TurnSlightLeft
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryRoute
import com.example.data.remote.NavigationRouteResult
import com.example.data.remote.TurnStep
import com.example.ui.components.NavigationMapPin
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.StopPinState
import com.example.ui.components.formatDistanceShort
import com.example.ui.components.formatInr
import com.example.ui.theme.semanticColors

@Composable
fun TransportTopBar(
  partnerTitle: String,
  hubSubtitle: String,
  pendingCount: Int,
  showSearch: Boolean,
  searchQuery: String,
  onToggleSearch: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onNotificationsClick: () -> Unit,
  onProfileSyncClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "HOMELY BY HOMIE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.5.sp,
              letterSpacing = 0.8.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Text(
              text = partnerTitle,
              style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Text(
              text = "• $hubSubtitle",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.secondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(44.dp)
              .clip(CircleShape)
              .clickable(onClick = onToggleSearch)
              .testTag("transport_search_button"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "Search route stops",
              tint = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp),
            )
          }

          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(44.dp)
              .clip(CircleShape)
              .clickable(onClick = onNotificationsClick)
              .testTag("transport_notifications_button"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Notifications,
              contentDescription = "Pending delivery stops",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp),
            )
            if (pendingCount > 0) {
              Surface(
                color = MaterialTheme.colorScheme.error,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .offset(x = (-4).dp, y = 4.dp)
                  .size(18.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = pendingCount.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                  )
                }
              }
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            shadowElevation = 2.dp,
            modifier = Modifier
              .padding(start = 2.dp)
              .minimumInteractiveComponentSize()
              .size(38.dp)
              .clip(CircleShape)
              .clickable(onClick = onProfileSyncClick)
              .testTag("transport_profile_sync_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Sync & Partner Account",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }

      AnimatedVisibility(visible = showSearch) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Filter stops by shop name, code, or order...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear search",
                modifier = Modifier.clickable { onSearchQueryChange("") },
              )
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("transport_search_input"),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
  }
}

@Composable
fun ActiveRouteTriggerCard(
  route: DeliveryRoute?,
  stopCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("transport_route_selector"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.AltRoute,
            contentDescription = "Active Route",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp),
          ) {
            Text(
              text = "ACTIVE ROUTE",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = route?.id ?: "ALL ROUTES",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = route?.name ?: "Select Delivery Route",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = "$stopCount stops",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        Icon(
          imageVector = Icons.Rounded.ExpandMore,
          contentDescription = "Switch delivery route",
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
fun VehicleProgressCard(
  planState: TransportRoutePlanState,
  onEditVehicleClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val route = planState.selectedRoute
  val vehicleSummary = parseVehicleSummary(route?.vehicle.orEmpty())
  val nextStopLabel = planState.activeTargetStop?.let {
    "Stop ${it.formattedStopNum} (${it.shortShopName})"
  } ?: if (planState.totalStops > 0) "All Stops Done" else "No Stops"

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(11.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(9.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.secondary,
            shape = CircleShape,
            modifier = Modifier
              .padding(top = 2.dp)
              .size(28.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = vehicleSummary.modelName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
              )
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = vehicleSummary.vehicleTypeTag,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
            Text(
              text = vehicleSummary.plateNumber.ifBlank { "Plate number not set" },
              style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.secondary,
            )
            if (vehicleSummary.extraDetails.isNotBlank()) {
              Text(
                text = vehicleSummary.extraDetails,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "Driver: ${route?.driverName ?: "Assigned"} | ${route?.dispatchWindow ?: "Morning Run"}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (onEditVehicleClick != null) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                shape = CircleShape,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .size(32.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onEditVehicleClick)
                  .testTag("transport_edit_vehicle_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit vehicle and driver details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = route?.id?.substringBefore("-") ?: "ROUTE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = route?.stagingBay ?: "Bay #03",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(9.dp),
          verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Storefront,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text = "${planState.completedCount} of ${planState.totalStops} Stops Completed",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Text(
                text = "${planState.completionPercent}% • ${planState.estimatedFinishText}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            LinearProgressIndicator(
              progress = { (planState.completionPercent / 100f).coerceIn(0f, 1f) },
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              strokeCap = StrokeCap.Round,
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(9.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              shadowElevation = 1.dp,
              modifier = Modifier.weight(1f),
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = "Cash to Collect",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = formatInr(planState.cashToCollect),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
            }

            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(9.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              shadowElevation = 1.dp,
              modifier = Modifier.weight(1f),
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = "Next Destination",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = nextStopLabel,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun StopFilterChipsRow(
  selectedFilter: TransportStopFilter,
  totalCount: Int,
  pendingCount: Int,
  completedCount: Int,
  onSelectFilter: (TransportStopFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    TransportStopFilter.entries.forEach { filter ->
      val selected = selectedFilter == filter
      val count = when (filter) {
        TransportStopFilter.ALL -> totalCount
        TransportStopFilter.PENDING -> pendingCount
        TransportStopFilter.COMPLETED -> completedCount
      }
      Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(999.dp),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        shadowElevation = if (selected) 2.dp else 0.5.dp,
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .clickable { onSelectFilter(filter) }
          .testTag("transport_filter_${filter.name.lowercase()}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = filter.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = count.toString(),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              fontWeight = FontWeight.Bold,
              color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun TransportMapCard(
  planState: TransportRoutePlanState,
  routeGeometry: NavigationRouteResult?,
  routes: List<DeliveryRoute>,
  selectedPinShopId: String?,
  userLatitude: Double?,
  userLongitude: Double?,
  followUserLocation: Boolean,
  recenterTrigger: Int,
  fitBoundsTrigger: Int,
  hasLocationPermission: Boolean,
  onRecenterUserClick: () -> Unit,
  onToggleFullscreenOrFitClick: () -> Unit,
  onPinSelected: (String) -> Unit,
  onDirectionsBannerClick: () -> Unit,
  onLaunchExternalMapsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeStop = planState.activeTargetStop
  val navPins = planState.stops.map { stop ->
    val distText = if (stop.isActiveTarget && routeGeometry != null && routeGeometry.activeLegDistanceMeters > 0) {
      formatDistanceShort(routeGeometry.activeLegDistanceMeters)
    } else if (stop.distanceFromUserMeters != null) {
      formatDistanceShort(stop.distanceFromUserMeters)
    } else {
      ""
    }
    NavigationMapPin(
      shop = stop.shop,
      stopNumber = stop.stopNumber,
      state = when {
        stop.isCompleted -> StopPinState.COMPLETED
        stop.isActiveTarget -> StopPinState.ACTIVE_TARGET
        else -> StopPinState.UPCOMING
      },
      distanceLabel = distText,
    )
  }

  val firstStep: TurnStep? = routeGeometry?.activeLegSteps?.firstOrNull()
  val legDistanceStr = when {
    routeGeometry != null && routeGeometry.activeLegDistanceMeters > 0 ->
      formatDistanceShort(routeGeometry.activeLegDistanceMeters)
    activeStop?.distanceFromUserMeters != null ->
      formatDistanceShort(activeStop.distanceFromUserMeters)
    else -> "Ready"
  }
  val legSummaryPill = if (activeStop != null && routeGeometry != null && routeGeometry.activeLegDistanceMeters > 0) {
    "Stop ${activeStop.formattedStopNum} • ${formatEtaSummary(routeGeometry.activeLegDistanceMeters, routeGeometry.activeLegDurationSeconds)}"
  } else if (activeStop != null) {
    "Next: Stop ${activeStop.formattedStopNum} • ${activeStop.shortShopName}"
  } else {
    "All ${planState.totalStops} Stops Completed"
  }

  val instructionTitle = when {
    activeStop == null -> "All route stops completed!"
    firstStep != null && firstStep.instruction.isNotBlank() -> firstStep.instruction
    else -> "Head toward ${activeStop.shop.name}"
  }
  val instructionSub = when {
    activeStop == null -> "Return to Dispatch Hub"
    firstStep != null && firstStep.distanceMeters > 0 ->
      "In ${formatDistanceShort(firstStep.distanceMeters)} • Stop ${activeStop.formattedStopNum}: ${activeStop.shortShopName}"
    !hasLocationPermission -> "Tap GPS button for live turn-by-turn directions"
    else -> "$legDistanceStr • ${activeStop.shop.address}"
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(260.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      RealOsmMapView(
        shops = planState.stops.map { it.shop },
        routes = routes,
        selectedRouteId = planState.selectedRoute?.id ?: "ALL",
        selectedShopId = selectedPinShopId ?: activeStop?.shop?.id,
        showPolylines = true,
        userLatitude = userLatitude,
        userLongitude = userLongitude,
        recenterUserTrigger = recenterTrigger,
        fitBoundsTrigger = fitBoundsTrigger,
        navigationPins = navPins,
        activeLegPolyline = routeGeometry?.activeLegGeometry.orEmpty(),
        upcomingRoutePolyline = routeGeometry?.upcomingGeometry.orEmpty(),
        followUserLocation = followUserLocation,
        showBuiltInZoomControls = false,
        fitBoundsIncludesUser = true,
        onPinSelected = onPinSelected,
        modifier = Modifier.fillMaxSize(),
      )

      // Subtle top & bottom gradient scrims for crisp readability
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .align(Alignment.TopCenter)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Black.copy(alpha = 0.26f), Color.Transparent)
            )
          )
      )

      // Top-left live corridor / priority leg pill
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(10.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (hasLocationPermission && userLatitude != null) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning),
          )
          Text(
            text = legSummaryPill,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Top-right Recenter GPS & Fullscreen Navigation controls
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          color = if (followUserLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
          shape = RoundedCornerShape(10.dp),
          shadowElevation = 3.dp,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onRecenterUserClick)
            .testTag("transport_recenter_gps_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.MyLocation,
              contentDescription = "Center on my GPS location",
              tint = if (followUserLocation) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(19.dp),
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
          shape = RoundedCornerShape(10.dp),
          shadowElevation = 3.dp,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggleFullscreenOrFitClick)
            .testTag("transport_fullscreen_map_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Fullscreen,
              contentDescription = "Expand fullscreen navigation map",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      // Bottom Turn-by-Turn Instruction Banner inside Map
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(11.dp),
        shadowElevation = 5.dp,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(10.dp)
          .clip(RoundedCornerShape(11.dp))
          .clickable(onClick = onDirectionsBannerClick)
          .testTag("transport_turn_banner"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(32.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = maneuverIconFor(firstStep),
                contentDescription = "Next turn",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(19.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = instructionTitle,
              style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = instructionSub,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onLaunchExternalMapsClick)
              .testTag("transport_external_maps_button"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
              contentDescription = "Open external Google Maps navigation",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun UpcomingStopCard(
  stop: TransportStopItem,
  isSelected: Boolean,
  onCardClick: () -> Unit,
  onCallShop: () -> Unit,
  onMarkDelivered: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    shadowElevation = if (stop.isActiveTarget || isSelected) 3.dp else 2.dp,
    border = if (stop.isActiveTarget) {
      BorderStroke(1.6.dp, MaterialTheme.colorScheme.primary)
    } else if (isSelected) {
      BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
    } else {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    },
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onCardClick)
      .testTag("transport_upcoming_stop_${stop.stopNumber}"),
  ) {
    Column(
      modifier = Modifier.padding(11.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(9.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = if (stop.isActiveTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier.size(30.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = stop.formattedStopNum,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (stop.isActiveTarget) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stop.shop.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = stop.shop.shopCode,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = stop.shop.address,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Surface(
          color = if (stop.isActiveTarget) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = if (stop.isActiveTarget) "Next Target" else "Queued",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (stop.isActiveTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val orderText = if (stop.primaryOrderCode != null) {
            "Order #${stop.primaryOrderCode} • ${stop.totalPacks} Packs"
          } else {
            val distStr = stop.distanceFromUserMeters?.let { " • ${formatDistanceShort(it)}" }.orEmpty()
            "Route Stop #${stop.stopNumber}$distStr"
          }
          Text(
            text = orderText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector = if (stop.isCod) Icons.Rounded.CurrencyRupee else Icons.Rounded.Verified,
              contentDescription = null,
              tint = if (stop.isCod) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = stop.paymentLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (stop.isCod) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
            )
          }
        }
      }

      if (stop.isActiveTarget || isSelected) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(9.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(9.dp))
              .clickable(onClick = onCallShop),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 7.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Call shop",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Call Shop",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier
              .weight(1.4f)
              .clip(RoundedCornerShape(9.dp))
              .clickable(onClick = onMarkDelivered)
              .testTag("transport_mark_delivered_${stop.stopNumber}"),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 7.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.TaskAlt,
                contentDescription = "Mark Delivered",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Mark Delivered",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun DeliveredStopCard(
  stop: TransportStopItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    shadowElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag("transport_delivered_stop_${stop.stopNumber}"),
  ) {
    Column(
      modifier = Modifier.padding(11.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.semanticColors.success,
            shape = CircleShape,
            modifier = Modifier.size(25.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Delivered",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(15.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stop.shop.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${stop.shop.shopCode} • Stop ${stop.formattedStopNum} Delivered",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Surface(
          color = MaterialTheme.semanticColors.successContainer,
          shape = RoundedCornerShape(6.dp),
        ) {
          Text(
            text = stop.paymentLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.semanticColors.success,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      if (stop.podNote.isNotBlank()) {
        Row(
          modifier = Modifier.padding(start = 33.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = stop.podNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun TransportActionDock(
  activeStop: TransportStopItem?,
  onCallDispatchClick: () -> Unit,
  onNavigatePrimaryClick: () -> Unit,
  onCompleteStopClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryLabel = if (activeStop != null) {
    "Navigate Stop ${activeStop.formattedStopNum} (${activeStop.shortShopName})"
  } else {
    "Route Complete"
  }

  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    shadowElevation = 8.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column {
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onCallDispatchClick)
            .testTag("transport_call_dispatch_btn"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.HeadsetMic,
              contentDescription = "Call Dispatch or Driver",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp),
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(12.dp),
          shadowElevation = 3.dp,
          modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = activeStop != null, onClick = onNavigatePrimaryClick)
            .testTag("transport_navigate_primary_btn"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Navigation,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
              text = primaryLabel,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(12.dp),
          shadowElevation = 1.dp,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = activeStop != null, onClick = onCompleteStopClick)
            .testTag("transport_complete_stop_btn"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (activeStop != null) Icons.Rounded.TaskAlt else Icons.Rounded.QrCodeScanner,
              contentDescription = "Complete current delivery stop",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }
  }
}

fun maneuverIconFor(step: TurnStep?): ImageVector {
  if (step == null) return Icons.Rounded.TurnSlightRight
  val type = step.maneuverType.lowercase()
  val mod = step.maneuverModifier.lowercase()
  return when {
    type == "arrive" -> Icons.Rounded.Flag
    type == "roundabout" || type == "rotary" -> Icons.Rounded.RoundaboutLeft
    mod == "uturn" -> Icons.Rounded.UTurnLeft
    mod == "left" || mod == "sharp left" -> Icons.Rounded.TurnLeft
    mod == "slight left" -> Icons.Rounded.TurnSlightLeft
    mod == "right" || mod == "sharp right" -> Icons.Rounded.TurnRight
    mod == "slight right" -> Icons.Rounded.TurnSlightRight
    type == "depart" -> Icons.Rounded.Navigation
    else -> Icons.Rounded.Straight
  }
}
