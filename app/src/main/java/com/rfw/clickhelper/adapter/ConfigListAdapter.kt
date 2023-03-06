package com.rfw.clickhelper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rfw.clickhelper.R
import com.rfw.clickhelper.data.db.entity.ClickData

/**
 * https://developer.android.com/codelabs/android-room-with-a-view-kotlin?hl=zh-cn#11
 */
class ConfigListAdapter :
    ListAdapter<ClickData, ConfigListAdapter.ConfigViewHolder>(ClickDataComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    class ConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val indexTv: TextView = itemView.findViewById(R.id.tv_item_index)
        private val doodleImage: ImageView = itemView.findViewById(R.id.iv_click_area)

        fun bind(text: String?) {
            indexTv.text = text
        }

        companion object {
            fun create(parent: ViewGroup): ConfigViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.config_item_view, parent, false)
                return ConfigViewHolder(view)
            }
        }
    }

    class ClickDataComparator : DiffUtil.ItemCallback<ClickData>() {
        override fun areItemsTheSame(oldItem: ClickData, newItem: ClickData): Boolean {
            return oldItem.id == newItem.id && oldItem.configId == newItem.configId
        }

        override fun areContentsTheSame(oldItem: ClickData, newItem: ClickData): Boolean {
            return oldItem.id == newItem.id && oldItem.clickArea == newItem.clickArea
        }
    }
}

