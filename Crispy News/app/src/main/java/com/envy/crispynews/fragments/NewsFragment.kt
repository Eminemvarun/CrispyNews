package com.envy.crispynews.fragments

import com.envy.crispynews.FirebaseRepository
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.envy.crispynews.SharedViewModel
import com.envy.crispynews.OnActionExecutedListener
import com.envy.crispynews.QLearningAgent
import com.envy.crispynews.R
import com.envy.crispynews.databinding.FragmentNewsBinding
import com.envy.crispynews.models.NewsArticle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.sql.Timestamp

class NewsFragment : Fragment() {

    //Variables
    private var like: Boolean = false
    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    private var startTime: Long = 0
    private lateinit var  qLearningAgent: QLearningAgent
    private lateinit var sharedViewModel: SharedViewModel
    private var listener: OnActionExecutedListener? = null
    private lateinit var currentArticleCategory: String // To be set based on article category
    private var endTime: Long = 0
    private lateinit var myRepository : FirebaseRepository
    private var fragmentPosition: Int = -1
    private var reward : Double = 0.0

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_IMAGE = "image"
        private const val ARG_URL = "url"
        private const val ARG_DATESTRING = "dateString"
        private const val ARG_CATEGORY = "category"
        private const val ARG_AUTHOR = "author"
        private const val ARG_TIMESTAMP = "published_at"
        private const val ARG_POPULARITY = "popularity"
        private const val ARG_POSITION = "position"


        fun newInstance(article: NewsArticle, position: Int): NewsFragment {
            val fragment = NewsFragment()
            val args = Bundle().apply {
                putString(ARG_TITLE, article.title)
                putString(ARG_DESCRIPTION, article.description)
                putString(ARG_IMAGE, article.image)
                putString(ARG_URL, article.url)
                putString(ARG_DATESTRING,
                    DateUtils
                        .getRelativeTimeSpanString(article.published_at?.time ?: DateUtils.DAY_IN_MILLIS )
                        .toString())
                putString(ARG_CATEGORY,article.category)
                putString(ARG_AUTHOR,article.source)
                putLong(ARG_POPULARITY,article.popularity)
                putLong(ARG_TIMESTAMP, article.published_at?.time ?: 0L)
                putInt(ARG_POSITION, position)
            }
            fragment.arguments = args
            fragment.currentArticleCategory = article.category
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        startTime = System.currentTimeMillis()
        currentArticleCategory = arguments?.getString(ARG_CATEGORY).toString()
        reward =0.0
    }

    override fun onStop() {
        super.onStop()
        endTime = System.currentTimeMillis()
        val timeSpent = endTime - startTime
        if(timeSpent <3000){
            reward -=0.01
        }
        // Notify the main activity
        val currentState = myRepository.getCurrentState(FirebaseAuth.getInstance().uid.toString()) ?:0
        listener?.onActionExecuted(reward, currentState,currentArticleCategory)
        reward = 0.0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        myRepository = sharedViewModel.firebaseRepository
        qLearningAgent =  sharedViewModel.qLearningAgent

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentPosition = arguments?.getInt(ARG_POSITION, -1) ?: -1
        arguments?.let {
            //Everything
            binding.titleTextView.text = it.getString(ARG_TITLE)
            binding.descriptionTextView.text = it.getString(ARG_DESCRIPTION)
            val imageUrl = it.getString(ARG_IMAGE)
            binding.fragmentNewsDatetime.text = it.getString(ARG_DATESTRING)
            binding.fragmentNewsAuthor.text =it.getString(ARG_AUTHOR)
            binding.fragmentNewsCategory.text = String.format("Category: %s", it.getString(ARG_CATEGORY))
            currentArticleCategory = it.getString(ARG_CATEGORY, "")
            val urlToArticle = it.getString(ARG_URL)?.toUri() // Get the URL a // nd convert it to Uri
            //Image
            Glide.with(this@NewsFragment)
                .load(imageUrl)
                // .placeholder(R.drawable.placeholder)  // Optional
                .error(R.drawable.placeholder)              // Optional
                .into(binding.imageView)
            //Read More Button
            binding.fragmentNewsReadMoreBTN.setOnClickListener {
                reward += 0.05
                urlToArticle?.let {
                    CustomTabsIntent.Builder()
                        .build()
                        .launchUrl(requireContext(), urlToArticle) // Pass context and url as Uri
                }
            }

            val title = it.getString(ARG_TITLE)
            val timestampMillis = it.getLong(ARG_TIMESTAMP)
            val timestamp = Timestamp(timestampMillis)
            val popularity = it.getLong(ARG_POPULARITY)
            val source = it.getString(ARG_AUTHOR)
            val docid  = "${title}_${timestamp.time}_$source"
            val newsRef = FirebaseFirestore.getInstance().collection("news").document(docid)

            // LIKE BUTTON LISTENER!!!!
            binding.fragmentNewsLikeBTN.setOnClickListener {
                if(!this.like) {
                    binding.fragmentNewsLikeBTN.setCompoundDrawablesWithIntrinsicBounds(R.drawable.thumbs_up_filled_24,0,0,0)
                    val animation = AnimationUtils.loadAnimation(context, androidx.appcompat.R.anim.abc_grow_fade_in_from_bottom)
                    binding.fragmentNewsLikeBTN.startAnimation(animation)
                    reward +=0.075
                    newsRef
                        .update(ARG_POPULARITY, popularity + 1)
                        .addOnSuccessListener {
                            Log.i("ENVYLOGS","Liked! updated to $popularity +1")
                        }
                    this.like=true
                }
            }
            newsRef.addSnapshotListener{ snapshot, error ->
                if(error !=null){
                    error.printStackTrace()
                }else {
                    val popularityFetched = snapshot?.getLong(ARG_POPULARITY) ?: 0
                    binding.fragmentNewsLikeBTN.text = getString(R.string.likes_count, popularityFetched)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnActionExecutedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnActionExecutedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}

