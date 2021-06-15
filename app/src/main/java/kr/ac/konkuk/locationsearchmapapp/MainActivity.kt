package kr.ac.konkuk.locationsearchmapapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import kr.ac.konkuk.locationsearchmapapp.MapActivity.Companion.SEARCH_RESULT_EXTRA_KEY
import kr.ac.konkuk.locationsearchmapapp.Model.Entity.LocationLatLngEntity
import kr.ac.konkuk.locationsearchmapapp.Model.Entity.SearchResultEntity
import kr.ac.konkuk.locationsearchmapapp.Response.Search.Poi
import kr.ac.konkuk.locationsearchmapapp.Response.Search.Pois
import kr.ac.konkuk.locationsearchmapapp.Utility.RetrofitUtil
import kr.ac.konkuk.locationsearchmapapp.databinding.ActivityMainBinding
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SearchRecyclerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        job = Job()

        initAdapter()
        initViews()
        //클릭하는 시점에 api를 호출
        bindViews()
        initData()
    }

    private fun initAdapter() {
        adapter = SearchRecyclerAdapter()
    }

    private fun initViews() {
        binding.emptyResultTextView.isVisible = false
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }

    private fun initData() {
        adapter.notifyDataSetChanged()
    }


    private fun bindViews() = with(binding) {
        searchButton.setOnClickListener {
            searchKeyword(searchBarInputView.text.toString())
        }
    }

    private fun setData(pois: Pois) {
        val dataList = pois.poi.map{
            SearchResultEntity(
                fullAddress = makeMainAddress(it),
                name = it.name ?: "",
                locationLatLng = LocationLatLngEntity(
//                    lng와 lat의 중심점 위치를 명시
                    it.noorLat,
                    it.noorLon
                )
            )
        }
        adapter.setSearchResultList(dataList) {
//            Toast.makeText(this, "빌딩이름 : {${it.name} 주소 : ${it.fullAddress}} 위도 : ${it.locationLatLng} ", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this, MapActivity::class.java).apply{
                    putExtra(SEARCH_RESULT_EXTRA_KEY, it)
                }
            )
        }
    }

    private fun searchKeyword(keywordString: String) {
        launch(coroutineContext) {
            try{
                //IO 쓰레드로 변환
                withContext(Dispatchers.IO) {
                    //api 호출
                    val response = RetrofitUtil.apiService.getSearchLocation(
                        keyword = keywordString
                    )
                    if (response.isSuccessful){
                        val body = response.body()
                        withContext(Dispatchers.Main) {
                            Log.e("list", body.toString())
                            body?.let{  searchResponseSchema ->
                                setData(searchResponseSchema.searchPoiInfo.pois)
                            }
                        }
                    }
                }
            } catch(e: Exception){
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "검색하는 과정에서 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun makeMainAddress(poi: Poi): String =
        if (poi.secondNo?.trim().isNullOrEmpty()) {
            (poi.upperAddrName?.trim() ?: "") + " " +
                    (poi.middleAddrName?.trim() ?: "") + " " +
                    (poi.lowerAddrName?.trim() ?: "") + " " +
                    (poi.detailAddrName?.trim() ?: "") + " " +
                    poi.firstNo?.trim()
        } else {
            (poi.upperAddrName?.trim() ?: "") + " " +
                    (poi.middleAddrName?.trim() ?: "") + " " +
                    (poi.lowerAddrName?.trim() ?: "") + " " +
                    (poi.detailAddrName?.trim() ?: "") + " " +
                    (poi.firstNo?.trim() ?: "") + " " +
                    poi.secondNo?.trim()
        }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }



}