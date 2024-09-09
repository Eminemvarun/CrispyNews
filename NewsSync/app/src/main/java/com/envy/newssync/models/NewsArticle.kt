package com.envy.crispynews.models

import java.sql.Timestamp

data class NewsArticle (
    val title: String,
    val description: String?,
    val url: String,
    val image: String?,
    val source: String?,
    val published_at: Timestamp?,
    val category: String,
    var popularity: Long = 0
) {
    var id: String = generateUniqueId(title, published_at)
    companion object {
        // Generate a unique ID based on title and published_at timestamp
        private fun generateUniqueId(title: String, publishedAt: Timestamp?): String {
            return "${title}_${publishedAt.toString()}"
        }
    }
}