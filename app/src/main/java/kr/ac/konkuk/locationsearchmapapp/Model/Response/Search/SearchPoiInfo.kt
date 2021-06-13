package kr.ac.konkuk.locationsearchmapapp.Response.Search

data class SearchPoiInfo(
    val totalCount: String,
    val count: String,
    val page: String,
    val pois: Pois
)