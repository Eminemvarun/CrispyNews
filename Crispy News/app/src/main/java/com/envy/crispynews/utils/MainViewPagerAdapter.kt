package com.envy.crispynews.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.envy.crispynews.fragments.NewsFragment
import com.envy.crispynews.models.NewsArticle


//Viewpager adapter class which displays fragments,needs to be implemented for viewpager to work
class MainViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private var newsArticles: List<NewsArticle>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return newsArticles.size
    }

    override fun createFragment(position: Int): Fragment {
        return NewsFragment.newInstance(newsArticles[position],position)
    }

    fun setNewsArticles(newsArticles: List<NewsArticle>) {
        this.newsArticles = newsArticles
    }

}
