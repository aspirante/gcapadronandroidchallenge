package com.mvvm.route.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.SphericalUtil
import com.mvvm.route.R
import com.mvvm.route.adapters.RouteAdapter
import com.mvvm.route.data.model.Coordinates
import com.mvvm.route.data.model.Route
import com.mvvm.route.databinding.FragmentMapsBinding
import com.mvvm.route.persistence.getDatabase
import com.mvvm.route.tools.*
import com.mvvm.route.viewModelFactory.RoutesViewModelFactory
import com.mvvm.route.viewModels.RouteViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.logging.Logger

class MapRoutesFragment : Fragment(), OnMapReadyCallback {
    private val log: Logger = Logger.getLogger(MapRoutesFragment::class.java.name)

    private lateinit var mMap: GoogleMap

    private var mBinding: FragmentMapsBinding? = null
    private val binding get() = mBinding!!

    private lateinit var mViewModel: RouteViewModel
    private lateinit var mMoviesViewModelFactory: RoutesViewModelFactory

    private var isRecord: Boolean = false
    private var lastLocation: Location? = null

    private var coordinatesArray = JSONArray()
    private var itemPositionDeleted = -1

    private var fusedLocationProvider: FusedLocationProviderClient? = null

    private lateinit var utils: Utils

    private val mLocationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        interval = UPDATE_INTERVAL
        fastestInterval = FASTEST_INTERVAL
    }

    private var mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            onLocationChanged(locationResult.lastLocation)
        }
    }

    private fun onLocationChanged(location: Location) {

        if (lastLocation != null) {
            if (location.latitude == lastLocation!!.latitude && location.longitude == lastLocation!!.longitude) {
                log.info("location == lastLocation ${location.latitude} ... ${lastLocation!!.latitude}")
                return
            }
        }

        if (isRecord) {

            createJSONCoordinate(location)
            val gotLocation = "Got Location: latitude:: " + location.latitude + " longitude:: " + location.longitude
            Toast.makeText(mBinding!!.root.context, gotLocation, Toast.LENGTH_LONG).show()
            log.info(gotLocation)

            lastLocation = location

            mMap.clear()
            createMaker(
                location.latitude,
                location.longitude,
                RouteViewModel.MakerType.TRIP
            )
        }
    }

    private fun createJSONCoordinate(location: Location) {
        val routeItem = JSONObject()
        routeItem.put(LATITUDE, location.latitude)
        routeItem.put(LONGITUDE, location.longitude)
        routeItem.put(TIMESTAMP, System.currentTimeMillis())
        coordinatesArray.put(routeItem)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (servicesOK())
            fusedLocationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())

        utils = Utils()
        checkPermissions()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentMapsBinding.inflate(inflater)
        mBinding!!.lifecycleOwner = this

        val database = getDatabase(mBinding!!.root.context).routeDao()
        mMoviesViewModelFactory = RoutesViewModelFactory(database)
        mViewModel = ViewModelProvider(this, mMoviesViewModelFactory)[RouteViewModel::class.java]

        createMap()

        mViewModel.getAllRoutes()

        val adapter = RouteAdapter(RouteAdapter.OnClickListener { item, _ ->
            log.info("selected Route ::  -> $item")
            itemSelected(item)
        }, RouteAdapter.OnClickListener { route, itemPosition ->
            log.info("mViewModel.deleteRoute(route) -> $route")
            showConformDialog(route)
            itemPositionDeleted = itemPosition
        })

        mBinding!!.rvRoutes.adapter = adapter

        bindingComponents()

        liveData(adapter)

        return binding.root
    }

    private fun itemSelected(item: Route) {
        mMap.clear()

        val latLongList: ArrayList<LatLng> = ArrayList()
        for (coordinate in item.coordinates) {
            log.info("selected Route :: coordinate -> $coordinate")
            latLongList.add(LatLng(coordinate.latitude, coordinate.longitude))
        }

        createRouteMakersPolyline(item, latLongList)
    }

    private fun bindingComponents() {
        mBinding!!.swipeRefresh.setOnRefreshListener {
            log.info("swipeRefresh()")
            mViewModel.getAllRoutes()
            mBinding!!.swipeRefresh.isRefreshing = false
        }

        mBinding!!.switchRecord.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mBinding!!.switchRecord.text = getString(R.string.recording)
                isRecord = true
            } else {
                mBinding!!.switchRecord.text = getString(R.string.record)
                isRecord = false
                showSaveLocationDialog()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun liveData(adapter: RouteAdapter) {
        mViewModel.insertStatus.observe(viewLifecycleOwner) {
            when (it) {
                RouteViewModel.Status.COMPLETED -> {
                    log.info("route inserted in DB OK!")
                    mViewModel.getAllRoutes()
                }
                else -> {
                    log.info("route inserted in DB Fail!")
                }
            }
        }

        mViewModel.deleteItemStatus.observe(viewLifecycleOwner) {
            when (it) {
                RouteViewModel.Status.COMPLETED -> {

                    if (itemPositionDeleted != -1) {
                        adapter.notifyItemChanged(itemPositionDeleted)
                        adapter.notifyDataSetChanged()
                        itemPositionDeleted = -1
                    }
                    log.info("route deleted from DB OK!")
                }
                else -> {
                    log.info("route deleted from DB Fail!")
                }
            }
        }

        mViewModel.routeListDB.observe(viewLifecycleOwner) { routeData ->
            log.info("mViewModel.routeListDB()")
            routeData.let {
                adapter.submitList(null)
                adapter.submitList(it)
                mBinding!!.swipeRefresh.isRefreshing = false
                log.info("routeListDB -> $it")
            }
        }
    }

    private fun createRouteMakersPolyline(item: Route, latLongList: ArrayList<LatLng>) {
        val coordinates: List<Coordinates> = item.coordinates
        val infoLatLngPosition = latLongList[(latLongList.size / 2)]

        // mMap is the Map Object
        mMap.addPolyline(
            PolylineOptions()
                .addAll(latLongList)
                .width(15f)
                .color(Color.BLUE)
                .geodesic(true)
                .clickable(false)
        )

        createMaker(
            coordinates.first().latitude,
            coordinates.first().longitude,
            RouteViewModel.MakerType.START
        )

        createMaker(
            coordinates.last().latitude,
            coordinates.last().longitude,
            RouteViewModel.MakerType.END
        )

        val distanceMts = SphericalUtil.computeDistanceBetween(
            LatLng(coordinates.first().latitude, coordinates.first().longitude),
            LatLng(coordinates.last().latitude, coordinates.last().longitude)
        )

        val totalTime = coordinates.last().timestamp - coordinates.first().timestamp

        utils.addInfoWindow(
            infoLatLngPosition,
            mMap,
            String.format("%3.1f Km", (distanceMts / KM)),
            utils.getHumanTimeFormatFromMilliseconds(totalTime)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    private fun showSaveLocationDialog() {
        val saveDialogView: View = layoutInflater.inflate(R.layout.save_dialog, null)
        val saveDialogBuilder =
            AlertDialog.Builder(mBinding!!.root.context, R.style.CustomAlertDialog).create()
        val routeName = saveDialogView.findViewById<EditText>(R.id.dialogNameEt)

        saveDialogBuilder.setOnDismissListener { saveDialogBuilder.dismiss() }
        saveDialogBuilder.setCancelable(false)
        saveDialogBuilder.setView(saveDialogView)
        log.info("showSaveLocationDialog():: routeName length -> ${routeName.length()}")

        saveDialogView.findViewById<Button>(R.id.dialogSaveBtn).setOnClickListener {
            val name = routeName.text.toString()
            log.info("showSaveLocationDialog():: name length -> ${name.length}")

            if (name.length <= NAME_LENGTH) {
                routeName.error = getString(R.string.name_error)
            } else {
                log.info("name:: $name")
                saveDataRoute(name)
                saveDialogBuilder.dismiss()
            }
        }

        saveDialogView.findViewById<Button>(R.id.dialogCancelBtn).setOnClickListener {
            coordinatesArray = JSONArray()
            mMap.clear()
            saveDialogBuilder.dismiss()
        }

        saveDialogBuilder.show()
    }

    private fun showConformDialog(route: Route) {
        val dialog = AlertDialog.Builder(mBinding!!.root.context)
            .setTitle(getString(R.string.confirm))
            .setMessage(getString(R.string.are_you_sure_delete_item))
            .setNegativeButton(getString(R.string.cancel)) { view, _ ->
                view.dismiss()
            }
            .setPositiveButton(getString(R.string.delete)) { view, _ ->
                mViewModel.deleteRoute(route)
                view.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun saveDataRoute(routeName: String) {
        val routeItem = JSONObject()
        routeItem.put(JSON_NAME, routeName)
        routeItem.put(JSON_COORDINATES, coordinatesArray)

        log.info("saveDataRoute():: routeItem -> $routeItem")

        val routeData: Route = Gson().fromJson(routeItem.toString(), object : TypeToken<Route>() {}.type)

        log.info("routeData -> $routeData")

        mViewModel.insertRoute(routeData)
        coordinatesArray = JSONArray()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProvider?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
        }
    }

    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProvider?.removeLocationUpdates(mLocationCallback)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding!!.unbind()
        mBinding = null
    }

    /**
     *  Map section
     */

    private lateinit var mapFragment: SupportMapFragment
    private fun createMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMapToolbarEnabled = false
                mMap.uiSettings.isCompassEnabled = true
            }
        } else {
            mMap.isMyLocationEnabled = true
        }
        relocateButtonMyLocation()
    }

    @SuppressLint("ResourceType")
    private fun relocateButtonMyLocation() {
        val locationButton = (mapFragment.view?.findViewById<View>(1)?.parent as View).findViewById<View>(2)/*(mapFragment.view?.findViewById<View>(1)?.parent as ViewGroup)*/
        val rlp = locationButton.layoutParams as RelativeLayout.LayoutParams
        rlp.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        var px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()
        rlp.setMargins(px, px, px, px)

        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
        rlp.marginStart = px

        locationButton.layoutParams = rlp
    }

    private fun createMaker(latitude: Double, longitude: Double, type: RouteViewModel.MakerType) {
        val coordinatesMaker = LatLng(latitude, longitude)
        when (type) {
            RouteViewModel.MakerType.START -> {
                mMap.addMarker(
                    MarkerOptions().position(coordinatesMaker)
                        .title(getString(R.string.marker_start))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            }
            RouteViewModel.MakerType.END -> {
                mMap.addMarker(
                    MarkerOptions().position(coordinatesMaker)
                        .title(getString(R.string.marker_end))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            }
            RouteViewModel.MakerType.TRIP -> {
                mMap.addMarker(
                    MarkerOptions().position(coordinatesMaker)
                        .title(getString(R.string.marker_current))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }
        }

        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(coordinatesMaker, 15f)
        )
    }

    private fun servicesOK(): Boolean {
        log.info("servicesOK()")
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(requireContext())
        log.info("servicesOK():: result -> $result")

        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
            } else {
                Toast.makeText(requireContext(), "Can not connect!", Toast.LENGTH_SHORT).show()
            }
            return false
        }
        return true
    }

    /**
     * Permission section
     */

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.location_permission_needed))
                    .setMessage(getString(R.string.location_permission_needed_message))
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                requestLocationPermission()
            }
        } else {
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        log.info("checkBackgroundLocation()")
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        log.info("requestLocationPermission()")
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
    }

    private fun requestBackgroundLocationPermission() {
        log.info("requestBackgroundLocationPermission()")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_BACKGROUND_LOCATION)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        log.info("requestCode permissions ... -> $requestCode")
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationProvider?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
                        // Check background location
                        checkBackgroundLocation()
                    }
                } else {
                    // Permission denied
                    Toast.makeText(requireContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG).show()

                    // Check if we are in a state where the user has denied the permission and selected Don't ask again
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireActivity().packageName, null)))
                    }
                }
                return
            }
            PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationProvider?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
                        Toast.makeText(requireContext(), getString(R.string.granted_background_location_permission), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // permission denied
                    Toast.makeText(requireContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 99
        private const val PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66
        private const val PLAY_SERVICES_RESOLUTION_REQUEST = 1000
    }

}