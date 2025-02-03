package com.example.globetrotter.network

data class UnsplashResponse(
    val results: List<UnsplashImage>
)

data class UnsplashImage(
    val urls: ImageUrls
)

data class ImageUrls(
    val regular: String // URL to the image
)