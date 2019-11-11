package com.xenderx.googlemapssdktest.utils

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.xenderx.googlemapssdktest.R
import java.util.*
import java.util.zip.Inflater

class MarkerRenderer (
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<CustomMapMarker>
) : DefaultClusterRenderer<CustomMapMarker>(context, map, clusterManager) {

    private var iconGenerator: IconGenerator = IconGenerator(context.applicationContext)
    private var markerWidth: Int = context.resources.getDimension(R.dimen.custom_marker_image_width).toInt()
    private var markerHeight: Int = context.resources.getDimension(R.dimen.custom_marker_image_height).toInt()
    private val titleTextView: TextView = TextView(context)
    private val coordinatesTextView: TextView = TextView(context)

    init {
        val padding = context.resources.getDimension(R.dimen.custom_marker_padding).toInt()
        val linearLayout = LinearLayout(context)
        linearLayout.layoutParams = ViewGroup.LayoutParams(markerWidth, markerHeight)
        linearLayout.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding)
            addView(titleTextView.apply {
                gravity = Gravity.CENTER
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(coordinatesTextView.apply {
                gravity = Gravity.CENTER
                textSize = 12f
            })
        }

//        val view = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
//            .inflate(R.layout.activity_login, null)

        iconGenerator.setContentView(linearLayout)
    }

    override fun onBeforeClusterItemRendered(
        item: CustomMapMarker?,
        markerOptions: MarkerOptions?
    ) {
        titleTextView.text = item?.title ?: ""
        coordinatesTextView.text = String.format(
            Locale.US,
            "%f, %f",
            item?.position?.latitude ?: 0.0,
            item?.position?.longitude ?: 0.0
        )
        val icon = iconGenerator.makeIcon()
        markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(icon))
    }

    override fun shouldRenderAsCluster(cluster: Cluster<CustomMapMarker>?): Boolean {
        return false
    }

    fun updateMarker(customMarker: CustomMapMarker) {
        val marker = getMarker(customMarker)
        marker?.let {
            it.position = customMarker.position

            val padding = context.resources.getDimension(R.dimen.custom_marker_padding).toInt()
            val linearLayout = LinearLayout(context)
            linearLayout.layoutParams = ViewGroup.LayoutParams(markerWidth, markerHeight)
            linearLayout.apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding)
                addView(TextView(context).apply {
                    gravity = Gravity.CENTER
                    text = customMarker.user.userName
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    gravity = Gravity.CENTER
                    text = String.format(
                        Locale.US,
                        "%f, %f",
                        customMarker.position.latitude,
                        customMarker.position.longitude
                    )
                    textSize = 12f
                })
            }

            iconGenerator.setContentView(linearLayout)
            val icon = iconGenerator.makeIcon()
            it.setIcon(BitmapDescriptorFactory.fromBitmap(icon))

        }
    }
}