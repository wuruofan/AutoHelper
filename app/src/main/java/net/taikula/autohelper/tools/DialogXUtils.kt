package net.taikula.autohelper.tools

import android.util.Log
import android.view.View
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.InputDialog
import com.kongzue.dialogx.dialogs.MessageDialog
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener
import com.kongzue.dialogx.interfaces.OnInputDialogButtonClickListener
import net.taikula.autohelper.tools.Extensions.TAG

object DialogXUtils {

    private const val RADIUS_100 = 100f

    /**
     * 显示信息提示框，类似 toast
     */
    fun showPopTip(title: String, timeMs: Long = 1500) {
        PopTip.build()
            .setMessage(title)
            .setRadius(RADIUS_100)
            .setTheme(DialogX.THEME.AUTO)
            .autoDismiss(timeMs)
//            .setMessageTextInfo(TextInfo().apply { fontColor = context.resources.getColor(R.color.design_default_color_on_primary, context.theme) })
            .show()

    }

    /**
     * 显示消息提示对话框
     */
    fun showMessageDialog(
        title: String,
        message: String,
        onOkAction: () -> Unit = {},
        onCancelAction: () -> Unit = {}
    ) {
        MessageDialog.build()
            .setTitle(title)
            .setMessage(message)
            .setOkButton("确定") { dialog, v ->
                onOkAction.invoke()
                false
            }
            .setCancelButton("取消") { dialog, v ->
                onCancelAction.invoke()
                false
            }
            .setTheme(DialogX.THEME.AUTO)
            .setCancelable(true)
            .show()
    }

    /**
     * 显示文本编辑对话框
     *
     * @param onOkAction 返回 true 时对话框不消失
     */
    fun showInputDialog(
        title: String, text: String,
        onOkAction: (String) -> Boolean = { false },
        onCancelAction: () -> Unit = {}
    ) {
        InputDialog.build()
            .setTitle("重命名")
            .setInputText(text)
            .setOkButton("确定", object : OnInputDialogButtonClickListener<InputDialog> {
                override fun onClick(dialog: InputDialog?, v: View?, inputStr: String?): Boolean {
                    if (dialog == null) return false

                    return onOkAction.invoke(inputStr ?: "")
                }
            })
            .setCancelButton("取消", object : OnDialogButtonClickListener<MessageDialog> {
                override fun onClick(dialog: MessageDialog?, v: View?): Boolean {
                    Log.d(TAG, "click cancel")
                    onCancelAction.invoke()
                    return false
                }
            })
            .setTheme(DialogX.THEME.AUTO) //if (ThemeUtils.isDarkTheme(this)) DialogX.THEME.DARK else DialogX.THEME.LIGHT)
//            .setBkgInterceptTouch(false)
            .setCancelable(true)
            .show()

    }
}