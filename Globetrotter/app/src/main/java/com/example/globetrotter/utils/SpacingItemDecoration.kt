package com.example.globetrotter.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// Reference from Chat-GPT for horizontal spacing in horizontal recycling view
class HorizontalSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        outRect.right = spacing

        // Ad spacing on the left of the first item
        if (position == 0)
            outRect.left = spacing
    }
}
// End reference