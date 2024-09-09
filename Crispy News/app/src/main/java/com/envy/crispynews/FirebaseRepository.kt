package com.envy.crispynews
import android.util.Log
import com.envy.crispynews.fragments.NewsFragment
import com.envy.crispynews.models.NewsArticle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private var interests : Map<String,Double> = emptyMap()
    private val lastFetchedDocuments = mutableMapOf<String, DocumentSnapshot>()

    suspend fun fetchUserInterests(userId: String): Map<String,Double>? {
        return try {
            val document = db.collection("user").document(userId)
                .get().await()
            val fetchedInterests = document.get("interests") as? Map<String,Double>
            if(fetchedInterests == null){
                null
            }
            fetchedInterests?.let {
                interests = it
            }
            Log.i("ENVYLOGS","Interests fetched as $fetchedInterests")
            fetchedInterests
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun getInterests(): Map<String, Double> {
        return interests
    }

    fun setInterests(newInterests: Map<String, Double>) {
        interests = newInterests
    }

    // Update the local interests
    fun updateInterest(category: String, increment: Double) {
        val updatedInterests = interests.toMutableMap()
        val currentInterest = updatedInterests[category] ?: 0.0
        updatedInterests[category] = (currentInterest + increment).coerceIn(0.0,1.0) // Ensure the value does not exceed 1.0
        setInterests(updatedInterests)
        Log.i("ENVYLOGS","Local Interest Updated in $category with $increment increment")
        Log.i("ENVYLOGS","Updated interest is $updatedInterests")
    }

    // Fetch the current state based on user interests
    fun getCurrentState(userId: String): Int? {
        return try {
            val timeOfDay = getCurrentTimeOfDay() // Function to get the current time of day as an index (0 to 3)

            interests.let {
                val stateIndex = convertInterestsToStateIndex(it, timeOfDay)
                stateIndex
            }
        } catch (e: Exception) {
            // Handle errors
            e.printStackTrace()
            null
        }
    }

    fun getCurrentInterestsMap(userId: String): Map<String, Double>? {
        var m_interests = interests.toMutableMap() // Fetch the user interests map
        val timeOfDay = getCurrentTimeOfDay()      // Get the current time of day as an index (0 to 3)

        // Add the time of day to the interests map
        m_interests.set("timeOfDay", timeOfDay.toDouble())

        // Return the modified interests map
        return m_interests
    }

    private fun convertInterestsToStateIndex(interests: Map<String, Double>, timeOfDay: Int): Int {
        val interestValues = listOf("general", "business", "technology", "science", "sports", "entertainment", "health")
        val intValues = interestValues.map {
            val value = interests[it] ?: 0.0
            when {
                value <= 0.3 -> 0 // Low interest
                value <= 0.6 -> 1 // Medium interest
                else -> 2 // High interest
            }
        }
        // Ensure interests size is 7
        if (intValues.size != 7) {
            throw IllegalArgumentException("Interests map must have exactly 7 elements")
        }

        var stateIndex = timeOfDay
        for (interest in intValues) {
            stateIndex = stateIndex * 3 + interest // 3 possible values (0, 1, 2) for each category
        }
        Log.i("ENVYLOGS", " state is $stateIndex  for interests as $interests, and time value $timeOfDay,")
        return stateIndex
    }

    private fun getCurrentTimeOfDay(): Int {
        val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hourOfDay in 6..11 -> 0 // Morning
            hourOfDay in 12..17 -> 1 // Noon
            hourOfDay in 18..20 -> 2 // Evening
            else -> 3 // Night
        }
    }

    fun updateUserInterestsToFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && interests.isNotEmpty()) {
            val userRef = db.collection("user")
                .document(userId)
            userRef.update("interests", interests).addOnSuccessListener {
                        Log.i("ENVYLOGS","Interests updated to firestore: $interests")
                    }
                    .addOnFailureListener { e ->
                        Log.i("ENVYLOGS","Interests update failed to firestore! ${e.message}")
                    }
        } else {
            Log.i("ENVYLOGS","User Not logged in! or empty interests")
        }
    }

    suspend fun fetchArticlesForCategory(category: String, numberOfArticles: Int): List<NewsArticle> {
        val db = FirebaseFirestore.getInstance()
        return try {
            var query = db.collection("news")
                .whereEqualTo("category", category)
                .orderBy("published_at", Query.Direction.DESCENDING)
                .limit(numberOfArticles.toLong())
            lastFetchedDocuments[category]?.let { lastDocument ->
                query = query.startAfter(lastDocument)
            }
            val result = query.get().await()
            Log.i("ENVYLOGS","Loaded ${result.size()} articles for $category")

            val articles = result.documents.mapNotNull { it.toObject(NewsArticle::class.java) }

            if (articles.isNotEmpty()) {
                lastFetchedDocuments[category] = result.documents.last()
            }
            articles

        } catch (e: Exception) {
            Log.e("ENVYLOGS","ERROR fetching articles for $category")
            e.printStackTrace()
            emptyList()
        }
    }

}
