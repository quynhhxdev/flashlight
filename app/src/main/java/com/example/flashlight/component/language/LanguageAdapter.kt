package com.example.flashlight.component.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.flashlight.data.model.Language
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ItemLanguageBinding

class LanguageAdapter(
    private var languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedCode: String? = null

    fun submitList(list: List<Language>, currentCode: String) {
        languages = list
        selectedCode = currentCode.ifEmpty { null }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(language: Language) {
            val isSelected = selectedCode != null && language.languageCode == selectedCode

            binding.imgFlag.setImageResource(language.iconRes)
            binding.tvLanguageName.text = language.nativeName.ifEmpty {
                binding.root.context.getString(language.nameRes)
            }

            if (isSelected) {
                binding.ivSelection.setImageResource(R.drawable.ic_check)
                binding.tvLanguageName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.bg_yellow_switch)
                )
            } else {
                binding.itemContainer.setBackgroundResource(R.drawable.bg_language)
                binding.ivSelection.setImageResource(0)
                binding.tvLanguageName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            }

            binding.root.setOnClickListener {
                // Guard: Do not re-select or trigger ad replacement when clicking already-selected language
                if (selectedCode != null && selectedCode == language.languageCode) {
                    return@setOnClickListener
                }
                selectedCode = language.languageCode
                notifyDataSetChanged()
                onLanguageSelected(language)
            }
        }
    }
}
