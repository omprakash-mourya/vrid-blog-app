package com.example.vrid.api

import com.example.vrid.model.Blog
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BlogApiService {
    @GET("wp-json/wp/v2/posts")
    suspend fun getBlogs(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): List<Blog>

    @GET("wp-json/wp/v2/posts/{id}")
    suspend fun getBlog(@Path("id") id: Int): Blog
}
