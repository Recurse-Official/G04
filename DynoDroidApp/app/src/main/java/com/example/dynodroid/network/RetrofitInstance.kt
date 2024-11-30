package com.example.dynodroid.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Define a logging interceptor to log HTTP requests and responses
private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
    // Set the log level to log the whole request and response body
    level = HttpLoggingInterceptor.Level.BODY
}

// Create an OkHttpClient with the logging interceptor
private val client: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(interceptor) // Add the logging interceptor
    .readTimeout(5, TimeUnit.MINUTES)
    .build()

// Create a Retrofit instance with the Gson converter and the base URL from Utils
private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl("https://crappie-exact-fully.ngrok-free.app")
    .client(client)
    .build()

// Create a singleton instance of the DynoAPiService using Retrofit
object DynoApi {
    val retrofitService: DynoApiService by lazy {
        retrofit.create(DynoApiService::class.java)
    }
}