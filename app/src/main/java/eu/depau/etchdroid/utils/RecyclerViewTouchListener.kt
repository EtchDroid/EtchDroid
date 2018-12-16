package eu.depau.etchdroid.utils

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewTouchListener(context: Context, val recyclerView: RecyclerView, val clickListener: ClickListener) : RecyclerView.OnItemTouchListener {
    private var gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean = true

            override fun onLongPress(e: MotionEvent) {
                val child = recyclerView.findChildViewUnder(e.x, e.y)
                if (child != null)
                    clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child))
            }
        })
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val child = rv.findChildViewUnder(e.x, e.y)
        if (child != null && gestureDetector.onTouchEvent(e))
            clickListener.onClick(child, rv.getChildAdapterPosition(child))
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}