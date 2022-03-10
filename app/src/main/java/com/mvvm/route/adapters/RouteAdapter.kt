package com.mvvm.route.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.mvvm.route.data.model.Coordinates
import com.mvvm.route.data.model.Route
import com.mvvm.route.databinding.ItemRouteBinding
import com.mvvm.route.tools.*

class RouteAdapter(
    private val onItemClickListener: OnClickListener,
    private val onDeleteButtonClickListener: OnClickListener
) :
    ListAdapter<Route, RouteAdapter.RouteViewHolder>(RouteDiffCallBack()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        return RouteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClickListener.onClick(route, position)
        }
        holder.bind(route, onDeleteButtonClickListener, position)
    }

    class RouteViewHolder private constructor(private val binding: ItemRouteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("QueryPermissionsNeeded")
        fun bind(route: Route, onDeleteButtonClickListener: OnClickListener, position: Int) =
            with(itemView) {
                val coordinatesList: List<Coordinates> = route.coordinates
                val fromCoordinate = coordinatesList.first()
                val toCoordinate = coordinatesList.last()

                val distanceMts = SphericalUtil.computeDistanceBetween(
                    LatLng(fromCoordinate.latitude, fromCoordinate.longitude),
                    LatLng(toCoordinate.latitude, toCoordinate.longitude)
                )

                val distanceKm = (distanceMts / KM)

                binding.tvRouteName.text = route.name
                binding.tvDistance.text = String.format("%3.1f Km", distanceKm)

                binding.shareItemButton.setOnClickListener {
                    it.let {
                        val urlLocation = String.format(
                            URL_SHARE_LOCATION,
                            fromCoordinate.latitude,
                            fromCoordinate.longitude,
                            toCoordinate.latitude,
                            toCoordinate.longitude
                        )

                        val totalTime = toCoordinate.timestamp - fromCoordinate.timestamp
                        val shareText = String.format(
                            SHARE_TEXT,
                            urlLocation,
                            distanceKm,
                            Utils().getHumanTimeFormatFromMilliseconds(totalTime)
                        )


                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(
                            Intent.EXTRA_TEXT, shareText
                        )

                        binding.root.context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                SHARE_WITH_TEXT
                            )
                        )

                    }
                }

                binding.deleteItemButton.setOnClickListener {
                    onDeleteButtonClickListener.onClick(route, position)
                }
            }

        companion object {
            fun from(parent: ViewGroup): RouteViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemRouteBinding.inflate(layoutInflater, parent, false)
                return RouteViewHolder(binding)
            }
        }

    }

    class OnClickListener(val clickListener: (route: Route, position: Int) -> Unit) {
        fun onClick(route: Route, itemPosition: Int) = clickListener(route, itemPosition)
    }

    class RouteDiffCallBack : DiffUtil.ItemCallback<Route>() {
        override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem == newItem
        }

    }
}