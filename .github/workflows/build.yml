package com.jbofficial.zoomflow

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_SCREEN_CAPTURE = 100
    }

    private lateinit var status: TextView
    private lateinit var button: Button

    private var recording = false

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

        status = TextView(this).apply {
            text = "\nReady to record your screen."
            textSize = 18f
        }

        button = Button(this).apply {
            text = "START RECORDING"

            setOnClickListener {

                if (!recording) {
                    requestScreenCapture()
                } else {
                    stopRecording()
                }
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(button)

        setContentView(layout)
    }

    private fun requestScreenCapture() {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            REQUEST_SCREEN_CAPTURE
        )
    }

    @Deprecated("Use Activity Result API in future versions")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == REQUEST_SCREEN_CAPTURE &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {

            val serviceIntent =
                Intent(
                    this,
                    RecordingService::class.java
                ).apply {

                    action =
                        RecordingService.ACTION_START

                    putExtra(
                        RecordingService.EXTRA_RESULT_CODE,
                        resultCode
                    )

                    putExtra(
                        RecordingService.EXTRA_RESULT_DATA,
                        data
                    )
                }

            startForegroundService(serviceIntent)

            recording = true

            button.text = "STOP RECORDING"

            status.text =
                "\n🔴 Recording...\n\n" +
                "Your screen is being recorded."
        }
    }

    private fun stopRecording() {

        val intent =
            Intent(
                this,
                RecordingService::class.java
            ).apply {
                action =
                    RecordingService.ACTION_STOP
            }

        startService(intent)

        recording = false

        button.text = "START RECORDING"

        status.text =
            "\n✅ Recording saved.\n\n" +
            "Check Movies/ZoomFlow."
    }
}
