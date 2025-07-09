package com.example.crdisease.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.crdisease.model.DiseaseRecord
import java.util.Date
import com.example.crdisease.R


class HistoryAdapter(private val records: MutableList<DiseaseRecord>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val diseaseText: TextView = itemView.findViewById(R.id.diseaseText)
        val recommendationText: TextView = itemView.findViewById(R.id.recommendationText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_disease_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.diseaseText.text = "Disease: ${record.disease}"
        holder.recommendationText.text = "Recommendation: ${record.recommendation}"
        holder.timestampText.text = "Time: ${Date(record.timestamp)}"
    }
    fun getRecordAt(position: Int): DiseaseRecord = records[position]



    fun removeRecordAt(position: Int) {
        records.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateRecords(newRecords: List<DiseaseRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }



}
