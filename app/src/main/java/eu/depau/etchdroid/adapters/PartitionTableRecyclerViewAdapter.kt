package eu.depau.etchdroid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import eu.depau.etchdroid.R
import eu.depau.etchdroid.kotlinexts.toHRSize
import eu.depau.etchdroid.utils.Partition
import kotlinx.android.synthetic.main.part_data_keyvalue.view.*
import kotlinx.android.synthetic.main.partition_row.view.*

class PartitionTableRecyclerViewAdapter(private val dataset: List<Partition>) : RecyclerView.Adapter<PartitionTableRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder {

        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.partition_row, parent, false) as LinearLayout
        return ViewHolder(layout)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val part = dataset[position]
        val layout = holder.layout
        val li = LayoutInflater.from(layout.context)
        var kv: LinearLayout

        layout.part_number.text = "${part.number}"

        layout.part_data_grid.removeAllViewsInLayout()

        if (part.partLabel != null) {
            kv = li.inflate(R.layout.part_data_keyvalue, layout.part_data_grid, false) as LinearLayout
            kv.key.text = layout.context.getString(R.string.part_label)
            kv.value.text = part.partLabel
            layout.part_data_grid.addView(kv)
        }
        if (part.fsLabel != null) {
            kv = li.inflate(R.layout.part_data_keyvalue, layout.part_data_grid, false) as LinearLayout
            kv.key.text = layout.context.getString(R.string.fs_label)
            kv.value.text = part.fsLabel
            layout.part_data_grid.addView(kv)
        }
        if (part.fsType != null) {
            kv = li.inflate(R.layout.part_data_keyvalue, layout.part_data_grid, false) as LinearLayout
            kv.key.text = layout.context.getString(R.string.fs_type)
            kv.value.text = part.fsType.getString(layout.context)
            layout.part_data_grid.addView(kv)
        }
        if (part.size != null) {
            kv = li.inflate(R.layout.part_data_keyvalue, layout.part_data_grid, false) as LinearLayout
            kv.key.text = layout.context.getString(R.string.part_size)
            kv.value.text = part.size.toHRSize()
            layout.part_data_grid.addView(kv)
        }
    }


    override fun getItemCount(): Int = dataset.size


    fun get(position: Int): Partition {
        return dataset[position]
    }
}