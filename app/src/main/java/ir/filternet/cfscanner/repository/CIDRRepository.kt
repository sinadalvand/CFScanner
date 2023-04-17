package ir.filternet.cfscanner.repository

import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.db.entity.CidrEntity
import ir.filternet.cfscanner.mapper.mapToCidr
import ir.filternet.cfscanner.model.CIDR
import ir.filternet.cfscanner.model.Log
import ir.filternet.cfscanner.model.STATUS
import ir.filternet.cfscanner.scanner.CFSLogger
import ir.filternet.cfscanner.utils.AppConfig
import ir.filternet.cfscanner.utils.calculateUsableHostCountBySubnetMask
import ir.filternet.cfscanner.utils.getFromGithub
import kotlinx.coroutines.delay
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CIDRRepository @Inject constructor(
    val logger: CFSLogger,
) : BasicRepository() {

    private val cloudflareASNs = arrayOf("AS13335", "AS209242")
    private val cloudflareOkOctave = arrayOf(
        "23", "31", "45", "66", "80", "89", "103", "104", "108", "141",
        "147", "154", "159", "168", "170", "173", "185", "188", "191",
        "192", "193", "194", "195", "199", "203", "205", "212"
    )

    private val DEPRECATION_TIME = 1000 * 60 * 60 * 10 // 10h
    private val cidrDao by lazy { db.CIDRDao() }

    suspend fun getAllCIDR(): List<CIDR> {
        if (isDeprecated()) {
            logger.add(Log("cidrs", "getting CIDRS from network ...", STATUS.INPROGRESS))
            getDataFromNetwork()
            logger.add(Log("cidrs", "CIDRS from network fetched.", STATUS.SUCCESS))
        } else {
            logger.add(Log("cidrs", "CIDRS from cache.", STATUS.SUCCESS))
        }
        return cidrDao.getAll().map { it.mapToCidr() }
    }

    suspend fun getById(uid: Int): CIDR? {
        return cidrDao.findById(uid)?.mapToCidr()
    }

    suspend fun getByAddress(address: String): CIDR? {
        return cidrDao.findByAddress(address)?.mapToCidr()
    }


    private suspend fun getDataFromNetwork() {
        val list = arrayListOf<String>()
        try {
            val result = getCIDRSGithub()?.filter {
                val firstOctave = it.split(".").firstOrNull()
                cloudflareOkOctave.contains(firstOctave)
            }/*?.sortedByDescending {
                (it.split("/").lastOrNull() ?: "24").toInt()
            }*/ ?: throw Exception("result is null")
            saveAllCIDR(result)
            saveLastUpdate()
        }catch (e:Exception){
            logger.add(Log("cidrs", "CIDRS fetching failed. retry ...", STATUS.FAILED))
            delay(4000)
            getDataFromNetwork()
        }
        // TODO check address status before request

    }


    private fun getCIDRSGithub(): List<String>? {
        return try {
            getFromGithub(AppConfig.CIDR_Address).lines().toList() ?: emptyList()
        } catch (e: Exception) {
            println("An error occurred: " + e.message)
            e.printStackTrace()
            null
        }
    }

    private fun getCidrsListfromCF(): List<String>? {
        return try {
            val request = Request.Builder()
                .url("https://www.cloudflare.com/ips-v4")
                .build()
            val response = okHttp.newCall(request).execute()
            val body = response.body?.string()

            body?.lines()?.toList() ?: emptyList()
        } catch (e: Exception) {
            println("An error occurred: " + e.message)
            e.printStackTrace()
            null
        }
    }

    private suspend fun saveAllCIDR(list: List<String>) {
        var ipCount = 0
        list.map {
            val splitted = it.split("/")
            val addres = splitted.firstOrNull() ?: ""
            val mask = (splitted.lastOrNull() ?: "0").toInt()
            CidrEntity(addres, mask)
        }.forEach {
            val count = calculateUsableHostCountBySubnetMask(it.subnetMask)
            ipCount += (count)
            Timber.d("CFScanner: save  ${it.address}/${it.subnetMask} Count: $count")
            cidrDao.insert(it)
        }
        Timber.d("CFScanner: All ip Count is $ipCount")
    }

    private fun getLastUpdateDate(): Date {
        return Date(tinyStorage.updateCIDR)
    }

    private fun saveLastUpdate() {
        tinyStorage.updateCIDR = Date().time
    }

    private fun isDeprecated(): Boolean {
        return true
        return getLastUpdateDate().before(Date(System.currentTimeMillis() - DEPRECATION_TIME))
    }

}