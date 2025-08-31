package com.example.algoquest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.algoquest.R
import com.example.algoquest.model.Problem

class RecommendationAdapter(
    private val problems: List<Problem>,
    private val onClick: (Problem) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.recTitle)
        val points: TextView = itemView.findViewById(R.id.recPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.items_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = problems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val problem = problems[position]
        holder.title.text = problem.title
        holder.points.text = "Points: ${problem.points}"

        holder.itemView.setOnClickListener { onClick(problem) }
    }
}
