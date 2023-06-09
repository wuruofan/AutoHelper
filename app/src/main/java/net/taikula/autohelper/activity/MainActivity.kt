package net.taikula.autohelper.activity


import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.taikula.autohelper.R
import net.taikula.autohelper.adapter.ClickConfigAdapter
import net.taikula.autohelper.algorithm.pHash
import net.taikula.autohelper.data.model.ClickArea
import net.taikula.autohelper.data.model.ClickAreaModel
import net.taikula.autohelper.data.model.ClickTask
import net.taikula.autohelper.data.viewmodel.ClickViewModel
import net.taikula.autohelper.databinding.ActivityMainBinding
import net.taikula.autohelper.helper.ImageReadyCallback
import net.taikula.autohelper.helper.MediaProjectionHelper
import net.taikula.autohelper.service.ClickAccessibilityService
import net.taikula.autohelper.service.FloatWindowService
import net.taikula.autohelper.tools.AccessibilityUtils
import net.taikula.autohelper.tools.AccessibilityUtils.click
import net.taikula.autohelper.tools.ColorUtils
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.FloatWindowUtils
import net.taikula.autohelper.tools.PhotoContracts


@SuppressLint("WrongConstant")
class MainActivity : BaseCompatActivity<ActivityMainBinding>() {

    private lateinit var mediaProjectionHelper: MediaProjectionHelper

    private var doodleHash = 0L
    private var captureHash = 0L

    private val clickViewModel: ClickViewModel by lazy {
        ClickViewModel((application as net.taikula.autohelper.MainApp).repository)
    }

    private var currentClickTask: ClickTask? = null
    var lastClickCheck = false

    private val mainScope = MainScope()

    private val imageReadyCallback: ImageReadyCallback = { bitmap ->
        mainScope.launch {
            val clickTask = currentClickTask ?: return@launch

            Log.w(TAG, "next task: $clickTask, last click check: $lastClickCheck")

            if (bitmap != null) {
                val doodleBitmap =
                    cropDoodleRectBitmap(bitmap, clickTask.currentClickArea.outlineRect())
                val captureHash = pHash.dctImageHash(doodleBitmap, false)
                val targetHash = clickTask.currentDstPHash

                Log.w(
                    TAG,
                    "bitmap: ${bitmap.width} x ${bitmap.height}, captureHash=$captureHash, targetHash=$targetHash"
                )

                bitmap.recycle()
                doodleBitmap?.recycle()

                val distance = pHash.hammingDistance(captureHash, targetHash)

                if (pHash.isSimilar(distance)) {
                    Log.w(TAG, "hammingDistance=$distance, is similar, try to click it!")

                    val point = clickTask.currentClickPoint
                    Log.w(TAG, "get random click point: $point")

//                    delay(100)
                    ClickAccessibilityService.accessibilityService?.click(point.x, point.y)

                    lastClickCheck = true

//                    Toast.makeText(
//                        net.taikula.autohelper.MainApp.appContext,
//                        "识别成功：${clickTask.currentClickArea.outlineRect()}, 点击：$point}",
//                        Toast.LENGTH_SHORT
//                    ).show()
                } else {
                    // 检查上一次点击事件是否成功
                    if (lastClickCheck) {
                        Log.i(TAG, "last click success, do next!")

                        clickTask.runningCount++
                        lastClickCheck = false
                    }
                }
            } else {
                Log.d(TAG, "image == null")
            }

        }
    }

    override fun onCreateViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProjectionHelper = MediaProjectionHelper.initInstance(this)
        mediaProjectionHelper.onRestoreInstanceState(savedInstanceState)
        mediaProjectionHelper.setImageReadyCallback(imageReadyCallback)

        // 拖拽监听
        val listener: OnItemDragListener = object : OnItemDragListener {
            override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "drag start")
                val holder = viewHolder as BaseViewHolder

