package net.taikula.autohelper.activity


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.*
import android.view.*
import net.taikula.autohelper.databinding.ActivityMainBinding
import net.taikula.autohelper.tools.ViewUtils.setSafeClickListener


@SuppressLint("WrongConstant")
class MainActivity : BaseCompatActivity<ActivityMainBinding>() {


    override fun onCreateViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.autoJobBtn.setSafeClickListener {
            startActivity(Intent(this, net.taikula.autojob.MainActivity::class.java))
        }

        binding.btnClickHelper.setSafeClickListener {
            startActivity(Intent(this, ClickHelperActivity::class.java))
        }

        binding.imageView.setSafeClickListener {
            (binding.imageView.drawable as? Animatable)?.run {
                if (isRunning) {
                    stop()
                } else {
                    start()
                }
            }
        }
    }


}