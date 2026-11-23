package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
        .padding(horizontal = 14.dp)
        .padding(bottom = 14.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
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
              .padding(10.dp),
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
  onConfirmArrived: () -> Unit = {},
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
        .padding(horizontal = 14.dp)
        .padding(bottom = 14.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
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
          item(key = "empty_steps_info", contentType = "directions_empty") {
            Text(
              text = "Calculating road maneuvers. Use External Maps or follow the highlighted polyline.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          itemsIndexed(
            items = steps,
            key = { idx, step -> "step_${idx}_${step.instruction}" },
            contentType = { _, _ -> "direction_step" },
          ) { _, step ->
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
          item(key = "following_stops_header", contentType = "following_header") {
            Text(
              text = "Following Sequential Stops",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 4.dp),
            )
          }
          items(
            items = planState.upcomingStops.drop(1),
            key = { "next_stop_${it.shop.id}" },
            contentType = { "following_stop" },
          ) { nextStop ->
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
          if (activeStop.isReadyToCompleteDelivery) {
            Button(
              onClick = onMarkActiveDelivered,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.semanticColors.success),
              modifier = Modifier
                .weight(1.3f)
                .testTag("directions_complete_delivery_button"),
            ) {
              Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Complete Delivery", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          } else {
            Button(
              onClick = onConfirmArrived,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier
                .weight(1.5f)
                .testTag("directions_arrived_bypass_button"),
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("I have reached the location", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPodDialog(
  stop: TransportStopItem,
  onConfirmDelivered: (podNote: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val paymentModes = remember(stop.isCod) {
    if (stop.isCod) {
      listOf("Cash Collected", "UPI Paid", "Cheque")
    } else {
      listOf("Prepaid Verified", "Cash Collected", "UPI Paid")
    }
  }
  var selectedMode by remember(stop.shop.id) { mutableStateOf(paymentModes.first()) }
  var receiverName by remember(stop.shop.id) { mutableStateOf(stop.shop.ownerName) }
  var isSubmitting by remember(stop.shop.id) { mutableStateOf(false) }

  val quickSigners = remember(stop.shop.ownerName) {
    listOfNotNull(
      stop.shop.ownerName.takeIf { it.isNotBlank() },
      "Shop Manager",
      "Counter Staff",
    ).distinct()
  }

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
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "Sequential Stop ${stop.formattedStopNum}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "Complete Stop ${stop.formattedStopNum}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${stop.shop.name} (${stop.shop.shopCode})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
            contentDescription = "Close POD confirmation",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (stop.totalAmountToCollect > 0.0) {
        Text(
          text = "Order Amount: ${formatInr(stop.totalAmountToCollect)} • ${stop.totalPacks} packs",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        quickSigners.forEach { preset ->
          FilterChip(
            selected = receiverName == preset,
            onClick = { receiverName = preset },
            label = {
              Text(
                text = preset,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        ) {
          Text("Cancel")
        }
        Button(
          onClick = {
            if (!isSubmitting) {
              isSubmitting = true
              val signer = receiverName.trim().ifBlank { stop.shop.ownerName.ifBlank { "Shop Staff" } }
              onConfirmDelivered("POD Signed by $signer • $selectedMode")
            }
          },
          enabled = !isSubmitting,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1.3f)
            .testTag("confirm_pod_delivered_button"),
        ) {
          Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Mark Delivered")
        }
      }
    }
  }
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
  focusShopTrigger: Int = 0,
  onFocusPin: (String) -> Unit = onPinSelected,
  onOpenStopExternalMapsClick: ((TransportStopItem?) -> Unit)? = null,
  onConfirmArrivedClick: () -> Unit = {},
  isRedirectingToNextStop: Boolean = false,
) {
  val orderedStops = planState.stops
  val activeStop = planState.activeTargetStop
  val focusedStop = selectedPinShopId
    ?.let { selId -> orderedStops.firstOrNull { it.shop.id == selId } }
    ?: activeStop
    ?: orderedStops.firstOrNull()
  val focusedIndex = if (focusedStop != null) {
    orderedStops.indexOfFirst { it.shop.id == focusedStop.shop.id }
  } else {
    -1
  }
  val isFocusedActive = focusedStop != null && activeStop != null && focusedStop.shop.id == activeStop.shop.id
  val activeSteps = routeGeometry?.activeLegSteps.orEmpty()
  val firstStep = activeSteps.firstOrNull()
  var showInlineSteps by remember(activeStop?.shop?.id) { mutableStateOf(false) }

  val navPins = orderedStops.map { stop ->
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
      shops = orderedStops.map { it.shop },
      routes = routes,
      selectedRouteId = planState.selectedRoute?.id ?: "ALL",
      selectedShopId = focusedStop?.shop?.id,
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
      focusShopTrigger = focusShopTrigger,
      onPinSelected = onFocusPin,
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
        .padding(12.dp)
        .testTag("nav_direction_header"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (focusedStop != null && isFocusedActive) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = when {
                focusedStop.isReadyToCompleteDelivery -> MaterialTheme.semanticColors.success
                else -> MaterialTheme.colorScheme.primary
              },
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = when {
                  focusedStop.isReadyToCompleteDelivery -> "ARRIVED • STOP #${focusedStop.stopNumber} READY TO COMPLETE"
                  isRedirectingToNextStop -> "REDIRECTING TO STOP #${focusedStop.stopNumber} • ${focusedStop.shortShopName.uppercase()}"
                  else -> "DIRECTIONS TO STOP #${focusedStop.stopNumber} • ${focusedStop.shortShopName.uppercase()}"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
            if (activeSteps.size > 1) {
              Text(
                text = if (showInlineSteps) "Hide Steps" else "Steps (${activeSteps.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { showInlineSteps = !showInlineSteps }
                  .padding(horizontal = 6.dp, vertical = 2.dp)
                  .testTag("nav_toggle_steps_button"),
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
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
                imageVector = maneuverIconFor(if (isFocusedActive) firstStep else null),
                contentDescription = "Next turn",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            val headerTitle = when {
              focusedStop == null -> "All Deliveries Completed"
              isFocusedActive && firstStep != null && firstStep.instruction.isNotBlank() -> firstStep.instruction
              else -> "Stop ${focusedStop.stopNumber} of ${planState.totalStops}: ${focusedStop.shop.name}"
            }
            val subText = when {
              focusedStop == null -> "Return to hub"
              isFocusedActive && routeGeometry != null && routeGeometry.activeLegDistanceMeters > 0 ->
                "Stop ${focusedStop.stopNumber}/${planState.totalStops} (${focusedStop.shortShopName}) • ${formatEtaSummary(routeGeometry.activeLegDistanceMeters, routeGeometry.activeLegDurationSeconds)}"
              focusedStop.distanceFromUserMeters != null ->
                "Stop ${focusedStop.stopNumber}/${planState.totalStops} • ${formatDistanceShort(focusedStop.distanceFromUserMeters)} • ${focusedStop.shop.address}"
              else ->
                "Stop ${focusedStop.stopNumber}/${planState.totalStops} • ${focusedStop.shop.address}"
            }
            Text(
              text = headerTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
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

        if (showInlineSteps && isFocusedActive && activeSteps.isNotEmpty()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            activeSteps.take(4).forEachIndexed { idx, step ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = maneuverIconFor(step),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text = "${idx + 1}. ${step.instruction}" +
                    if (step.distanceMeters > 0) " (${formatDistanceShort(step.distanceMeters)})" else "",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
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
          .clickable(onClick = onFitRouteClick)
          .testTag("fullscreen_fit_route_button"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.FitScreen, contentDescription = "Fit all numbered stops on map", tint = MaterialTheme.colorScheme.primary)
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
          .clickable(onClick = onRecenterClick)
          .testTag("fullscreen_recenter_gps_button"),
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
        if (orderedStops.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            orderedStops.forEach { stop ->
              val isFocused = focusedStop?.shop?.id == stop.shop.id
              val chipBg = when {
                isFocused && stop.isActiveTarget -> MaterialTheme.colorScheme.primary
                isFocused -> MaterialTheme.colorScheme.secondary
                stop.isCompleted -> MaterialTheme.semanticColors.successContainer
                stop.isActiveTarget -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
              }
              val chipContent = when {
                isFocused -> MaterialTheme.colorScheme.onPrimary
                stop.isCompleted -> MaterialTheme.semanticColors.success
                stop.isActiveTarget -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
              }
              Surface(
                color = chipBg,
                shape = RoundedCornerShape(999.dp),
                border = if (isFocused) {
                  null
                } else {
                  BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                },
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .clickable { onFocusPin(stop.shop.id) }
                  .testTag("nav_stop_chip_${stop.stopNumber}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Text(
                    text = "${stop.stopNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = chipContent,
                  )
                  Text(
                    text = stop.shortShopName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                    color = chipContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        if (focusedStop != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                color = when {
                  focusedStop.isActiveTarget -> MaterialTheme.colorScheme.primary
                  focusedStop.isCompleted -> MaterialTheme.semanticColors.success
                  else -> MaterialTheme.colorScheme.secondary
                },
                shape = CircleShape,
                modifier = Modifier.size(28.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = focusedStop.stopNumber.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = focusedStop.shop.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${focusedStop.shop.address} • ${focusedStop.paymentLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              if (!hasLocationPermission) {
                Surface(color = MaterialTheme.semanticColors.warningContainer, shape = RoundedCornerShape(999.dp)) {
                  Text(
                    text = "GPS Off",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.semanticColors.warning,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  )
                }
              }
              val canPrev = focusedIndex > 0
              val canNext = focusedIndex in 0 until (orderedStops.size - 1)
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .size(36.dp)
                  .clip(CircleShape)
                  .clickable(enabled = canPrev) {
                    if (canPrev) onFocusPin(orderedStops[focusedIndex - 1].shop.id)
                  }
                  .testTag("nav_prev_stop_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous stop",
                    tint = if (canPrev) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .size(36.dp)
                  .clip(CircleShape)
                  .clickable(enabled = canNext) {
                    if (canNext) onFocusPin(orderedStops[focusedIndex + 1].shop.id)
                  }
                  .testTag("nav_next_stop_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next stop",
                    tint = if (canNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }

          if (!isFocusedActive && activeStop != null) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (focusedStop.isCompleted) {
                    "Stop ${focusedStop.stopNumber} (${focusedStop.shortShopName}) • Completed"
                  } else {
                    "Deliver #${activeStop.stopNumber} (${activeStop.shortShopName}) before completing #${focusedStop.stopNumber}"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Go to #${activeStop.stopNumber}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onFocusPin(activeStop.shop.id) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }

          if (isFocusedActive) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = { onCallShopClick(focusedStop.shop) },
                modifier = Modifier.weight(0.85f),
              ) {
                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call")
              }
              OutlinedButton(
                onClick = {
                  if (onOpenStopExternalMapsClick != null) {
                    onOpenStopExternalMapsClick(focusedStop)
                  } else {
                    onOpenExternalMapsClick()
                  }
                },
                modifier = Modifier
                  .weight(1.25f)
                  .testTag("nav_open_external_maps_button"),
              ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Google Maps Directions", maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }

            if (focusedStop.isReadyToCompleteDelivery) {
              Button(
                onClick = onMarkActiveDeliveredClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.semanticColors.success),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp)
                  .testTag("fullscreen_mark_delivered_button"),
              ) {
                Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Complete Delivery — Stop #${focusedStop.stopNumber}",
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            } else {
              Button(
                onClick = onConfirmArrivedClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp)
                  .testTag("nav_bypass_arrived_button"),
              ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "I have reached the location",
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = { onCallShopClick(focusedStop.shop) },
                modifier = Modifier.weight(0.8f),
              ) {
                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call")
              }
              OutlinedButton(
                onClick = {
                  if (onOpenStopExternalMapsClick != null) {
                    onOpenStopExternalMapsClick(focusedStop)
                  } else {
                    onOpenExternalMapsClick()
                  }
                },
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Maps")
              }
              if (activeStop != null) {
                Button(
                  onClick = { onFocusPin(activeStop.shop.id) },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                  modifier = Modifier
                    .weight(1.3f)
                    .testTag("nav_jump_active_stop_button"),
                ) {
                  Icon(Icons.Rounded.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Next #${activeStop.stopNumber}", maxLines = 1)
                }
              } else {
                Button(
                  onClick = onExitFullscreen,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.weight(1.3f),
                ) {
                  Text("Done")
                }
              }
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
