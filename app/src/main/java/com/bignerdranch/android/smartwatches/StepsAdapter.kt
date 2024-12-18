package com.bignerdranch.android.smartwatches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StepsAdapter(private val stepsList: List<Pair<String, Int>>) : RecyclerView.Adapter<StepsAdapter.StepsViewHolder>() {

    class StepsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        val stepsTextView: TextView = itemView.findViewById(R.id.steps_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step, parent, false)
        return StepsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {
        val (date, steps) = stepsList[position]
        holder.dateTextView.text = date
        holder.stepsTextView.text = steps.toString()
    }

    override fun getItemCount() = stepsList.size
}