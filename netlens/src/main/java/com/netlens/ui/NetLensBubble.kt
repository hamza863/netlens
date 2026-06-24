package com.netlens.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * A small, draggable floating button attached to an activity's content view.
 * Tapping it opens the NetLens viewer — handy on emulators where shaking is
 * awkward. Uses no system-overlay permission: it lives inside the host window.
 *
 * Enable via `NetLensConfig(showBubble = true)`.
 */
@SuppressLint("ClickableViewAccessibility")
internal class NetLensBubble(
    private val activity: Activity,
    private val onClick: () -> Unit
) {
    private var view: View? = null

    fun attach() {
        if (view != null) return
        val root = activity.window?.decorView
            ?.findViewById<ViewGroup>(android.R.id.content) ?: return

        val density = activity.resources.displayMetrics.density
        val sizePx = (52 * density).toInt()

        val bubble = TextView(activity).apply {
            text = "🔍"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1F2937"))
            }
            elevation = 8 * density
        }

        val params = FrameLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            marginEnd = (16 * density).toInt()
        }

        var downX = 0f
        var downY = 0f
        var startTx = 0f
        var startTy = 0f
        var dragging = false
        val slop = 12 * density

        bubble.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startTx = v.translationX; startTy = v.translationY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragging = true
                    v.translationX = startTx + dx
                    v.translationY = startTy + dy
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) v.performClick()
                    true
                }
                else -> false
            }
        }
        bubble.setOnClickListener { onClick() }

        root.addView(bubble, params)
        view = bubble
    }

    fun detach() {
        (view?.parent as? ViewGroup)?.removeView(view)
        view = null
    }
}
