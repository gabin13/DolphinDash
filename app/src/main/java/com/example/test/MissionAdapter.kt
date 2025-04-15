package com.example.test

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MissionAdapter(
    private val context: Context,
    private val missionList: List<Mission>,
    private val onClaimClick: (Mission) -> Unit
) : RecyclerView.Adapter<MissionAdapter.MissionViewHolder>() {

    class MissionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.missionTitle)
        val description: TextView = view.findViewById(R.id.missionDescription)
        val reward: TextView = view.findViewById(R.id.missionReward)
        val progressBar: ProgressBar = view.findViewById(R.id.missionProgressBar)
        val progressText: TextView = view.findViewById(R.id.missionProgressText)
        val image: ImageView = view.findViewById(R.id.missionImage)
        val claimButton: Button = view.findViewById(R.id.claimButton)
        val typeIndicator: TextView = view.findViewById(R.id.missionType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.mission_item, parent, false)
        return MissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
        val mission = missionList[position]

        // Configurer les textes
        holder.title.text = mission.title
        holder.description.text = mission.description
        holder.reward.text = "${mission.reward} coins"

        // Configurer le type de mission
        holder.typeIndicator.text = mission.type
        holder.typeIndicator.setBackgroundResource(
            if (mission.type == "Daily") R.drawable.badge_daily
            else R.drawable.badge_general
        )

        // Configurer la progression
        val progressPercent = mission.getProgressPercentage()
        holder.progressBar.progress = progressPercent
        holder.progressText.text = "${mission.currentProgress}/${mission.progressRequired}"

        // Charger l'image
        if (mission.imageURL.isNotEmpty()) {
            Glide.with(context).load(mission.imageURL).into(holder.image)
        } else {
            // Image par défaut si aucune URL n'est fournie
            holder.image.setImageResource(R.drawable.ic_mission_default)
        }

        // Configurer le bouton de réclamation
        if (mission.isComplete() && !mission.completed) {
            holder.claimButton.visibility = View.VISIBLE
            holder.claimButton.isEnabled = true
            holder.claimButton.text = "Réclamer"
            holder.claimButton.setOnClickListener {
                onClaimClick(mission)
            }
        } else if (mission.completed) {
            holder.claimButton.visibility = View.VISIBLE
            holder.claimButton.isEnabled = false
            holder.claimButton.text = "Réclamé"
        } else {
            holder.claimButton.visibility = View.VISIBLE
            holder.claimButton.isEnabled = false
            holder.claimButton.text = "En cours"
        }
    }

    override fun getItemCount(): Int = missionList.size
}