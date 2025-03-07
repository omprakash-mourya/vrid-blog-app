package com.example.vrid.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vrid.model.Blog
import com.example.vrid.repository.BlogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BlogViewModel(private val repository: BlogRepository) : ViewModel() {
    private val _blog = MutableStateFlow<Blog?>(null)
    val blog: StateFlow<Blog?> = _blog

    private val _blogs = MutableStateFlow<List<Blog>>(emptyList())
    val blogs: StateFlow<List<Blog>> = _blogs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasError = MutableStateFlow<String?>(null)
    val hasError: StateFlow<String?> = _hasError

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage

    private var currentPage = 1
    private val pageSize = 10

    init {
        loadCachedBlogs()
    }

    private fun loadCachedBlogs() {
        viewModelScope.launch {
            try {
                val cachedBlogs = repository.getCachedBlogs()
                if (cachedBlogs.isNotEmpty()) {
                    _blogs.value = cachedBlogs
                }
            } catch (e: Exception) {
                Log.e("BlogViewModel", "Error loading cached blogs", e)
            }
        }
    }

    fun fetchBlogs(forceRefresh: Boolean = false) {
        if (_isLoading.value) return

        if (forceRefresh) {
            currentPage = 1
            _blogs.value = emptyList()
            _isLastPage.value = false
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _hasError.value = null

                val newBlogs = repository.getBlogs(currentPage, pageSize)
                
                if (newBlogs.isEmpty()) {
                    _isLastPage.value = true
                    return@launch
                }

                _blogs.value = if (currentPage == 1) {
                    newBlogs
                } else {
                    _blogs.value + newBlogs
                }

                currentPage++
            } catch (e: Exception) {
                Log.e("BlogViewModel", "Error fetching blogs", e)
                _hasError.value = "Failed to load blogs. Please check your internet connection."

                if (currentPage == 1 && _blogs.value.isEmpty()) {
                    loadCachedBlogs()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getBlog(id: String) {
        viewModelScope.launch {
            try {
                Log.d("BlogViewModel", "Getting blog with ID: $id")

                val blogFromList = _blogs.value.find { it.id == id.toInt() }
                if (blogFromList != null) {
                    _blog.value = blogFromList
                    return@launch
                }

                val cachedBlog = repository.getCachedBlog(id.toInt())
                if (cachedBlog != null) {
                    _blog.value = cachedBlog
                    return@launch
                }

                val fetchedBlog = repository.getBlog(id.toInt())
                _blog.value = fetchedBlog
            } catch (e: Exception) {
                Log.e("BlogViewModel", "Error getting blog", e)
                _hasError.value = "Failed to load blog. Please check your internet connection."
            }
        }
    }

    fun retryLastOperation() {
        _hasError.value = null
        if (_blog.value == null) {
            fetchBlogs(forceRefresh = true)
        } else {
            _blog.value?.id?.toString()?.let { getBlog(it) }
        }
    }

    fun resetError() {
        _hasError.value = null
    }
}
