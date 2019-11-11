package com.xenderx.googlemapssdktest.ui.fragments


import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.xenderx.googlemapssdktest.utils.Constants.Companion.MAPVIEW_BUNDLE_KEY
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterManager
import com.xenderx.googlemapssdktest.*
import com.xenderx.googlemapssdktest.groupieitems.UserItem
import com.xenderx.googlemapssdktest.models.User
import com.xenderx.googlemapssdktest.models.UserLocation
import com.xenderx.googlemapssdktest.utils.CustomMapMarker
import com.xenderx.googlemapssdktest.utils.MarkerRenderer
import com.xenderx.googlemapssdktest.viewmodels.UserLocationViewModel
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_user_list.view.*
import java.util.ArrayList


/**
 * A simple [Fragment] subclass.
 */
class UserListFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "UserListFragment"
    }

    private lateinit var mMapView: MapView
    private lateinit var mUserLocationViewModel: UserLocationViewModel
    private lateinit var mClusterManager: ClusterManager<CustomMapMarker>
    private lateinit var mMarkerRenderer: MarkerRenderer
    private lateinit var mMap: GoogleMap

    private val mMarkers = ArrayList<CustomMapMarker>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user_list, container, false)

        val userAdapter = GroupAdapter<GroupieViewHolder>()
        view.recycler_view_user_list.layoutManager = LinearLayoutManager(activity)
        view.recycler_view_user_list.adapter = userAdapter

        val user = (activity?.applicationContext as UserClient).user

        user?.let {
            userAdapter.add(it.toUserItem())
        }

        mUserLocationViewModel = ViewModelProviders.of(activity!!).get(UserLocationViewModel::class.java)
        mUserLocationViewModel.getUserLocationLiveData().observe(viewLifecycleOwner, Observer<UserLocation> {
            (userAdapter.getItem(0) as UserItem).setCoordinates(it.geoLocation)
        })

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)

        mMapView = view.findViewById(R.id.map_view)

        mMapView.onCreate(mapViewBundle)
        mMapView.getMapAsync(this)

        return view
    }

    fun enableMyLocation() {
        if (::mMap.isInitialized) {
            mMap.isMyLocationEnabled = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }

        mMapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mMapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mMapView.onStop()
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map

        if (!::mClusterManager.isInitialized) {
            mClusterManager = ClusterManager(activity?.applicationContext, map)
        }
        if (!::mMarkerRenderer.isInitialized) {
            mMarkerRenderer =
                MarkerRenderer(activity!!, map, mClusterManager)
            mClusterManager.renderer = mMarkerRenderer
        }

        mUserLocationViewModel.getUserLocationsListLiveData().observe(viewLifecycleOwner, Observer {
            it?.let { userLocations ->
                addMarkers(userLocations)
            }
        })

        ::getUsersLocations.afterEvery(2000)

//        val userLocation = LatLng(
//            mUserLocationViewModel.getUserLocation()?.geoLocation?.latitude ?: 0.0,
//            mUserLocationViewModel.getUserLocation()?.geoLocation?.longitude ?: 0.0
//        )
//        Log.d(TAG, "onMapReady: $userLocation")
//        map.addMarker(MarkerOptions().position(lahore).title("Marker"))
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f))
    }

    fun moveMapCameraTo(latLng: LatLng) {
        if (::mMap.isInitialized) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.5f))
        }
    }

    private fun addMarkers(usersLocations: ArrayList<UserLocation>) {
        for (location in usersLocations) {

            if (location.user.userId == FirebaseAuth.getInstance().currentUser?.uid) {
                continue
            }

            val latLng = LatLng(location.geoLocation.latitude, location.geoLocation.longitude)
            val marker = CustomMapMarker(
                location.user.userName,
                location.user.email,
                latLng,
                location.user
            )

            mClusterManager.addItem(marker)
            mMarkers.add(marker)
//            map.addMarker(MarkerOptions().position(latLng).title(location.user.userName))
        }
        mClusterManager.cluster()
    }

    override fun onPause() {
        mMapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mMapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView.onLowMemory()
    }

    private fun User.toUserItem(): UserItem {
        return UserItem(this)
    }

    private fun getUsersLocations() {
        for (marker in mMarkers) {
            FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_user_location))
                .document(marker.user.userId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updatedUserLocation = task.result?.toObject(UserLocation::class.java)
                        val updatedCoordinated = LatLng(
                            updatedUserLocation?.geoLocation?.latitude ?: 0.0,
                            updatedUserLocation?.geoLocation?.longitude ?: 0.0
                        )

                        marker.position = updatedCoordinated
                        mMarkerRenderer.updateMarker(marker)
                    }
                }
        }
    }

}


val handler = Handler()
fun (() -> Unit).afterEvery(delay: Long) {
    handler.postDelayed({
        this.invoke()
        this.afterEvery(delay)
    }, delay)
}

fun (() -> Unit).stop() {
    handler.removeCallbacks(this)
}