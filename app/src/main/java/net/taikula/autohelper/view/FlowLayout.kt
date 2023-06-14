package net.taikula.autohelper.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import net.taikula.autohelper.tools.Extensions.TAG


/**
 * [流式布局](https://blog.csdn.net/zxt0601/article/details/50533658)
 *
 * 优化代码，支持每行元素高度不同
 */
class FlowLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    //在onMeasure里，测量所有子View的宽高，以及确定ViewGroup自己的宽高。
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 获取系统传递过来测量出的宽度 高度，以及相应的测量模式。
        // 如果测量模式为 EXACTLY(确定的dp值，match_parent)，则可以调用setMeasuredDimension()设置，
        // 如果测量模式为 AT_MOST(wrap_content)，则需要经过计算再去调用setMeasuredDimension()设置
        val widthMeasure = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMeasure = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        // 计算宽度 高度 //wrap_content测量模式下会使用到:
        // 存储最后计算出的宽度，
        var maxLineWidth = 0
        // 存储最后计算出的高度
        var totalHeight = 0
        // 存储当前行的宽度
        var curLineWidth = 0
        // 存储当前行的高度
        var curLineHeight = 0

        //遍历子View 计算父控件宽高
        for (i in 0 until childCount) {
            val child = getChildAt(i)

            if (GONE == child.visibility) {
                continue
            }

            // 先测量子View
            measureChild(child, widthMeasureSpec, heightMeasureSpec)

            //获取子View的LayoutParams，(子View的LayoutParams的对象类型，取决于其ViewGroup的generateLayoutParams()方法的返回的对象类型，这里返回的是MarginLayoutParams)
            val childLayoutParams = child.layoutParams as MarginLayoutParams

            //子View Layout需要的宽高(包含margin)，用于计算是否越界
            val childWidth =
                child.measuredWidth + childLayoutParams.leftMargin + childLayoutParams.rightMargin
            val childHeight =
                child.measuredHeight + childLayoutParams.topMargin + childLayoutParams.bottomMargin
            Log.i(
                TAG,
                "子View Layout需要的宽高(包含margin)：childWidth=$childWidth, childHeight=$childHeight"
            )

            //如果当前的行宽度大于 父控件允许的最大宽度 则要换行
            //父控件允许的最大宽度 如果要适配 padding 这里要- getPaddingLeft() - getPaddingRight()
            //即为测量出的宽度减去父控件的左右边距
            if (curLineWidth + childWidth > widthMeasure - paddingLeft - paddingRight) {
                //通过比较 当前行宽 和以前存储的最大行宽,得到最新的最大行宽,用于设置父控件的宽度
                maxLineWidth = Math.max(maxLineWidth, curLineWidth)
                //父控件的高度增加了，为当前高度+当前行的高度
                totalHeight += curLineHeight
                //换行后 刷新 当前行 宽高数据： 因为新的一行就这一个View，所以为当前这个view占用的宽高(要加上View 的 margin)
                curLineWidth = childWidth
                curLineHeight = childHeight
            } else {
                //不换行：叠加当前行宽 和 比较当前行高:
                curLineWidth += childWidth
                curLineHeight = Math.max(curLineHeight, childHeight)
            }

            //如果已经是最后一个View,要比较当前行的 宽度和最大宽度，叠加一共的高度
            if (i == childCount - 1) {
                maxLineWidth = Math.max(maxLineWidth, curLineWidth)
                totalHeight += childHeight
            }
        }
        Log.i(
            TAG,
            "系统测量允许的尺寸最大值：widthMeasure=$widthMeasure, heightMeasure=$heightMeasure"
        )
        Log.i(
            TAG,
            "经过我们测量实际的尺寸(不包括父控件的padding)：maxLineWidth=$maxLineWidth,totalHeight=$totalHeight"
        )

        // 适配padding,如果是wrap_content,则除了子控件本身占据的控件，还要在加上父控件的padding
        setMeasuredDimension(
            if (widthMode != MeasureSpec.EXACTLY) maxLineWidth + paddingLeft + paddingRight else widthMeasure,
            if (heightMode != MeasureSpec.EXACTLY) totalHeight + paddingTop + paddingBottom else heightMeasure
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.i(TAG, "changed:$changed   ,l:$l  t:$t  r:$r  b:$b")

        // ViewParent 的右边x的布局限制值
        val rightLimit = width - paddingRight

        // 存储基准的left top (子类.layout(),里的坐标是基于父控件的坐标，所以 x应该是从0+父控件左内边距开始，y从0+父控件上内边距开始)
        val baseLeft = 0 + paddingLeft
        val baseTop = 0 + paddingTop

        // 存储现在布局位置的left top
        var curLeft = baseLeft
        var curTop = baseTop

        // 子View
        // 子view用于layout的 l t r b
        var viewL: Int
        var viewT: Int
        var viewR: Int
        var viewB: Int

        //临时增加一个temp 存储上一个View的高度 解决过长的两行View导致显示不正确的bug
        var lastLineHeight = 0
        var curLineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (GONE == child.visibility) {
                continue
            }

            //获取子View的LayoutParams，用于获取其margin
            val childLayoutParams = child.layoutParams as MarginLayoutParams
            //子View需要的宽高 为 本身宽高+marginLeft + marginRight，用于计算是否越界
            val childTotalWidth =
                child.measuredWidth + childLayoutParams.leftMargin + childLayoutParams.rightMargin
            val childTotalHeight =
                child.measuredHeight + childLayoutParams.topMargin + childLayoutParams.bottomMargin

            //这里要考虑padding，所以右边界为 ViewParent宽度(包含padding)-ViewParent右内边距
            if (curLeft + childTotalWidth > rightLimit) {
                //如果当前行已经放不下该子View了 需要换行放置：
                //在新的一行布局子View，左x就是baseLeft，上y是 top +前一行高(这里假设的是每一行行高一样)，
                lastLineHeight = curLineHeight
                curLineHeight = childTotalHeight

                curTop += lastLineHeight
                //layout时要考虑margin
                viewL = baseLeft + childLayoutParams.leftMargin
                viewT = curTop + childLayoutParams.topMargin
                viewR = viewL + child.measuredWidth
                viewB = viewT + child.measuredHeight
                //child.layout(baseLeft + params.leftMargin, curTop + params.topMargin, baseLeft + params.leftMargin + child.getMeasuredWidth(), curTop + params.topMargin + child.getMeasuredHeight());
                //Log.i(TAG,"新的一行:" +"   ,baseLeft:"+baseLeft +"  curTop:"+curTop+"  baseLeft+childWidth:"+(baseLeft+childWidth)+"  curTop+childHeight:"+ ( curTop+childHeight));
                curLeft = baseLeft + childTotalWidth
            } else {
                //当前行可以放下子View:
                viewL = curLeft + childLayoutParams.leftMargin
                viewT = curTop + childLayoutParams.topMargin
                viewR = viewL + child.measuredWidth
                viewB = viewT + child.measuredHeight

                curLineHeight = Math.max(curLineHeight, childTotalHeight)

                //child.layout(curLeft + params.leftMargin, curTop + params.topMargin, curLeft + params.leftMargin + child.getMeasuredWidth(), curTop + params.topMargin + child.getMeasuredHeight());
                //Log.i(TAG,"当前行:"+changed +"   ,curLeft:"+curLeft +"  curTop:"+curTop+"  curLeft+childWidth:"+(curLeft+childWidth)+"  curTop+childHeight:"+(curTop+childHeight));
                curLeft += childTotalWidth
            }

            // 布局子View
            child.layout(viewL, viewT, viewR, viewB)
        }
    }

    /**
     * @return 当前ViewGroup返回的Params的类型
     */
    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}