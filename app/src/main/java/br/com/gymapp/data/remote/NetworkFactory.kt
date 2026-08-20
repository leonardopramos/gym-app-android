package br.com.gymapp.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkFactory {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"

    fun createApi(): GymApi = createApi(DEFAULT_BASE_URL)

    fun createApi(baseUrl: String): GymApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GymApi::class.java)
}
