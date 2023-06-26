package net.taikula.autohelper.activity

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import net.taikula.autohelper.MainApp
import net.taikula.autohelper.R
import net.taikula.autohelper.adapter.ClickConfigAdapter
import net.taikula.autohelper.algorithm.pHash
import net.taikula.autohelper.data.db.entity.ConfigData
import net.taikula.autohelper.databinding.ActivityClickHelperBinding
import net.taikula.autohelper.helper.MediaProjectionHelper
import net.taikula.autohelper.model.ClickArea
import net.taikula.autohelper.model.ClickTask
import net.taikula.autohelper.service.ClickAccessibilityService
import net.taikula.autohelper.service.FloatWindowService
import net.taikula.autohelper.tools.AccessibilityUtils
import net.taikula.autohelper.tools.AccessibilityUtils.click
import net.taikula.autohelper.tools.ColorUtils
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.FloatWindowUtils
import net.taikula.autohelper.tools.PhotoContracts
import net.taikula.autohelper.tools.ViewUtils.setSafeClickListener
import net.taikula.autohelper.viewmodel.ClickViewModel

class ClickHelperActivity : BaseCompatActivity<ActivityClickHelperBinding>() {
    private val mainScope = MainScope()

    private lateinit var mediaProjectionHelper: MediaProjectionHelper

    private val clickViewModel: ClickViewModel by lazy {
        ClickViewModel((application as MainApp).repository)
    }

    private var currentClickTask: ClickTask? = null

    private var lastClickCheck = false

    override fun onCreateViewBinding(layoutInflater: LayoutInflater): ActivityClickHelperBinding {
        return ActivityClickHelperBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initToolbar()

        initMediaProjectionHelper(savedInstanceState)

        initRecyclerView()

        initClickListeners()
    }

    private fun initToolbar() {
        supportActionBar?.apply {
            setTitle(R.string.click_helper)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_baseline_close_24)
        }
    }

    private fun initClickListeners() {
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
                    Snackbar.make(binding.root, "尚未开启悬浮窗权限", Snackbar.LENGTH_SHORT)
                        .setAction("去开启") {
                            FloatWindowUtils.requestPermission(this)
                        }.show()
                }
            } else {
                Snackbar.make(binding.root, "尚未开启辅助功能", Snackbar.LENGTH_SHORT)
                    .setAction("去开启") {
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
                        SnapshotDoodleActivity::class.java
                    )
                    intent.putExtra(
                        SnapshotDoodleActivity.INTENT_KEY_IMAGE_URI,
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
    }

    private fun initRecyclerView() {
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
                canvas.drawColor(ColorUtils.getColor(this@ClickHelperActivity, R.attr.colorPrimary))
            }
        }


        val clickConfigAdapter = ClickConfigAdapter().apply {
            draggableModule.run {
                isSwipeEnabled = true
                isDragEnabled = true
                setOnItemDragListener(listener)
                setOnItemSwipeListener(onItemSwipeListener)
                itemTouchHelperCallback
                    .setSwipeMoveFlags(ItemTouchHelper.START or ItemTouchHelper.END)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = clickConfigAdapter


        clickViewModel.currentClickData.observe(this) { allData ->
            if (allData == null)
                return@observe

            clickConfigAdapter.setList(allData)
            currentClickTask = ClickTask(allData)
        }

        clickViewModel.allConfigData.observe(this) {
            val list = it.map { config ->
                Log.w(TAG, "configs: ${config.id} - ${config.name}")
                config.name
            }
            binding.spinnerView.adapter = ArrayAdapter(this@ClickHelperActivity, android.R.layout.simple_spinner_item, list).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }
        }

        binding.spinnerView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.i(TAG, "click ${clickViewModel.allConfigData.value?.get(position)?.name}")
                clickViewModel.allConfigData.value?.let {
                    clickViewModel.currentConfigId = it[position].id
                    clickViewModel.queryClickData(clickViewModel.currentConfigId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        binding.newConfigImageView.setSafeClickListener {
            clickViewModel.insert(ConfigData(0, "配置_${clickViewModel.allConfigData.value?.size}"))
        }
    }

    private fun initMediaProjectionHelper(savedInstanceState: Bundle?) {
        mediaProjectionHelper = MediaProjectionHelper.initInstance(this)
        mediaProjectionHelper.onRestoreInstanceState(savedInstanceState)
        mediaProjectionHelper.setImageReadyCallback { bitmap ->
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
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_test -> {
//                startActivity(
//                    Intent(
//                        this,
//                        TestActivity::class.java
//                    )
//                )
            }

            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
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

        val width = bitmap.width
        val height = bitmap.height
        val doodleWidth = rect.width()
        val doodleHeight = rect.height()
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

}