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
        private const val SCREEN_CAPTURE_REQUEST = 1001
    }

    private lateinit var statusText: TextView
    private lateinit var recordButton: Button

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

        statusText = TextView(this).apply {
            text = """
                Screen recorder with automatic
                zoom and motion effects.

                Ready to record.
            """.trimIndent()

            textSize = 18f
            setPadding(0, 40, 0, 40)
        }

        recordButton = Button(this).apply {
            text = "START RECORDING"

            setOnClickListener {
                requestScreenCapture()
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(recordButton)

        setContentView(layout)
    }

    private fun requestScreenCapture() {

        val projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        val captureIntent =
            projectionManager.createScreenCaptureIntent()

        startActivityForResult(
            captureIntent,
            SCREEN_CAPTURE_REQUEST
        )
    }

    @Deprecated("Use Activity Result API in a future update")
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

        if (requestCode != SCREEN_CAPTURE_REQUEST) {
            return
        }

        if (
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {

            statusText.text = """
                Screen capture permission granted.

                Starting recorder...
            """.trimIndent()

            val serviceIntent = Intent(
                this,
                RecordingService::class.java
            ).apply {

                // IMPORTANT: tell the service to START
                action = RecordingService.ACTION_START

                // IMPORTANT: use the exact keys
                // expected by RecordingService
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

            recordButton.text = "RECORDING..."
            recordButton.isEnabled = false

        } else {

            statusText.text = """
                Screen capture permission denied.

                Tap START RECORDING
                to try again.
            """.trimIndent()
        }
    }
}
