package ir.filternet.cfscanner.ui.destination

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ir.filternet.cfscanner.ui.navigation.Navigation
import ir.filternet.cfscanner.ui.page.root.MainContract
import ir.filternet.cfscanner.ui.page.root.MainScreen
import ir.filternet.cfscanner.ui.page.root.MainScreenVM

@Composable
fun MainScreenDestination(
    navController: NavController,
    vm: MainScreenVM = hiltViewModel()
) {
    MainScreen(
        state = vm.viewState.value,
        effectFlow = vm.effect,
        onEventSent = { event ->  vm.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
            when(navigationEffect){
                is MainContract.Effect.Navigation.ToScan -> {
                    navController.navigate(Navigation.MainRoutes.ScanDetailsRoute(navigationEffect.scan.uid))
                }
                is MainContract.Effect.Navigation.ToCidrManagement -> {
                    navController.navigate(Navigation.MainRoutes.CIDR_MANAGEMENT)
                }
            }
        }
    )
}