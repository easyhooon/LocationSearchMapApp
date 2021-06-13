package kr.ac.konkuk.locationsearchmapapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.*
import kr.ac.konkuk.locationsearchmapapp.Model.Entity.LocationLatLngEntity
import kr.ac.konkuk.locationsearchmapapp.Model.Entity.SearchResultEntity
import kr.ac.konkuk.locationsearchmapapp.Utility.RetrofitUtil
import kr.ac.konkuk.locationsearchmapapp.databinding.ActivityMapBinding
import kotlin.coroutines.CoroutineContext

class MapActivity : AppCompatActivity(), OnMapReadyCallback, CoroutineScope {
    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private var currentSelectMarker: Marker? = null

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var searchResult: SearchResultEntity

    //안드로이드에서 위치정보를 가져올때 관리를 해주는 유틸리티 클래스
    private lateinit var locationManager: LocationManager

    private lateinit var myLocationListener: MyLocationListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        job = Job()

        if(::searchResult.isInitialized.not()){
            intent?.let{
                searchResult = it.getParcelableExtra(SEARCH_RESULT_EXTRA_KEY) ?: throw Exception("데이터가 존재하지 않습니다.")
                setupGoogleMap()
            }
        }
        bindViews()
    }

    private fun bindViews() = with(binding) {
        currentLocationButton.setOnClickListener {
            getMyLocation()
        }
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        currentSelectMarker = setupMarker(searchResult)

        currentSelectMarker?.showInfoWindow()
    }

    private fun setupMarker(searchResult: SearchResultEntity): Marker {
        val positionLatLng = LatLng(
            searchResult.locationLatLng.latitude.toDouble(),
            searchResult.locationLatLng.longtitude.toDouble()
        )

        val markerOptions = MarkerOptions().apply{
            position(positionLatLng)
            title(searchResult.name)
            snippet(searchResult.fullAddress)
        }
        //지도의 줌 설정
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(positionLatLng, CAMERA_ZOOM_LEVEL))

        return map.addMarker(markerOptions)
    }

    private fun getMyLocation() {
        if(::locationManager.isInitialized.not()) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGpsEnabled) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
            else {
                setMyLocationListener()
            }
        }
    }

    //위쪽에서 이미 퍼미션 체크를 했기때문에 @SuppressLint
    @SuppressLint("MissingPermission")
    private fun setMyLocationListener() {
        val minTime: Long = 1500
        val minDistance = 100f

        if (::myLocationListener.isInitialized.not()){
            myLocationListener = MyLocationListener()
        }
        with(locationManager) {
            //위치 정보를 가져옴
            requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime, minDistance, myLocationListener
            )
        }
    }

    private fun onCurrentLocationChanged(locationLatLngEntity: LocationLatLngEntity) {
        //지도의 줌 설정
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
            LatLng(
                locationLatLngEntity.latitude.toDouble(),
                locationLatLngEntity.longtitude.toDouble()
            ),
            CAMERA_ZOOM_LEVEL
            )
        )
        locationReverseGeoInformation(locationLatLngEntity)
        removeLocationListener()
    }

    private fun locationReverseGeoInformation(locationLatLngEntity: LocationLatLngEntity){
        launch(coroutineContext){
            try {
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.apiService.getReverseGeoCode(
                        lat = locationLatLngEntity.latitude.toDouble(),
                        lon = locationLatLngEntity.longtitude.toDouble()
                    )
                    if (response.isSuccessful){
                        val body = response.body()
                        withContext(Dispatchers.Main){
                            Log.e("list", body.toString())
                            body?.let {
                                currentSelectMarker =  setupMarker(
                                    SearchResultEntity(
                                        fullAddress = it.addressInfo.fullAddress ?:"",
                                        name = "내 위치",
                                        locationLatLng = locationLatLngEntity
                                    )
                                )
                                currentSelectMarker?.showInfoWindow()
                            }
                        }

                    }
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MapActivity, "검색하는 과정에서 에러가 발생했습니다. : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //리스너 등록을 했던것을 사용할필요가 없을때 제거하는 함수
    private fun removeLocationListener() {
        if(::locationManager.isInitialized && ::myLocationListener.isInitialized) {
            locationManager.removeUpdates(myLocationListener)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                setMyLocationListener()
            } else {
                Toast.makeText(this, "권한을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class MyLocationListener: LocationListener {
        override fun onLocationChanged(location: Location) {
            val locationLatLngEntity = LocationLatLngEntity(
                location.latitude.toFloat(),
                location.longitude.toFloat()
            )
            onCurrentLocationChanged(locationLatLngEntity)
        }
    }

    companion object {
        const val SEARCH_RESULT_EXTRA_KEY = "SEARCH_RESULT_EXTRA_KEY"
        const val CAMERA_ZOOM_LEVEL = 17f
        const val PERMISSION_REQUEST_CODE = 101
    }
}