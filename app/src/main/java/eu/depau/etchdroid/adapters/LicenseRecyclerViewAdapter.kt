package eu.depau.etchdroid.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.License
import kotlinx.android.synthetic.main.license_row.view.*


class LicenseRecyclerViewAdapter(private val dataset: Array<License>) : RecyclerView.Adapter<LicenseRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(val relLayout: RelativeLayout) : RecyclerView.ViewHolder(relLayout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder {

        val relLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.license_row, parent, false) as RelativeLayout
        return ViewHolder(relLayout)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val license = dataset[position]

        holder.relLayout.software_name.text = license.name
        holder.relLayout.software_desc.text = license.description
        holder.relLayout.software_license.text = license.license
        holder.relLayout.software_url.text = license.url.toString()

        holder.relLayout.software_desc.visibility = if (license.description == null) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = dataset.size

    fun get(position: Int): License {
        return dataset[position]
    }
}
