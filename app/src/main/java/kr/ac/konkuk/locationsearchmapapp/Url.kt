package kr.ac.konkuk.locationsearchmapapp

object Url {
    const val TMAP_URL = "https://apis.openapi.sk.com"

    const val GET_TMAP_LOCATION = "/tmap/pois"

    //내 위치를 가져올때 쓰는 api 코드 (위도 경도를 통해서 역으로 내 위치를 가져오는)
    const val GET_TMAP_REVERSE_GEO_CODE = "/tmap/geo/reversegeocoding"
}