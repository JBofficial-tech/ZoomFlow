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

        if (resultCode != Activity.RESULT_OK || data == null) {

            statusText.text = """
                Screen capture permission denied.

                Tap START RECORDING
                to try again.
            """.trimIndent()

            recordButton.text = "START RECORDING"
            recordButton.isEnabled = true

            return
        }

        statusText.text = """
            Screen capture permission granted.

            Starting recorder...
        """.trimIndent()

        /*
         * IMPORTANT:
         * RecordingService expects these exact
         * action and extra names.
         */
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

        try {

            startForegroundService(
                serviceIntent
            )

            recordButton.text = "RECORDING..."
            recordButton.isEnabled = false

            statusText.text = """
                Recording started.

                Check the notification bar.
                ZoomFlow should show:

                "Screen recording is active"
            """.trimIndent()

        } catch (e: Exception) {

            statusText.text = """
                Could not start recorder.

                Error:
                ${e.javaClass.simpleName}

                ${e.message}
            """.trimIndent()

            recordButton.text = "START RECORDING"
            recordButton.isEnabled = true
        }
    }
}
