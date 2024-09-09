package com.envy.crispynews.utils

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.pow


private const val MIN_SCALE = 0.75f


class VerticalDepthPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the top.
                    alpha = 0f
                }
                position <= 0 -> { // [-1,0]
                    // Use the default slide transition when moving to the top page
                    alpha = 1f
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
                position <= 1 -> { // (0,1]
                    // Fade the page out.
                    alpha = 1 - position.pow(0.25f)

                    // Counteract the default slide transition
                    translationY = pageHeight * -position

                    // Scale the page down (between MIN_SCALE and 1)
                    val scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the bottom.
                    alpha = 0f
                }
            }
        }
    }
}