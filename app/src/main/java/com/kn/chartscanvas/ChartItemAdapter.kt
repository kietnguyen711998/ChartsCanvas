package com.kn.chartscanvas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ChartItemAdapter(
    context: Context,
    chartItems: ArrayList<Item>
) :
    RecyclerView.Adapter<ChartViewHolder>() {
    var chartItems: ArrayList<Item> = chartItems
    var context: Context = context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chart, parent, false)
        return ChartViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        holder.cardViewColor.setBackgroundColor(chartItems[position].color)
        holder.txtLabel.text = chartItems[position].label
        holder.txtValue.text = "" + chartItems[position].value
    }

    override fun getItemCount(): Int {
        return chartItems.size
    }

}

class ChartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var cardViewColor: Button = itemView.findViewById(R.id.cardviewItemColor)
    var txtLabel: TextView = itemView.findViewById(R.id.txtItemLabel)
    var txtValue: TextView = itemView.findViewById(R.id.txtItemValue)

}