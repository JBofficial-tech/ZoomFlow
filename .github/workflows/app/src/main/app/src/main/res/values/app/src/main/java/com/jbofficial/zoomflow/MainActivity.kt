package com.jbofficial.zoomflow

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val title = TextView(this).apply {
            text = "ZoomFlow"
            textSize = 32f
        }

        val info = TextView(this).apply {
            text = "\nRecord your Android screen and create smooth automatic zoom effects."
            textSize = 18f
        }

        val button = Button(this).apply {
            text = "START RECORDING"

            setOnClickListener {
                info.text =
                    "\nScreen recording engine coming next.\n\nAPK build is working!"
            }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(button)

        setContentView(layout)
    }
}
