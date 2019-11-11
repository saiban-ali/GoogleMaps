package com.xenderx.googlemapssdktest.ui.activities

import android.app.ActivityManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.xenderx.googlemapssdktest.utils.Constants
import com.xenderx.googlemapssdktest.R
import com.xenderx.googlemapssdktest.UserClient
import com.xenderx.googlemapssdktest.viewmodels.UserLocationViewModel
import com.xenderx.googlemapssdktest.models.User
import com.xenderx.googlemapssdktest.models.UserLocation
import com.xenderx.googlemapssdktest.services.LocationService
import com.xenderx.googlemapssdktest.ui.fragments.UserListFragment
import com.xenderx.googlemapssdktest.utils.IFragmentCallback

class MainActivity : AppCompatActivity(), IFragmentCallback {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var mLocationPermissionGranted: Boolean = false
//    private lateinit var user: User
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mUserLocation: UserLocation
    private lateinit var mUserLocationViewModel: UserLocationViewModel

//    public fun getUser() = user
    override fun checkLocationPermission(): Boolean = mLocationPermissionGranted

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val bundle = intent.extras
//        user = bundle?.getParcelable(Constants.USER) ?: User()

        mUserLocationViewModel = ViewModelProviders.of(this).get(UserLocationViewModel::class.java)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mFirebaseFirestore = FirebaseFirestore.getInstance()


        val fragment = UserListFragment()

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_user_list,
                fragment
            )
            .commit()
    }

    override fun onResume() {
        super.onResume()

        if (checkMapServices()) {
            if (!mLocationPermissionGranted) {
                getLocationPermission()
            } else {
                getUserDetails()
                getAllUsers()
            }
        }
    }

    private fun getUserDetails() {
        if (!::mUserLocation.isInitialized) {
            mUserLocation = UserLocation()
        }

        mUserLocation.user = (applicationContext as UserClient).user ?: User()
        getCurrentLocation()
        (supportFragmentManager.findFragmentById(R.id.fragment_user_list) as UserListFragment).enableMyLocation()

//        FirebaseAuth.getInstance().uid?.let {
//            mFirebaseFirestore.collection(getString(R.string.collection_users))
//                .document(it)
//                .get()
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        val user = task.result?.toObject(User::class.java) ?: User()
//                        mUserLocation.user = user
//                        getCurrentLocation()
//                    }
//                }
//        }
    }

    private fun saveUserLocation() {
        mUserLocationViewModel.setUserLocation(mUserLocation)
        if (::mUserLocation.isInitialized) {
            FirebaseAuth.getInstance().uid?.let {
                mFirebaseFirestore.collection(getString(R.string.collection_user_location))
                    .document(it)
                    .set(mUserLocation)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "saveUserLocation: user location saved with $mUserLocation")
                        } else {
                            Log.e(TAG, "saveUserLocation: ", task.exception)
                        }
                    }
            }
        }
    }

    private fun getCurrentLocation() {
        mFusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val location = task.result
                var geoPoint: GeoPoint? = null
                location?.let {
                    geoPoint = GeoPoint(it.latitude, it.longitude)
                }

                if (geoPoint != null) {
                    mUserLocation.geoLocation = geoPoint as GeoPoint
                    mUserLocation.timestamp = null

                    val fragment = supportFragmentManager.findFragmentById(R.id.fragment_user_list)
                    fragment?.let {
                        if (fragment is UserListFragment) {
                            fragment.moveMapCameraTo(LatLng(
                                geoPoint?.latitude ?: 0.0,
                                geoPoint?.longitude ?: 0.0
                            ))
                        }
                    }

                    saveUserLocation()
                    startLocationService()
                } else {
                    getCurrentLocation()
                    Log.d(TAG, "getCurrentLocation: Called again")
                }
            }
        }
    }

    private fun checkMapServices(): Boolean {
        if (checkGoogleServices()) {
            if (isGPSEnabled()) {
                return true
            }
        }
        return false
    }

    private fun showGPSDisabledError() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent,
                    Constants.PERMISSIONS_REQUEST_ENABLE_GPS
                )
            }
        val alert = builder.create()
        alert.show()
    }

    private fun isGPSEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDisabledError()
            return false
        }
        return true
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermissionGranted = true
//            getChatrooms()
            getUserDetails()
            getAllUsers()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    private fun getAllUsers() {
        mFirebaseFirestore.collection(getString(R.string.collection_user_location))
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result?.let { querySnapShot ->
                        val usersList = ArrayList<UserLocation>()
                        for (document in querySnapShot.documents) {
                            val userLocation = document.toObject(UserLocation::class.java)
                            userLocation?.let { userLoc -> usersList.add(userLoc) }
                        }
                        mUserLocationViewModel.setUserLocationsList(usersList)
                        Log.d(TAG, "getAllUsers: Got all users locations")
                    }
                } else {
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "getAllUsers: ", it.exception)
                }
            }
    }

    private fun checkGoogleServices(): Boolean {
        Log.d(TAG, "checkGoogleServices: checking google services version")

        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@MainActivity)

        when {
            available == ConnectionResult.SUCCESS -> {
                //everything is fine and the user can make map requests
                Log.d(TAG, "checkGoogleServices: Google Play Services is working")
                return true
            }
            GoogleApiAvailability.getInstance().isUserResolvableError(available) -> {
                //an error occured but we can resolve it
                Log.d(TAG, "checkGoogleServices: an error occured but we can fix it")
                val dialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this@MainActivity, available,
                        Constants.ERROR_DIALOG_REQUEST
                    )
                dialog.show()
            }
            else -> Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            val serviceIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "startLocationService: LocationService started")
        }
    }

    private fun isLocationServiceRunning(): Boolean {

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.className ==
                    "com.xenderx.googlemapssdktest.services.LocationService") {
                Log.d(TAG, "isLocationServiceRunning: LocationSevice is already running")
                return true
            }
        }
        Log.d(TAG, "isLocationServiceRunning: LocationService is not running")
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        when (requestCode) {
            Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: called.")
        when (requestCode) {
            Constants.PERMISSIONS_REQUEST_ENABLE_GPS -> {
                if (mLocationPermissionGranted) {
//                    getChatrooms()
                    getUserDetails()
                    getAllUsers()
                } else {
                    getLocationPermission()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> false
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        gotoLoginActivity()
    }

    private fun gotoLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
