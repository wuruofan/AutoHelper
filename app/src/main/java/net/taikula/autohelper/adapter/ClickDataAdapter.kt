package net.taikula.autohelper.adapter

import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.DraggableModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.taikula.autohelper.R
import net.taikula.autohelper.data.db.entity.ClickData
import net.taikula.autohelper.tools.DisplayUtils

/**
 * 点击助手配置相关 adapter
 */
class ClickDataAdapter : BaseQuickAdapter<ClickData, BaseViewHolder>(R.layout.config_item_view),
    DraggableModule {
    override fun convert(holder: BaseViewHolder, item: ClickData) {
        holder.setText(R.id.tv_item_index, item.sequence.toString())

        item.clickArea.run {
            if (bitmap != null) {
                Glide.with(holder.itemView).load(bitmap)
                    .apply(
                        RequestOptions.bitmapTransform(
                            RoundedCorners(
                                DisplayUtils.dip2px(
                                    holder.itemView.context,
                                    20f
                                )
                            )
                        )
                    )
                    .into(holder.getView(R.id.iv_click_area))
            } else {
                imagePath?.let {
                    Glide.with(holder.itemView).load(it)
                        .apply(
                            RequestOptions.bitmapTransform(
                                RoundedCorners(
                                    DisplayUtils.dip2px(
                                        holder.itemView.context,
                                        20f
                                    )
                                )
                            )
                        )
                        .into(holder.getView(R.id.iv_click_area))
                }
            }

            holder.setText(R.id.tv_config_desc, item.name)
        }
    }
}