package com.orbis.orbis.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.orbis.orbis.BuildConfig
import com.orbis.orbis.helpers.PrefManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SwaggerApiClient {
    private const val JAVA_BASE_URL = BuildConfig.BASE_URL
    private const val X_MASTER_KEY = BuildConfig.X_MASTER_KEY
    lateinit var retrofit: Retrofit
    fun getClient(context: Context): Retrofit {
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val okHttpClient = OkHttpClient().newBuilder()
            .readTimeout(160, TimeUnit.SECONDS)
            .writeTimeout(160, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val originalRequest = chain.request()
                val builder: Request.Builder = originalRequest.newBuilder()
                    .addHeader("LANGUAGE", PrefManager(context).getLanguage()!!)
                    .addHeader("X-Master-Key", X_MASTER_KEY)
                val newRequest: Request = builder.build()
                chain.proceed(newRequest)
            }).addInterceptor(interceptor).build()

        val gson = GsonBuilder()
            .setLenient()
            .create()
        retrofit = Retrofit.Builder()
            .baseUrl(JAVA_BASE_URL)
            .client(okHttpClient)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit
    }
}