package com.mvvm.route.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.mvvm.route.data.model.Route

@Dao
interface RouteDao {
    @Insert
    suspend fun insertRoute(route: Route): Long

    @Query("SELECT * FROM Route ORDER BY name DESC")
    fun getAllRoute(): LiveData<List<Route>>

    @Query("SELECT * FROM Route WHERE name=:nameRoute LIMIT 1")
    fun findRouteByName(nameRoute: String): LiveData<List<Route>>

    @Delete
    suspend fun delete(route: Route): Int

    @Query("DELETE FROM Route")
    suspend fun clearRoutes(): Int

}