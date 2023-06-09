package net.taikula.autohelper.activity

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
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
import net.taikula.autohelper.tools.BitmapUtils.cropRectBitmap
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

    private lateinit var clickConfigAdapter: ClickConfigAdapter

    // 启动截图涂抹 activity
    private val doodleActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val intent = it.data
                val clickArea = intent?.getSerializableExtra("data") as? ClickArea
                if (clickArea != null) {
                    clickViewModel.insert(clickArea)
                }
            }
        }

    // 启动选择图片 activity
    private val selectPhotoActivityLauncher =
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
                    this, SnapshotDoodleActivity::class.java
                )
                intent.putExtra(
                    SnapshotDoodleActivity.INTENT_KEY_IMAGE_URI, uri.toString()
                )
                doodleActivityLauncher.launch(intent)
            } else {
                Toast.makeText(this, "您没有选择任何图片", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateViewBinding(layoutInflater: LayoutInflater): ActivityClickHelperBinding {
        return ActivityClickHelperBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews()
        initMediaProjectionHelper(savedInstanceState)
    }

    private fun initViews() {
        initToolbar()
        initClickListeners()

        // 初始化配置下拉菜单相关
        initConfigSpinnerView()
        initClickDataRecyclerView()
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
            // 辅助功能权限
            if (!AccessibilityUtils.isPermissionGranted(
                    ClickAccessibilityService::class.java.name, this
                )
            ) {
                Snackbar.make(
                    binding.root,
                    resources.getString(R.string.accessibility_permission_not_granted),
                    Snackbar.LENGTH_SHORT
                ).setAction(resources.getString(R.string.go_to_grant_permission)) {
                    AccessibilityUtils.requestPermission(
                        this
                    )
                }.show()

                return@setOnClickListener
            }

            // 悬浮窗权限
            if (!FloatWindowUtils.isPermissionGranted(this)) {
                Snackbar.make(
                    binding.root,
                    resources.getString(R.string.float_window_permission_not_granted),
                    Snackbar.LENGTH_SHORT
                ).setAction(resources.getString(R.string.go_to_grant_permission)) {
                    FloatWindowUtils.requestPermission(this)
                }.show()
                return@setOnClickListener
            }

            // 屏幕录制权限
            if (mediaProjectionHelper.isPermissionGranted()) {
                startService(Intent(this, FloatWindowService::class.java))
                moveTaskToBack(true)
            } else {
                mediaProjectionHelper.requestPermission()
            }
        }


        binding.fabAddConfig.setOnClickListener {
            selectPhotoActivityLauncher.launch(null)
        }

        binding.fabHelp.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            }
        }
    }

    private fun initConfigSpinnerView() {
        // 下拉菜单
        binding.spinnerView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                Log.i(TAG, "click ${clickViewModel.allConfigData.value?.get(position)?.name}")
                clickViewModel.allConfigData.value?.let {
                    clickViewModel.updateCurrentConfigId(it[position].id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        binding.newConfigImageView.setSafeClickListener {
            clickViewModel.insert(ConfigData(0, "配置_${clickViewModel.allConfigData.value?.size}"))
        }


        clickViewModel.allConfigData.observe(this) {
            val list = it.map { config ->
                Log.w(TAG, "configs: ${config.id} - ${config.name}")
                config.name
            }

            binding.spinnerView.adapter = ArrayAdapter(
                this@ClickHelperActivity, android.R.layout.simple_spinner_item, list
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }
        }
    }

    private fun initClickDataRecyclerView() {
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
                source: RecyclerView.ViewHolder, from: Int, target: RecyclerView.ViewHolder, to: Int
            ) {
                Log.d(
                    TAG, "move from: " + source.adapterPosition + " to: " + target.adapterPosition
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

        clickConfigAdapter = ClickConfigAdapter().apply {
            draggableModule.run {
                isSwipeEnabled = true
                isDragEnabled = true
                setOnItemDragListener(listener)
                setOnItemSwipeListener(onItemSwipeListener)
                itemTouchHelperCallback.setSwipeMoveFlags(ItemTouchHelper.START or ItemTouchHelper.END)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = clickConfigAdapter


        clickViewModel.currentClickData.observe(this) { allData ->
            if (allData == null) return@observe

            clickConfigAdapter.setList(allData)
            currentClickTask = ClickTask(allData)
        }
    }

    private fun initMediaProjectionHelper(savedInstanceState: Bundle?) {
        mediaProjectionHelper = MediaProjectionHelper.initInstance(this).apply {
            onRestoreInstanceState(savedInstanceState)
            setImageReadyCallback { bitmap ->
                mainScope.launch {
                    val clickTask = currentClickTask ?: return@launch

                    Log.w(TAG, "next task: $clickTask, last click check: $lastClickCheck")

                    if (bitmap != null) {
                        val doodleBitmap =
                            bitmap.cropRectBitmap(clickTask.currentClickArea.outlineRect())

                        if (doodleBitmap == null) {
                            bitmap.recycle()
                            return@launch
                        }

                        // 获取待识别图片 hash
                        val captureHash = pHash.dctImageHash(doodleBitmap, false)
                        // 目标图片 hash
                        val targetHash = clickTask.currentDstPHash

                        Log.w(
                            TAG,
                            "bitmap: ${bitmap.width} x ${bitmap.height}, captureHash=$captureHash, targetHash=$targetHash"
                        )

                        bitmap.recycle()
                        doodleBitmap.recycle()

                        // 计算汉明距离
                        val distance = pHash.hammingDistance(captureHash, targetHash)
                        // 比较是否相似
                        if (pHash.isSimilar(distance)) {
                            Log.w(TAG, "hammingDistance=$distance, is similar, try to click it!")

                            // 获取随机点并点击
                            val point = clickTask.currentClickPoint
                            Log.w(TAG, "get random click point: $point")
                            ClickAccessibilityService.accessibilityService?.click(point.x, point.y)

                            lastClickCheck = true

                            // 这里不要 toast，会遮挡屏幕截图，导致检查是否点击成功判断为成功！
//                            Toast.makeText(
//                                MainApp.appContext,
//                                "识别成功：${clickTask.currentClickArea.outlineRect()}, 点击：$point}",
//                                Toast.LENGTH_SHORT
//                            ).show()
                        } else {
                            // 检查上一次点击事件是否成功
                            // 和当前图片不一致则认为上次点击成功了，此时再执行 runningCount++
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


}