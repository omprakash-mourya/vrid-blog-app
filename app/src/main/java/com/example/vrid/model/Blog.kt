package com.example.vrid.model

data class Blog(
    val id: Int,
    val title: Rendered,
    val content: Rendered,
    val excerpt: Rendered,
    val link: String
)

data class Rendered(
    val rendered: String
)