package com.rfw.clickhelper.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.rfw.clickhelper.R
import com.rfw.clickhelper.data.model.ClickAreaModel
import com.rfw.clickhelper.tools.DisplayUtils
import com.rfw.clickhelper.tools.Extensions.TAG
import com.rfw.clickhelper.view.DoodleImageView

class SnapshotDoodleActivity : Activity() {
    companion object {
        const val INTENT_KEY_IMAGE_URI = "image_uri"
    }

    private lateinit var bitmap: Bitmap
    private lateinit var grafftiImageView: DoodleImageView
    private var lastBackTime = 0L

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snapshot_config)

        DisplayUtils.fullscreen(this)

        grafftiImageView = findViewById(R.id.graffti_image_view)
        intent.getStringExtra(INTENT_KEY_IMAGE_URI)?.let {
            bitmap = BitmapFactory.decodeStream(this.contentResolver.openInputStream(Uri.parse(it)))

            if (bitmap.width > bitmap.height) {
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ClickAreaModel.isLandscape = true
            } else {
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ClickAreaModel.isLandscape = false
            }

            grafftiImageView.setImageBitmap(bitmap)
        }
    }

    override fun onResume() {
        super.onResume()

        Log.w(
            TAG,
            "width: ${bitmap.width}, height: ${bitmap.height}, screen size: ${
                DisplayUtils.getScreenWidth(this)
            } x ${DisplayUtils.getScreenHeight(this)}, real screen size: ${
                DisplayUtils.getRealScreenSize(
                    this
                )
            }"
        )
    }

    override fun onPause() {
        Log.w(TAG, "onPause")

        // 放到onStop/onDestroy中都会出现上一个Activity的onResume先执行，但是onStop/onDestroy尚未执行的情况
//        cropDoodleRectBitmap()
//        ClickAreaModel.bitmap = grafftiImageView.doodledBitmap()

//        if (grafftiImageView.clickArea?.isEmpty() == true) {
//            setResult(RESULT_CANCELED, Intent())
//        } else {
//            val resultIntent = Intent()
//            resultIntent.putExtra("data", grafftiImageView.clickArea)
//            setResult(RESULT_OK, resultIntent)
//        }

        super.onPause()
    }

    override fun onStop() {
        Log.w(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val now = System.currentTimeMillis()
            if (now - lastBackTime <= 1500) {
                if (grafftiImageView.clickArea?.isEmpty() == true) {
                    setResult(RESULT_CANCELED, Intent())
                } else {
                    val resultIntent = Intent()
                    resultIntent.putExtra("data", grafftiImageView.clickArea)
                    setResult(RESULT_OK, resultIntent)
                }
                finish()
            } else {
                lastBackTime = now
                Toast.makeText(this, "再次点击返回退出", Toast.LENGTH_SHORT).show()
            }

            return false
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun finish() {
//        if (grafftiImageView.clickArea?.isEmpty() == true) {
//            setResult(RESULT_CANCELED, Intent())
//        } else {
//            val resultIntent = Intent()
//            resultIntent.putExtra("data", grafftiImageView.clickArea)
//            setResult(RESULT_OK, resultIntent)
//        }

        super.finish()
    }
}