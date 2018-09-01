package eu.depau.etchdroid.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View

import androidx.recyclerview.widget.RecyclerView

class EmptyRecyclerView : RecyclerView {
    var emptyView: View? = null
        set(value) {
            field = value
            checkIfEmpty()
        }

    private val observer: RecyclerView.AdapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            checkIfEmpty()
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    internal fun checkIfEmpty() {
        if (emptyView != null)
            emptyView!!.visibility = if (adapter?.itemCount ?: 0 > 0) View.GONE else View.VISIBLE
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        val oldAdapter = this.adapter
        oldAdapter?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        checkIfEmpty()
    }

}
