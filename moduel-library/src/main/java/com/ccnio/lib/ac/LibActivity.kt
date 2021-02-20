package com.ccnio.lib.ac

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ccnio.lib.R

class LibActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lib)
        val a = getString(R.string.lib_str)
    }
}