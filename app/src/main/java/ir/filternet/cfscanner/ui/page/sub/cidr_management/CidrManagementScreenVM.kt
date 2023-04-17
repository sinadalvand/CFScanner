package ir.filternet.cfscanner.ui.page.sub.cidr_management

import androidx.annotation.WorkerThread
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.model.CIDR
import ir.filternet.cfscanner.model.ScanSettings
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.CIDRRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class CidrManagementScreenVM @Inject constructor(
    private val cidrRepository: CIDRRepository,
    private val tinyStorage: TinyStorage,
) : BaseViewModel<CidrManagementContract.Event, CidrManagementContract.State, CidrManagementContract.Effect>() {


    init {
        Timber.d("CidrManagementScreenVM init")

        vmScope {
            tinyStorage.scanSettings.apply {
                setState { copy(autofetch = this@apply.autoFetch, shuffle = this@apply.shuffle) }
            }
        }

        vmScope {
            getCidrList()
        }

    }


    override fun setInitialState(): CidrManagementContract.State {
        return CidrManagementContract.State(loading = true)
    }

    override fun handleEvents(event: CidrManagementContract.Event) {
        when (event) {
            is CidrManagementContract.Event.AddIpRanges -> {
                addCidr(event.ranges)
            }
            is CidrManagementContract.Event.RemoveCIDR -> {
                removeCidr(event.cidr)
            }
            is CidrManagementContract.Event.MoveCidr -> {
                saveCidrMoves(event.from, event.to)
            }
            CidrManagementContract.Event.SaveCidrs -> {
                saveCidrList()
            }
            is CidrManagementContract.Event.AutoFetchChange -> {
                saveAutoFetchState(event.enabled)
            }
            is CidrManagementContract.Event.ShuffleChange -> {
                saveShuffleState(event.enabled)
            }
        }
    }

    @WorkerThread
    private fun getCidrList() = vmScope(Dispatchers.IO) {
        val cidrs = cidrRepository.getAllCIDR().sortedBy { it.position }
        setState { copy(loading = false, cidrs = cidrs) }
    }

    private fun saveShuffleState(enabled: Boolean) {
        tinyStorage.scanSettings.copy(shuffle = enabled).apply {
            tinyStorage.scanSettings = this
            setState { copy(shuffle = enabled) }
        }
    }

    private fun saveAutoFetchState(enabled: Boolean) {
        tinyStorage.scanSettings.copy(autoFetch = enabled).apply {
            tinyStorage.scanSettings = this
            setState { copy(autofetch = enabled) }
        }
    }

    private fun saveCidrMoves(from: Int, to: Int) {
        setState {
            val newCidrs = cidrs.toMutableList().apply {
                add(to, removeAt(from).copy(position = to))
                add(from, removeAt(from).copy(position = from))
            }
            copy(cidrs = newCidrs)
        }
    }

    private fun saveCidrList() {
        vmScope {
            cidrRepository.updateCidrPositions(viewState.value.cidrs)
        }
    }

    private fun removeCidr(cidr: CIDR) {
        vmScope {

            // delete from database
            cidrRepository.deleteCIdr(cidr)


            // update positions after item in list
            val newCidr = viewState.value.cidrs.toMutableList()
                .apply { remove(cidr) }
                .mapIndexed { index, cidr ->
                    cidr.copy(position = index)
                }


            // update ui and db
            setState { copy(cidrs = newCidr) }
            saveCidrList()

        }
    }

    private fun addCidr(rawCidrs: List<String>) {
        vmScope(Dispatchers.IO) {

            val cidrs = rawCidrs.map {
                val (address, mask) = it.split("/")
                CIDR(address, mask.toInt(), custom = true)
            }

            // update positions after item in list
            val newCidr = viewState.value.cidrs.toMutableList()
                .apply { addAll(0, cidrs) }
                .mapIndexed { index, cidr ->
                    cidr.copy(position = index)
                }

            cidrRepository.updateCidrPositions(newCidr)
            getCidrList()
        }
    }


}