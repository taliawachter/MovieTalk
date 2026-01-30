package com.example.movietalk.data.api

// Minimal OMDb response for our needs

data class OmdbMovieResponse(
    val Year: String?,
    val Genre: String?,
    val Actors: String?
)
