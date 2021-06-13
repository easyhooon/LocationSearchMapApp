package kr.ac.konkuk.locationsearchmapapp.Utility

import kr.ac.konkuk.locationsearchmapapp.BuildConfig
import kr.ac.konkuk.locationsearchmapapp.Url
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitUtil {

    val apiService: ApiService by lazy { getRetrofit().create(ApiService::class.java) }

    private fun getRetrofit(): Retrofit {

        return Retrofit.Builder()
                //baseUrl 설정
            .baseUrl(Url.TMAP_URL)
                //Url을 통해 받아온 response body는 gson으로 파싱
            .addConverterFactory(GsonConverterFactory.create())
                //클라이언트 연결
            .client(buildOkHttpClient())
            .build()
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        //매번 api를 호출할때마다 HttpLoggingInterceptor를 통해서 로그를 찍어줌
        if (BuildConfig.DEBUG) {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            interceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
                //5초 동안 api에 대한 응답이 없으면 에러가 발생이 되도록
            .connectTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()
    }
}