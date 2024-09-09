package com.envy.crispynews.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.envy.crispynews.databinding.ActivityEnterInterestsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EnterInterests : AppCompatActivity() {
        private var interestExists : Boolean = false
        private lateinit var binding: ActivityEnterInterestsBinding
        private val db = FirebaseFirestore.getInstance()
        private val userId = FirebaseAuth.getInstance().currentUser?.uid

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityEnterInterestsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            loadInterestsFromFirestore()
            binding.buttonContinue.setOnClickListener {
                saveInterestsToFirestore()
            }
            binding.buttonSkip.setOnClickListener {
                if(!interestExists){
                    saveInterestsToFirestore()
                }
                finish()
            }
        }

    private fun loadInterestsFromFirestore() {
        if (userId != null) {
            db.collection("user").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.contains("interests")) {
                        val interests = document.get("interests") as? Map<String, Double>
                        interests?.let {
                            interestExists = true
                            Toast.makeText(this,"Loaded current interests", Toast.LENGTH_SHORT).show()
                            binding.businessSlider.value = denormalizeSliderValue(it["business"] as? Double ?: 0.5)
                            binding.entertainmentSlider.value = denormalizeSliderValue(it["entertainment"] as? Double ?: 0.5)
                            binding.healthSlider.value = denormalizeSliderValue(it["health"] as? Double ?: 0.5)
                            binding.scienceSlider.value = denormalizeSliderValue(it["science"] as? Double ?: 0.5)
                            binding.sportsSlider.value = denormalizeSliderValue(it["sports"] as? Double ?: 0.5)
                            binding.technologySlider.value = denormalizeSliderValue(it["technology"] as? Double ?: 0.5)
                            Log.i("ENVYLOG",binding.businessSlider.value.toString())
                        }
                    } else {
                        interestExists = false
                        Log.i("ENVYLOGS","No interests found in firestore!")
                        // Handle the case where no interests are found
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load interests", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in , can't fetch interests", Toast.LENGTH_SHORT).show()
        }
    }

    private fun denormalizeSliderValue(value: Double): Float {
        val returnval = roundToStepSize((value * 100).toFloat())
        Log.i("ENVYLOG","Returned value is $returnval")
        return returnval

    }

    private fun roundToStepSize(value: Float): Float {
        val stepSize = 10.0f // Define the step size
        return (Math.round(value / stepSize) * stepSize)
    }

    private fun saveInterestsToFirestore() {
        if (userId != null) {
            val userInterests: Map<String, Any> =
                mapOf(
                    "general" to 0.85,
                    "business" to normalizeSliderValue(binding.businessSlider.value),
                    "entertainment" to normalizeSliderValue(binding.entertainmentSlider.value),
                    "health" to normalizeSliderValue(binding.healthSlider.value),
                    "science" to normalizeSliderValue(binding.scienceSlider.value),
                    "sports" to normalizeSliderValue(binding.sportsSlider.value),
                    "technology" to normalizeSliderValue(binding.technologySlider.value)
                )

            // Set the user's interests in Firestore
            db.collection("user").document(userId)
            .set(mapOf("interests" to userInterests))
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        } else {
            Log.e("ENVYLOGS","User not logged in can't save interests!")
        }
    }

    private fun normalizeSliderValue(value: Float): Float {
            return value / 100
        }

}