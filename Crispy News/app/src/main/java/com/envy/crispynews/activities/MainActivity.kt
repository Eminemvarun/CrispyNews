package com.envy.crispynews.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.envy.crispynews.FirebaseRepository
import com.envy.crispynews.utils.MainViewPagerAdapter
import com.envy.crispynews.OnActionExecutedListener
import com.envy.crispynews.QLearningAgent
import com.envy.crispynews.R
import com.envy.crispynews.SharedViewModel
import com.envy.crispynews.utils.VerticalDepthPageTransformer
import com.envy.crispynews.models.NewsArticle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


//Main activity to display news and help user navigate to different sections
class MainActivity : AppCompatActivity(), OnActionExecutedListener {

    private lateinit var mainViewPagerAdapter: MainViewPagerAdapter
    private val newsArticles = mutableListOf<NewsArticle>()
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingTV : TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var qLearningAgent: QLearningAgent
    private lateinit var myRepository : FirebaseRepository
    private val userId = FirebaseAuth.getInstance().uid.toString()
    private lateinit var sharedViewModel: SharedViewModel
    private var initialState: Int = 0
    private val selectedActions = mutableListOf<String>()
    private val REQUEST_EXIT = 17

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize Shared ViewModel and Qlearning agent

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        qLearningAgent = sharedViewModel.qLearningAgent
        myRepository = sharedViewModel.firebaseRepository

        // Initialize Widgets and ViewPager
        progressBar = findViewById(R.id.main_progressBar)
        loadingTV = findViewById(R.id.loading)
        mainViewPagerAdapter = MainViewPagerAdapter(this, newsArticles)
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = mainViewPagerAdapter
        viewPager.setPageTransformer(VerticalDepthPageTransformer())
        progressBar.visibility = View.VISIBLE

        // Toolbar and Profile Button Setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setLogo(R.mipmap.crispy_icon)
        setSupportActionBar(toolbar)
        val profileIcon: ImageView = findViewById(R.id.profile_icon)
        profileIcon.setOnClickListener {
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            startActivityForResult(intent, REQUEST_EXIT);
            overridePendingTransition(com.firebase.ui.auth.R.anim.fui_slide_in_right, com.firebase.ui.auth.R.anim.fui_slide_out_left)
        }

    }

    override fun onStart() {
        super.onStart()
        // Load user interests and Q-table
        if(newsArticles.size == 0){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                qLearningAgent.loadQTableFromFirestore(userId)
                val userInterests = myRepository.fetchUserInterests(userId)
                if (userInterests != null) {
                    myRepository.getInterests()
                    Log.i("ENVYLOG","User interest found as $userInterests")
                    fetchAndDisplayArticles()
                } else {
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@MainActivity, EnterInterests::class.java))
                        overridePendingTransition(
                            androidx.appcompat.R.anim.abc_grow_fade_in_from_bottom,
                            androidx.appcompat.R.anim.abc_fade_out)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error loading data!", Toast.LENGTH_SHORT).show()
                    Log.e("ENVYLOGS", e.message.toString())
                }
            }
        }
        }
    }

    //Initial method to load fetched articles and show them to the user
    private suspend fun fetchAndDisplayArticles() {
            try {
                val articles = fetchArticlesBasedOnQlearning(userId)

                withContext(Dispatchers.Main) {
                    newsArticles.clear()
                    newsArticles.addAll(articles)
                    mainViewPagerAdapter.notifyItemRangeInserted(0,newsArticles.size)
                    progressBar.visibility = View.GONE
                    loadingTV.visibility = View.GONE
                    animateFirstFragment()
                }

                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        if (position == newsArticles.size - 1) {
                            loadMoreArticles()
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ENVYLOGS", e.message.toString())
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error fetching articles!", Toast.LENGTH_SHORT).show()
                }
            }
    }


    // Function which uses qlearning agent to decide which article to display and then fetches
    // and returns those recommended articles

    private suspend fun fetchArticlesBasedOnQlearning(userId: String): List<NewsArticle> {
        selectedActions.clear()
        val state = myRepository.getCurrentState(userId) as Int // Assuming getCurrentState returns an Int state
        initialState = state
        val selectedCategories = qLearningAgent.chooseActions(state, numActions = 5)
        selectedActions.addAll(selectedCategories)
        val selectedArticles = mutableListOf<NewsArticle>()
        for (selectedCategory in selectedCategories) {
            val interestLevel = myRepository.getCurrentInterestsMap(userId)?.get(selectedCategory) ?: 0.0
            val numArticles = when {
                interestLevel <= 0.3 -> 5
                interestLevel <= 0.6 -> 10
                else -> 15
            }

            val articles = myRepository.fetchArticlesForCategory(selectedCategory, numArticles)
            selectedArticles.addAll(articles)
        }
        selectedArticles.shuffle()
        selectedArticles.sortByDescending { it.published_at }
        Log.i("ENVYLOGS","Loaded ${selectedArticles.size} articles")

        return selectedArticles
    }


    //Function which loads more articles as user swipes to the bottom
    private fun loadMoreArticles() {
        Toast.makeText(this, "Loading More Articles", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                qLearningAgent.saveQTableToFirestore(userId)
                myRepository.updateUserInterestsToFirestore()
                val moreArticles = fetchArticlesBasedOnQlearning(userId)

                withContext(Dispatchers.Main) {
                    val previous = newsArticles.size
                    newsArticles.addAll(moreArticles)
                    mainViewPagerAdapter.notifyItemRangeInserted(previous,newsArticles.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error loading more articles!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        myRepository.updateUserInterestsToFirestore()
        qLearningAgent.saveQTableToFirestore(userId)
    }

    //Update local Qtable and interests after every fragment is destroyed
    override fun onActionExecuted(reward: Double, nextstate: Int, currentArticleCategory: String) {
        if(selectedActions.isNotEmpty()) {
            qLearningAgent.updateQValueinTable(
                initialState,
                selectedActions.last(),
                reward,
                nextstate
            )
        }
        initialState = nextstate // Update initial state to the new state after action execution
        myRepository.updateInterest(currentArticleCategory,reward)
    }

    //Function which destroys activity if user has logged out
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                finish()
            }
        }
    }

    //Code which animates a swipe up once articles are loaded
    private fun animateFirstFragment() {
        val firstPage = viewPager.getChildAt(0) // Get the first page view
        firstPage?.let { page ->
            page.translationY = page.height * 1.5f // Start below the screen

            // Animate it with slide up
            page.animate()
                .translationY(0f)
                .setDuration(800)
                .start()
        }
    }

}

