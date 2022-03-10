package com.mvvm.route.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mvvm.route.persistence.RouteDao
import com.mvvm.route.viewModels.RouteViewModel
import java.util.logging.Logger

class RoutesViewModelFactory(
    private val database: RouteDao
) : ViewModelProvider.Factory {
    private val log: Logger = Logger.getLogger(RoutesViewModelFactory::class.java.name)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) {
            log.info("create()")
            return RouteViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}