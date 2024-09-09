package com.envy.crispynews

import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val qLearningAgent = QLearningAgent(numStates = 8748, numActions = 7)
    val firebaseRepository = FirebaseRepository() // Initialize FirebaseRepository here
}