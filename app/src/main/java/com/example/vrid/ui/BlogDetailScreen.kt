package com.example.vrid.ui

import android.text.Html
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(id: String, viewModel: BlogViewModel, navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val blog by viewModel.blog.collectAsState()

    LaunchedEffect(id) {
        viewModel.getBlog(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Html.fromHtml(blog?.title?.rendered ?: "", Html.FROM_HTML_MODE_COMPACT).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                blog == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Error loading blog",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = {
                                    error = null
                                    isLoading = true
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    databaseEnabled = true
                                    setSupportMultipleWindows(true)
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // Inject CSS to hide the header and adjust content
                                        val css = """
                                            javascript:(function() {
                                                var style = document.createElement('style');
                                                style.type = 'text/css';
                                                style.innerHTML = `
                                                    header.site-header,
                                                    .site-header,
                                                    .wp-block-group.has-background.has-black-background-color,
                                                    .wp-block-group.has-background.has-black-background-color.has-background {
                                                        display: none !important;
                                                    }
                                                    body {
                                                        margin-top: 0 !important;
                                                        padding-top: 0 !important;
                                                    }
                                                    .entry-content {
                                                        margin-top: 0 !important;
                                                        padding-top: 0 !important;
                                                    }
                                                `;
                                                document.head.appendChild(style);
                                            })()
                                        """.trimIndent()
                                        evaluateJavascript(css, null)
                                        isLoading = false
                                        Log.d("BlogDetailScreen", "Page finished loading")
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        super.onReceivedError(view, errorCode, description, failingUrl)
                                        error = "Error loading page: $description"
                                        isLoading = false
                                        Log.e("BlogDetailScreen", "Error loading page: $description")
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                                        Log.d("BlogDetailScreen", "Console: $message")
                                    }
                                }

                                val blogUrl = blog?.link ?: ""
                                Log.d("BlogDetailScreen", "Loading URL: $blogUrl")
                                loadUrl(blogUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Loading blog content...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}