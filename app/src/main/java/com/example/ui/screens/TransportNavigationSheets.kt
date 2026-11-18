package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryRoute
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.data.remote.NavigationRouteResult
import com.example.ui.components.NavigationMapPin
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.StopPinState
import com.example.ui.components.formatDistanceShort
import com.example.ui.components.formatInr
import com.example.ui.theme.semanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectRouteBottomSheet(
  routes: List<DeliveryRoute>,
  selectedRouteId: String,
  allShops: List<Shop>,
  onSelectRoute: (DeliveryRoute) -> Unit,
  onManageRoutePlanning: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(38.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.AltRoute,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Column {
            Text(
              text = "Select Delivery Route",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Sequential stops follow Route Planning order",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close route picker",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

      routes.forEach { route ->
        val isSelected = route.id == selectedRouteId
        val stopCount = allShops.count { it.status == ShopStatus.ACTIVE && it.routeId == route.id }
        Surface(
          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelectRoute(route) }
            .testTag("transport_route_option_${route.id}"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.LocalShipping,
                  contentDescription = null,
                  tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp),
                )
              }
            }

            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = route.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                if (isSelected) {
                  Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                  ) {
                    Text(
                      text = "ACTIVE",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                  }
                }
              }
              Text(
                text = "${route.vehicle} • ${route.driverName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Surface(
              color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "$stopCount stops",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }

            if (isSelected) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Selected route",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }

      OutlinedButton(
        onClick = onManageRoutePlanning,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.AltRoute,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Configure Stop Order in Route Planning")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnByTurnDirectionsSheet(
  planState: TransportRoutePlanState,
  routeGeometry: NavigationRouteResult?,
  onLaunchExternalMaps: () -> Unit,
  onMarkActiveDelivered: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val activeStop = planState.activeTargetStop
  val steps = routeGeometry?.activeLegSteps.orEmpty()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (activeStop != null) {
              "Turn-by-Turn • Stop ${activeStop.formattedStopNum}"
            } else {
              "Route Navigation Summary"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (activeStop != null) {
            val legMeta = if (routeGeometry != null && routeGeometry.activeLegDistanceMeters > 0) {
              "${formatEtaSummary(routeGeometry.activeLegDistanceMeters, routeGeometry.activeLegDurationSeconds)} • Priority #1"
            } else {
              activeStop.shop.address
            }
            Text(
              text = "${activeStop.shop.name} ($legMeta)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Rounded.Close, contentDescription = "Close directions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (steps.isEmpty()) {
          item {
            Text(
              text = "Calculating road maneuvers. Use External Maps or follow the highlighted polyline.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          items(steps) { step ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.size(32.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = maneuverIconFor(step),
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  if (step.distanceMeters > 0) {
                    Text(
                      text = formatDistanceShort(step.distanceMeters),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        if (planState.upcomingStops.size > 1) {
          item {
            Text(
              text = "Following Sequential Stops",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 4.dp),
            )
          }
          items(planState.upcomingStops.drop(1)) { nextStop ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(24.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = nextStop.formattedStopNum,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              Text(
                text = "${nextStop.shop.name} • ${nextStop.shop.address}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onLaunchExternalMaps,
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Google Maps")
        }
        if (activeStop != null) {
          Button(
            onClick = onMarkActiveDelivered,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1.2f),
          ) {
            Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delivered & Next")
          }
        }
      }
    }
  }
}

@Composable
fun DeliveryPodDialog(
  stop: TransportStopItem,
  onConfirmDelivered: (podNote: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val paymentModes = remember(stop.isCod) {
    if (stop.isCod) {
      listOf("Cash Collected", "UPI Paid", "Cheque")
    } else {
      listOf("Prepaid Verified", "Cash Collected", "UPI Paid")
    }
  }
  var selectedMode by remember { mutableStateOf(paymentModes.first()) }
  var receiverName by remember { mutableStateOf(stop.shop.ownerName) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Complete Stop ${stop.formattedStopNum}",
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${stop.shop.name} (${stop.shop.shopCode})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        if (stop.totalAmountToCollect > 0.0) {
          Text(
            text = "Order Amount: ${formatInr(stop.totalAmountToCollect)} • ${stop.totalPacks} packs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          paymentModes.forEach { mode ->
            FilterChip(
              selected = selectedMode == mode,
              onClick = { selectedMode = mode },
              label = { Text(mode.substringBefore(" "), style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        OutlinedTextField(
          value = receiverName,
          onValueChange = { receiverName = it },
          label = { Text("POD Signed / Received By") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val signer = receiverName.trim().ifBlank { stop.shop.ownerName.ifBlank { "Shop Staff" } }
          onConfirmDelivered("POD Signed by $signer • $selectedMode")
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        Text("Mark Delivered")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
fun FullscreenNavigationOverlay(
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
  onRecenterClick: () -> Unit,
  onFitRouteClick: () -> Unit,
  onExitFullscreen: () -> Unit,
  onPinSelected: (String) -> Unit,
  onCallShopClick: (Shop) -> Unit,
  onOpenExternalMapsClick: () -> Unit,
  onMarkActiveDeliveredClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeStop = planState.activeTargetStop
  val firstStep = routeGeometry?.activeLegSteps?.firstOrNull()
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

  Box(modifier = modifier.fillMaxSize()) {
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
      showBuiltInZoomControls = true,
      fitBoundsIncludesUser = true,
      onPinSelected = onPinSelected,
      modifier = Modifier.fillMaxSize(),
    )

    // Top Turn-by-Turn Navigation Header
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer,
      shape = RoundedCornerShape(16.dp),
      shadowElevation = 6.dp,
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(12.dp),
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
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.size(42.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = maneuverIconFor(firstStep),
              contentDescription = "Next turn",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(24.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = firstStep?.instruction ?: activeStop?.let { "Navigate to ${it.shop.name}" } ?: "Route Complete",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          val subText = if (activeStop != null && routeGeometry != null && routeGeometry.activeLegDistanceMeters > 0) {
            "Stop ${activeStop.formattedStopNum} (${activeStop.shortShopName}) • ${formatEtaSummary(routeGeometry.activeLegDistanceMeters, routeGeometry.activeLegDurationSeconds)}"
          } else if (activeStop != null) {
            "Stop ${activeStop.formattedStopNum} • ${activeStop.shop.address}"
          } else {
            "All stops delivered"
          }
          Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f))
            .clickable(onClick = onExitFullscreen)
            .testTag("transport_exit_fullscreen_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.FullscreenExit,
            contentDescription = "Exit fullscreen navigation",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    // Map Recenter / Fit Bounds Controls on Top-Right below header
    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 82.dp, end = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onFitRouteClick),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.FitScreen, contentDescription = "Fit full route", tint = MaterialTheme.colorScheme.primary)
        }
      }

      Surface(
        color = if (followUserLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onRecenterClick),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.MyLocation,
            contentDescription = "Follow my location",
            tint = if (followUserLocation) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // Bottom Navigation Control Card
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
      shadowElevation = 10.dp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (activeStop != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = activeStop.formattedStopNum,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimary,
                    )
                  }
                }
                Text(
                  text = activeStop.shop.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "${activeStop.shop.address} • ${activeStop.paymentLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (!hasLocationPermission) {
              Surface(color = MaterialTheme.semanticColors.warningContainer, shape = RoundedCornerShape(999.dp)) {
                Text(
                  text = "GPS Off",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.semanticColors.warning,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { onCallShopClick(activeStop.shop) },
              modifier = Modifier.weight(0.8f),
            ) {
              Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Call")
            }
            OutlinedButton(
              onClick = onOpenExternalMapsClick,
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Maps")
            }
            Button(
              onClick = onMarkActiveDeliveredClick,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.weight(1.3f),
            ) {
              Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Delivered")
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.semanticColors.success)
              Text(
                text = "All ${planState.totalStops} stops completed on this route!",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Button(
              onClick = onExitFullscreen,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
              Text("Done")
            }
          }
        }
      }
    }
  }
}
