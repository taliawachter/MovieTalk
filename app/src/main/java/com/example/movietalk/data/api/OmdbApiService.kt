package com.example.movietalk.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

interface OmdbApiService {
    @GET("/")
    fun getMovieByTitle(
        @Query("t") title: String,
        @Query("apikey") apiKey: String = "a64aa4bd"
    ): Call<OmdbMovieResponse>
}
