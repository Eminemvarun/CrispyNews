package com.envy.crispynews.models
import java.util.Date

//Model class news article
data class NewsArticle (
    val title: String = "",
    val description: String? = null,
    val url: String = "",
    val image: String? = null,
    val source: String? = null,
    val published_at: Date? = null,
    val category: String = "",
    val popularity: Long = 0
    ) {
    var id: String = generateUniqueId(title, published_at,source)
    companion object {
        private fun generateUniqueId(title: String, publishedAt: Date?, source : String?)
        : String {
            // Combine title and formattedDate to create a unique ID
            return "${title}_${publishedAt?.time}_${source}"
        }
    }
}