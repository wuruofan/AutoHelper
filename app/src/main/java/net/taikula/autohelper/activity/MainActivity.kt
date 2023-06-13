package net.taikula.autohelper.activity


import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.view.*
import net.taikula.autohelper.databinding.ActivityMainBinding


@SuppressLint("WrongConstant")
class MainActivity : BaseCompatActivity<ActivityMainBinding>() {


    override fun onCreateViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.autoJobBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnClickHelper.setOnClickListener {
            startActivity(Intent(this, ClickHelperActivity::class.java))
        }
    }



}