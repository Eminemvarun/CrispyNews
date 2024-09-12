package com.envy.crispynews

import androidx.lifecycle.ViewModel

//Shared viewmodel to help keeping a single copy of Qlearning agent and firebase repository and their data
// between the MainActivity and NewsFragments
class SharedViewModel : ViewModel() {
    val qLearningAgent = QLearningAgent(numStates = 8748, numActions = 7)
    val firebaseRepository = FirebaseRepository() // Initialize FirebaseRepository here
}