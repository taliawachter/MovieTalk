package com.example.movietalk.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApiService {
    @GET("/")
    suspend fun getMovieByTitle(
        @Query("t") title: String,
        @Query("apikey") apiKey: String = "a64aa4bd"
    ): OmdbMovieResponse
}