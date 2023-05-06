package net.taikula.autohelper.activity


import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import net.taikula.autohelper.R
import net.taikula.autohelper.algorithm.pHash
import net.taikula.autohelper.data.model.ClickAreaModel
import net.taikula.autohelper.helper.MediaProjectionHelper
import net.taikula.autohelper.service.ClickAccessibilityService
import net.taikula.autohelper.service.FloatWindowService
import net.taikula.autohelper.tools.AccessibilityUtils
import net.taikula.autohelper.tools.AccessibilityUtils.click
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.FloatWindowUtils
import net.taikula.autohelper.tools.PhotoContracts


@SuppressLint("WrongConstant")
class TestActivity : AppCompatActivity() {
    private lateinit var doodleImageView: ImageView
    private lateinit var captureImageView: ImageView
    private lateinit var textView: TextView

    private lateinit var mediaProjectionHelper: MediaProjectionHelper

    private var doodleHash = 0L
    private var captureHash = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        supportActionBar?.title = "测试"

        mediaProjectionHelper = MediaProjectionHelper.initInstance(this) { bitmap ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (bitmap != null) {
                    captureImageView.setImageBitmap(cropDoodleRectBitmap(bitmap))
                    captureHash = pHash.dctImageHash(bitmap, false)
                    Log.w(
                        TAG,
                        "bitmap: ${bitmap.width} x ${bitmap.height}, captureHash=$captureHash"
                    )

                    val distance = pHash.hammingDistance(doodleHash, captureHash)
                    textView.text = distance.toString()

                    if (pHash.isSimilar(distance)) {
                        Log.w(TAG, "hammingDistance=$distance, is similar, try to click it!")

                        val point = ClickAreaModel.clickArea.randomPoint()
                        Log.w(TAG, "get random click point: $point")
                        ClickAccessibilityService.accessibilityService?.click(point.x, point.y)
                    }
                } else {
                    Log.d(TAG, "image == null")
                }

                Toast.makeText(
                    this,
                    "截图完成: ${this.resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}",
                    Toast.LENGTH_SHORT
                ).show()
            }, 1000)
        }
        mediaProjectionHelper.onRestoreInstanceState(savedInstanceState)

        doodleImageView = findViewById(R.id.doodle_image_view)
        captureImageView = findViewById(R.id.capture_image_view)
        textView = findViewById(R.id.tv_similarity)

        val layout = findViewById<CoordinatorLayout>(R.id.root_layout)


        findViewById<FloatingActionButton>(R.id.fab_run).setOnClickListener {
            if (AccessibilityUtils.isPermissionGranted(
                    ClickAccessibilityService::class.java.name,
                    this
                )
            ) {
                if (FloatWindowUtils.isPermissionGranted(this)) {
                    if (mediaProjectionHelper.isPermissionGranted()) {
                        startService(Intent(this, FloatWindowService::class.java))
                        moveTaskToBack(true)
                    } else {
                        mediaProjectionHelper.requestPermission()
                    }
                } else {
                    Snackbar.make(layout, "尚未开启悬浮窗权限", Snackbar.LENGTH_SHORT).setAction("去开启") {
                        FloatWindowUtils.requestPermission(this)
                    }.show()
                }
            } else {
                Snackbar.make(layout, "尚未开启辅助功能", Snackbar.LENGTH_SHORT).setAction("去开启") {
                    AccessibilityUtils.requestPermission(
                        this
                    )
                }.show()
            }
        }

        // 选择图片
        val selectPhoto =
            registerForActivityResult(PhotoContracts.SelectPhotoContract()) { uri: Uri? ->
                if (uri != null) {
//                // 返回的选择的图片uri
//                if (needCrop) {
//                    // 需要剪裁图片，再调用剪裁图片的launch()方法
//                    cropPhoto.launch(CropParams(uri))
//                } else {
//                    // 如果不剪裁图片，则直接显示
//                    ivImage.setImageURI(uri)
//                }
                    val intent = Intent(this, SnapshotDoodleActivity::class.java)
                    intent.putExtra(SnapshotDoodleActivity.INTENT_KEY_IMAGE_URI, uri.toString())
                    startActivity(intent)
//                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//
//                }
                } else {
                    Toast.makeText(this, "您没有选择任何图片", Toast.LENGTH_SHORT).show()
                }
            }

        findViewById<FloatingActionButton>(R.id.fab_add_config).setOnClickListener {
            selectPhoto.launch(null)
        }

        findViewById<FloatingActionButton>(R.id.fab_help).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDefaultBrowser(this)
            }
        }

    }

    override fun onResume() {
        Log.w(TAG, "onResume: ${ClickAreaModel.bitmap}")

        ClickAreaModel.bitmap?.let {
            doodleImageView.setImageBitmap(it)
            doodleImageView.invalidate()
            doodleHash = pHash.dctImageHash(it, false)
            Log.w(TAG, "onResume: ${it.isRecycled}, doodleHash=$doodleHash")
        }

        super.onResume()
    }

    override fun onDestroy() {
        mediaProjectionHelper.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mediaProjectionHelper.onSaveInstanceState(outState)
    }

    private fun cropDoodleRectBitmap(bitmap: Bitmap?): Bitmap? {
        val rect = ClickAreaModel.clickArea.outlineRect()
        if (rect.left == -1 || bitmap == null) {
            return null
        }

        var width = bitmap.width
        var height = bitmap.height
        var doodleWidth = rect.width()
        var doodleHeight = rect.height()
//        if (isLandscape && width < height) {
////            width = height.also { height = width } // swap: https://stackoverflow.com/a/45377921/1097709
//            val tmp = doodleWidth
//            doodleWidth = doodleHeight
//            doodleHeight = tmp
//        }
        Log.w(
            TAG,
            "cropBitmap: origin size = $width x $height, doodle size = $doodleWidth x $doodleHeight, rect: $rect"
        )

        return Bitmap.createBitmap(bitmap, rect.left, rect.top, doodleWidth, doodleHeight)

    }

    /********************** test code **********************/

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setDefaultBrowser(activity: Activity) {
        val roleManager: RoleManager = activity.getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) && !roleManager.isRoleHeld(
                RoleManager.ROLE_BROWSER
            )
        ) {
            Log.w(TAG, "start activity now!")
            activity.startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER),
                0x5222
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0x5222) {
            Log.w(TAG, "onActivityResult: $resultCode, $data")
        }
    }
}