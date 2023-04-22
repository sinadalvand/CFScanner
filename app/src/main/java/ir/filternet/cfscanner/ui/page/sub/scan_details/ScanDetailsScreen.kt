package ir.filternet.cfscanner.ui.page.sub.scan_details

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.SIDE_EFFECTS_KEY
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.service.CloudSpeedService
import ir.filternet.cfscanner.ui.common.HeaderPage
import ir.filternet.cfscanner.ui.page.main.scan.component.LoadingView
import ir.filternet.cfscanner.ui.page.sub.cidr_management.CidrManagementContract
import ir.filternet.cfscanner.ui.page.sub.scan_details.component.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScanDetailsScreen(
    state: ScanDetailsContract.State,
    effectFlow: Flow<ScanDetailsContract.Effect>?,
    onEventSent: (event: ScanDetailsContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: ScanDetailsContract.Effect.Navigation) -> Unit,
) {

    val context = LocalContext.current
    LaunchedEffect(SIDE_EFFECTS_KEY) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is ScanDetailsContract.Effect.Navigation.ToUp -> {
                    onNavigationRequested(effect)
                }
                else -> {}
            }
        }?.collect()
    }


    val loading = state.loading
    val scan = state.scan
    val connections = state.connections
    val configs = state.configs
    val isScanning = state.scanning
    val deleteable = state.deleteable
    val speedStatus = state.speedStatus
    val speedChecking = speedStatus is CloudSpeedService.SpeedServiceStatus.Checking


    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {

            item {
                HeaderPage(stringResource(id = R.string.scan_manager),Modifier.padding(horizontal = 10.dp)){
                    onNavigationRequested.invoke(ScanDetailsContract.Effect.Navigation.ToUp)
                }
            }

            if (scan != null)
                item {
                    ConfigIspCell(scan)
                }

            if (scan?.progress != null)
                item {
                    ScanProgressCell(scan.progress, (isScanning && !deleteable))
                }

            if (scan != null)
                item {
                    ActionCell(isScanning, !deleteable, {
                        ContextCompat.startForegroundService(context, Intent(context, CloudScannerService::class.java))
                        onEventSent.invoke(ScanDetailsContract.Event.ResumeScan)
                    }, stop = {
                        onEventSent.invoke(ScanDetailsContract.Event.StopScan)
                    }) {
                        onEventSent.invoke(ScanDetailsContract.Event.DeleteScan)
                    }
                }

            if (scan != null && connections.isNotEmpty())
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Spacer(
                        modifier = Modifier
                            .width(50.dp)
                            .height(3.dp)
                            .background(MaterialTheme.colors.primary, RoundedCornerShape(50))
                    )

                    Spacer(modifier = Modifier.height(20.dp))


                    SpeedCheckButton(speedStatus, {
                        ContextCompat.startForegroundService(context, Intent(context, CloudSpeedService::class.java))
                        onEventSent(ScanDetailsContract.Event.StartSortAllBySpeed)
                    }) {
                        onEventSent(ScanDetailsContract.Event.StopSortAllBySpeed)
                    }
                }



            items(connections, { it.ip }) {
                ConnectionCell(Modifier.animateItemPlacement(), it, speedChecking,configs) {
                    onEventSent.invoke(ScanDetailsContract.Event.UpdateSpeed(it))
                }
            }

            if (loading)
                item {
                    LoadingView()
                }
        }
    }

}