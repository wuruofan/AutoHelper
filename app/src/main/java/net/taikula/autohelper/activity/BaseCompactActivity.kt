package net.taikula.autohelper.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseCompatActivity<VB : ViewBinding> : AppCompatActivity() {

    lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = onCreateViewBinding(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * 视图绑定
     * @see MainActivity.onCreateViewBinding
     */
    abstract fun onCreateViewBinding(layoutInflater: LayoutInflater): VB

}