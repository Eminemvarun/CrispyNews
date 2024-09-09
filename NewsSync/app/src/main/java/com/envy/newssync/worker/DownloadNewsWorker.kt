package com.envy.newssync.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.envy.crispynews.models.NewsArticle
import com.envy.newssync.repository.FirestoreRepository

class DownloadNewsWorker(
    appContext: Context,
    workerParameters: WorkerParameters
    ):CoroutineWorker(appContext,workerParameters){
    override suspend fun doWork(): Result {
        val service = MediaStackService.create()
        val firestorerepo = FirestoreRepository()
        return try{
            val response = service.getNews(apiKey = "1e7fe8506652ea53ff8c2c54edac0120")
            // Log data size

            val filteredNews = response.data.filter { it.image !=null }
            Log.i("ENVYLOG Response", filteredNews.toString())
            Log.i("ENVYLOG Size filtered articles",filteredNews.size.toString())
            filteredNews.forEach { article :NewsArticle->
                try {
                    firestorerepo.saveNews(article)
                } catch (e: Exception) {
                    Log.e("ENVYLOG ERROR: ", "Failed to save article", e)
                }
            }
            val dataToSend = Data.Builder().putString("result","Success!").build()
            saveLastSyncTime()
            Result.success(dataToSend)
        }catch (e:retrofit2.HttpException){
            e.printStackTrace()
            e.response()?.errorBody()?.let { Log.e("ENVY ERROR", it.string()) }
            Result.retry()
        }catch (e :Exception){
            e.printStackTrace()
            Result.failure()
        }

    }

    private fun saveLastSyncTime() {
        val sharedPreferences = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("last_sync_time", System.currentTimeMillis())
            apply()
        }
    }

}