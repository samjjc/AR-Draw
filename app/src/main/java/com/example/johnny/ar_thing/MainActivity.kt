package com.example.johnny.ar_thing

import android.os.Bundle
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_main.*
import org.artoolkit.ar.base.ARActivity
import org.artoolkit.ar.base.rendering.ARRenderer

class MainActivity : ARActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun supplyFrameLayout(): FrameLayout = frameLayout

    override fun supplyRenderer(): ARRenderer = Renderer()
}
