package com.godwin.nyumbanilauncher.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Wraps GestureDetector to expose simple onSwipeUp / onSwipeDown callbacks
 * for the home screen. Attach via a View's setOnTouchListener.
 */
class GestureHelper(
    context: Context,
    private val onSwipeUp: () -> Unit,
    private val onSwipeDown: () -> Unit,
    private val onDoubleTap: () -> Unit = {},
    private val onLongPress: () -> Unit = {}
) {
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffY) > abs(diffX) && abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY < 0) onSwipeUp() else onSwipeDown()
                return true
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongPress()
        }

        override fun onDown(e: MotionEvent): Boolean = true
    })

    fun onTouchEvent(event: MotionEvent): Boolean = detector.onTouchEvent(event)

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}
