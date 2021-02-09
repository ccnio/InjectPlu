package com.ccnio

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.ccnio.lib.LibUtil

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var prefer: SharedPreferences
    private val key = "keys"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefer = getSharedPreferences("text.share", Context.MODE_PRIVATE)
        Log.d("TAG2", "onCreate: test")
        LibUtil().lib()
        findViewById<View>(R.id.trueView).setOnClickListener {
            prefer.edit {
                putString(
                    key,
                    "true"
                )
            }
        }
        findViewById<View>(R.id.falseView).setOnClickListener {
            prefer.edit {
                putString(
                    key,
                    null
                )
            }
        }
        findViewById<View>(R.id.nullView).setOnClickListener {
//            prefer.
            prefer.edit { remove(key) }

        }

        findViewById<View>(R.id.getView).setOnClickListener {
            val boolean = prefer.getString(key,"")
            Log.d(TAG, "onCreate: prefer = $boolean")
        }
    }
}