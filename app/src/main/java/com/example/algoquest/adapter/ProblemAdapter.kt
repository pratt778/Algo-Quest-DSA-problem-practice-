package com.example.algoquest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.algoquest.R
import com.example.algoquest.model.Problem

class ProblemAdapter(
    private var problems: List<Problem> = emptyList(),
    private val onClick: (Problem) -> Unit
) : RecyclerView.Adapter<ProblemAdapter.ProblemViewHolder>() {

    class ProblemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.problemTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ProblemViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_problem, parent, false))

    override fun getItemCount() = problems.size

    override fun onBindViewHolder(holder: ProblemViewHolder, position: Int) {
        val problem = problems[position]
        holder.titleText.text = problem.title
        holder.itemView.setOnClickListener { onClick(problem) }
    }

    fun updateData(newProblems: List<Problem>) {
        problems = newProblems
        notifyDataSetChanged()
    }
}
