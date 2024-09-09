package com.envy.newssync.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.envy.crispynews.models.NewsArticle
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    fun saveNews(newsArticle: NewsArticle) {
        val newsCollection = firestore.collection("news")
        val documentId = "${newsArticle.title}_${newsArticle.published_at?.time}_${newsArticle.source}"

        newsCollection.document(documentId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                //Skip saving document
                Log.i("ENVYLOG","Document exists $documentId")
            } else {
                // Document doesn't exist, proceed to save
                newsArticle.id = documentId
                newsArticle.popularity = 0
                newsCollection.document(documentId).set(newsArticle)
                    .addOnSuccessListener {
                        Log.i("ENVYLOG","Document saved $documentId")
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
            }
        }.addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

}