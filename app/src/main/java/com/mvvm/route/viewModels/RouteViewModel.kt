package com.mvvm.route.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mvvm.route.data.model.Route
import com.mvvm.route.persistence.RouteDao
import kotlinx.coroutines.*
import java.util.logging.Logger

class RouteViewModel(
    private val database: RouteDao
) : ViewModel() {
    private val log: Logger = Logger.getLogger(RouteViewModel::class.java.name)

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.IO)

    private val _insertStatus = MutableLiveData<Status>()
    val insertStatus: LiveData<Status>
        get() = _insertStatus

    private val _deleteItemStatus = MutableLiveData<Status>()
    val deleteItemStatus: LiveData<Status>
        get() = _deleteItemStatus

    private val _routeListDB = MutableLiveData<LiveData<List<Route>>>()
    val routeListDB: LiveData<List<Route>>
        get() = _routeListDB.value!!

    fun insertRoute(route: Route) {
        coroutineScope.launch {
            val addedID = database.insertRoute(route)
            withContext(Dispatchers.Main) {
                log.info("insertRoute():: Inserted ID -> $addedID")
                if (addedID > 0)
                    _insertStatus.value = Status.COMPLETED
                else
                    _insertStatus.value = Status.FAIL
            }
        }
    }

    fun getAllRoutes() {
        log.info("getAllRoutes()")
        val routeList = database.getAllRoute()
        _routeListDB.value = routeList
    }

    fun deleteRoute(route: Route) {
        log.info("deleteRoute()")
        var itemDeleted: Int
        coroutineScope.launch {
            itemDeleted = database.delete(route)
            withContext(Dispatchers.Main) {
                if (itemDeleted > 0)
                    _deleteItemStatus.value = Status.COMPLETED
                else
                    _deleteItemStatus.value = Status.FAIL
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    enum class Status {
        COMPLETED,
        FAIL
    }

    enum class MakerType {
        START,
        END,
        TRIP
    }

}