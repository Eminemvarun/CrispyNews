import com.envy.crispynews.models.NewsArticle
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MediaStackService {
    @GET("news")
    suspend fun getNews(
        @Query("access_key") apiKey: String,
        @Query("countries") country: String = "gb",
        @Query("limit") limit: String = "100"
    ): MediaStackResponse

    companion object {
        fun create(): MediaStackService {
            return Retrofit.Builder()
                .baseUrl("http://api.mediastack.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MediaStackService::class.java)
        }
    }
}

data class MediaStackResponse(
    val data: List<NewsArticle>
)