package com.envy.crispynews

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class QLearningAgent(
    private val numStates: Int, // Total number of states (this could be removed if states are dynamic)
    private val numActions: Int, // Number of categories or actions
    private val alpha: Double = 0.2, // Learning rate
    private val gamma: Double = 0.8, // Discount factor
    private val epsilon: Double = 0.8, // Exploration rate
    private val categories: List<String> = listOf("general", "technology", "sports", "entertainment", "health", "science", "business") // Article categories
) {
    private val qTable: MutableMap<Int, MutableMap<String, Double>> = mutableMapOf()

    fun chooseActions(state: Int, numActions: Int = 5): List<String> {
        val sortedCategories = qTable[state]
            ?.entries
            ?.sortedByDescending { it.value }
            ?.map { it.key } ?: emptyList()
        if (Random.nextDouble() < epsilon) {
            // Exploration: choose random categories
            return categories.shuffled().take(numActions)
        } else {
            // Exploitation: choose the top categories based on Q-values
            val topCategories = sortedCategories.take(numActions)
            if (topCategories.size < numActions) {
                // If not enough top categories, add more random ones
                val additionalCategories = categories.shuffled()
                    .filter { it !in topCategories }.take(numActions - topCategories.size)
                return topCategories + additionalCategories
            }
            return topCategories
        }
    }


    fun updateQValueinTable(state: Int, action: String, reward: Double, nextState: Int) {
        require(Action.isValidAction(action)) { "Invalid action: $action" }

        val currentQ = qTable[state]?.get(action) ?: 0.0
        val maxNextQ = qTable[nextState]?.values?.maxOrNull() ?: 0.0

        val newQ = currentQ + alpha * (reward + gamma * maxNextQ - currentQ)
        qTable.computeIfAbsent(state) { mutableMapOf() }[action] = newQ
        Log.i("ENVYLOGS","QTABLE LOCAL UPDATED for $state mapping to $action with Qvalue $newQ")
    }

    fun saveQTableToFirestore(agentId: String) {
        val db = FirebaseFirestore.getInstance()
        val qTableDocRef = db.collection("QTables").document("qTable")

        // Fetch the existing Q-table from Firestore before saving
        qTableDocRef.get().addOnSuccessListener { doc ->
            val existingQTableData = if (doc.exists()) {
                (doc.get("qTableData") as? List<Map<String, Any>>) ?: emptyList()
            } else {
                emptyList<Map<String, Any>>()
            }

            // Convert existing Firestore data to a MutableMap for merging
            val existingQTable: MutableMap<Int, MutableMap<String, Double>> = existingQTableData.groupBy { entry ->
                (entry["state"] as? Number)?.toInt()
                    ?: throw IllegalArgumentException("State value must be a number")
            }.mapValues { (_, entries) ->
                entries.associate { entry ->
                    val action = entry["action"] as? String ?: throw IllegalArgumentException("Action must be a string")
                    val qValue = entry["qValue"] as? Double ?: throw IllegalArgumentException("QValue must be a double")
                    action to qValue
                }.toMutableMap()
            }.toMutableMap()

            // Merge the existing Q-table data with the local Q-table
            qTable.forEach { (state, actionsMap) ->
                actionsMap.forEach { (action, qValue) ->
                    val existingQValue = existingQTable[state]?.get(action) ?: 0.0
                    val updatedQValue = existingQValue + qValue
                    existingQTable.computeIfAbsent(state) { mutableMapOf() }[action] = updatedQValue
                }
            }

            // Convert the updated Q-table to the Firestore-compatible format
            val mergedQTableData = existingQTable.flatMap { (state, actionsMap) ->
                actionsMap.map { (action, qValue) ->
                    mapOf("state" to state, "action" to action, "qValue" to qValue)
                }
            }

            // Save the merged Q-table back to Firestore
            qTableDocRef.set(mapOf("qTableData" to mergedQTableData))
                .addOnSuccessListener {
                    Log.i("ENVYLOGS", "Firestore QTable merged and updated with ${mergedQTableData.size} entries")
                }
                .addOnFailureListener { e ->
                    Log.e("ENVYLOGS", "Failed to upload QTable to Firestore: ${e.message}")
                }

        }.addOnFailureListener { e ->
            Log.e("ENVYLOGS", "Failed to fetch QTable from Firestore: ${e.message}")
        }
    }



    suspend fun loadQTableFromFirestore(agentId: String): MutableMap<Int, MutableMap<String, Double>> {
        val db = FirebaseFirestore.getInstance()
        Log.i("ENVYLOGS", "Starting to load QTable for Agent ID: $agentId")

        try {
            val doc = db.collection("QTables")
                .document("qTable")
                .get()
                .await()

            if (!doc.exists()) {
                Log.e("ENVYLOGS", "No QTable found in Firestore")
                return mutableMapOf() // Return an empty map if the document does not exist.
            }

            val qTableData = doc.get("qTableData") as? List<Map<String, Any>>
                ?: run {
                    Log.e("ENVYLOGS", "Failed to cast data to expected List<Map<String, Any>> type or data is null.")
                    return mutableMapOf() // Return an empty map if casting fails or data is null.
                }

            Log.i("ENVYLOGS", "QTable fetched from Firestore: $qTableData")

            // Transform the fetched data into the required format
            return qTableData.groupBy { entry ->
                (entry["state"] as? Number)?.toInt()
                    ?: throw IllegalArgumentException("State value must be a number")
            }.mapValues { (_, entries) ->
                entries.associate { entry ->
                    val action = entry["action"] as? String ?: throw IllegalArgumentException("Action must be a string")
                    val qValue = entry["qValue"] as? Double ?: throw IllegalArgumentException("QValue must be a double")
                    action to qValue
                }.toMutableMap()
            }.toMutableMap()

        } catch (e: Exception) {
            Log.e("ENVYLOGS", "Exception occurred while loading QTable: ${e.message}")
            throw e // Rethrow the exception after logging it to handle it further up the call stack if necessary.
        }
    }


}

// Utility object to validate actions and provide random action selection
object Action {
    const val GENERAL = "general"
    const val TECHNOLOGY = "technology"
    const val SPORTS = "sports"
    const val ENTERTAINMENT = "entertainment"
    const val BUSINESS = "business"
    const val HEALTH = "health"
    const val SCIENCE = "science"

    private val allActions = listOf(
        GENERAL, TECHNOLOGY, SPORTS, ENTERTAINMENT, BUSINESS, HEALTH, SCIENCE
    )

    fun getAllActions(): List<String> = allActions

    fun getRandomAction(): String = allActions.random()

    fun isValidAction(action: String): Boolean = action in allActions
}

// Interface to communicate between fragments and activities
interface OnActionExecutedListener {
    fun onActionExecuted(reward: Double, state: Int, currentArticleCategory: String)
}