                // 开始时，item背景色变化，demo这里使用了一个动画渐变，使得自然
                val startColor = Color.WHITE
                val endColor = Color.rgb(245, 245, 245)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val v = ValueAnimator.ofArgb(startColor, endColor)
                    v.addUpdateListener { animation -> holder.itemView.setBackgroundColor(animation.animatedValue as Int) }
                    v.duration = 300
                    v.start()
                }
            }

            override fun onItemDragMoving(
                source: RecyclerView.ViewHolder,
                from: Int,
                target: RecyclerView.ViewHolder,
                to: Int
            ) {
                Log.d(
                    TAG,
                    "move from: " + source.adapterPosition + " to: " + target.adapterPosition
                )
            }

            override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "drag end: $pos")
                val holder = viewHolder as BaseViewHolder
                // 结束时，item背景色变化，demo这里使用了一个动画渐变，使得自然
                val startColor = Color.rgb(245, 245, 245)
                val endColor = Color.WHITE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val v = ValueAnimator.ofArgb(startColor, endColor)
                    v.addUpdateListener { animation -> holder.itemView.setBackgroundColor(animation.animatedValue as Int) }
                    v.duration = 300
                    v.start()
                }

            }
        }

        // 侧滑监听
        val onItemSwipeListener: OnItemSwipeListener = object : OnItemSwipeListener {
            override fun onItemSwipeStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "view swiped start: $pos")
                val holder = viewHolder as BaseViewHolder
            }

            override fun clearView(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "View reset: $pos")
                val holder = viewHolder as BaseViewHolder
            }

            override fun onItemSwiped(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "View Swiped: $pos")
                clickViewModel.delete(pos)
            }

            override fun onItemSwipeMoving(
                canvas: Canvas,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                isCurrentlyActive: Boolean
            ) {
                canvas.drawColor(ColorUtils.getColor(this@MainActivity, R.attr.colorPrimary))
            }
        }


        val clickConfigAdapter = ClickConfigAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = clickConfigAdapter

        clickConfigAdapter.draggableModule.isSwipeEnabled = true
        clickConfigAdapter.draggableModule.isDragEnabled = true
        clickConfigAdapter.draggableModule.setOnItemDragListener(listener)
        clickConfigAdapter.draggableModule.setOnItemSwipeListener(onItemSwipeListener)
        clickConfigAdapter.draggableModule.itemTouchHelperCallback
            .setSwipeMoveFlags(ItemTouchHelper.START or ItemTouchHelper.END)

        clickViewModel.allData.observe(this, Observer { allData ->
            clickConfigAdapter.setList(allData)
            currentClickTask = ClickTask(allData)
        })


        binding.fabRun.setOnClickListener {
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
                    Snackbar.make(binding.root, "尚未开启悬浮窗权限", Snackbar.LENGTH_SHORT).setAction("去开启") {
                        FloatWindowUtils.requestPermission(this)
                    }.show()
                }
            } else {
                Snackbar.make(binding.root, "尚未开启辅助功能", Snackbar.LENGTH_SHORT).setAction("去开启") {
                    AccessibilityUtils.requestPermission(
                        this
                    )
                }.show()
            }
        }

        val doodleActivityLaunch =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val intent = it.data
                    val clickArea = intent?.getSerializableExtra("data") as? ClickArea
                    if (clickArea != null) {
                        clickViewModel.insert(clickArea)
                    }
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
                    val intent = Intent(
                        this,
                        net.taikula.autohelper.activity.SnapshotDoodleActivity::class.java
                    )
                    intent.putExtra(
                        net.taikula.autohelper.activity.SnapshotDoodleActivity.Companion.INTENT_KEY_IMAGE_URI,
                        uri.toString()
                    )
//                    startActivity(intent)
                    doodleActivityLaunch.launch(intent)
                } else {
                    Toast.makeText(this, "您没有选择任何图片", Toast.LENGTH_SHORT).show()
                }
            }

        binding.fabAddConfig.setOnClickListener {
            selectPhoto.launch(null)
        }

        binding.fabHelp.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            }
        }

        binding.autoJobBtn.setOnClickListener {
            startActivity(Intent(this, net.taikula.autojob.MainActivity::class.java))
        }

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_test -> {
                startActivity(
                    Intent(
                        this,
                        net.taikula.autohelper.activity.TestActivity::class.java
                    )
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        Log.w(TAG, "onResume: ${ClickAreaModel.bitmap}")

        super.onResume()
    }

    override fun onDestroy() {
        mediaProjectionHelper.destroy()
        mainScope.cancel()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mediaProjectionHelper.onSaveInstanceState(outState)
    }

    private fun cropDoodleRectBitmap(bitmap: Bitmap?, rect: Rect): Bitmap? {
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

}