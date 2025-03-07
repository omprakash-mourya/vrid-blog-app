package com.example.vrid.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.vrid.api.BlogApiService
import com.example.vrid.model.Blog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class BlogRepository(private val context: Context) {
    private val api: BlogApiService
    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "blogs")
    private val blogListCacheFile = File(cacheDir, "blog_list.json")
    private val blogCacheDir = File(cacheDir, "individual_blogs")

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://blog.vrid.in/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(BlogApiService::class.java)

        cacheDir.mkdirs()
        blogCacheDir.mkdirs()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun getBlogs(page: Int, pageSize: Int): List<Blog> = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                throw IOException("No internet connection")
            }

            val blogs = api.getBlogs(pageSize, page)
            if (page == 1) {
                saveBlogsToCache(blogs)
            }
            blogs
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getBlog(id: Int): Blog = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                throw IOException("No internet connection")
            }

            val blog = api.getBlog(id)
            saveBlogToCache(blog)
            blog
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getCachedBlogs(): List<Blog> = withContext(Dispatchers.IO) {
        try {
            if (!blogListCacheFile.exists()) {
                return@withContext emptyList()
            }
            val json = blogListCacheFile.readText()
            val type = object : TypeToken<List<Blog>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCachedBlog(id: Int): Blog? = withContext(Dispatchers.IO) {
        try {
            val blogFile = File(blogCacheDir, "blog_$id.json")
            if (!blogFile.exists()) {
                return@withContext null
            }
            val json = blogFile.readText()
            gson.fromJson(json, Blog::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveBlogsToCache(blogs: List<Blog>) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(blogs)
            blogListCacheFile.writeText(json)
        } catch (e: Exception) {
            Log.w("BlogRepository", "Error saving blogs to cache", e)
        }
    }

    private suspend fun saveBlogToCache(blog: Blog) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(blog)
            val blogFile = File(blogCacheDir, "blog_${blog.id}.json")
            blogFile.writeText(json)
        } catch (e: Exception) {
            Log.w("BlogRepository", "Error saving blog to cache", e)
        }
    }
}