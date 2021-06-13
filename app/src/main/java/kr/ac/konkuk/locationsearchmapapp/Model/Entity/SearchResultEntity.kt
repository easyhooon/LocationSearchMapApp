package kr.ac.konkuk.locationsearchmapapp.Model.Entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kr.ac.konkuk.locationsearchmapapp.Model.Entity.LocationLatLngEntity

@Parcelize
data class SearchResultEntity (
    val fullAddress: String,
    val name: String,
    val locationLatLng: LocationLatLngEntity
): Parcelable