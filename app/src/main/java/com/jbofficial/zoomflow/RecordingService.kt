package com.jbofficial.zoomflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.content.ContentValues

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.jbofficial.zoomflow.START"
        const val ACTION_STOP = "com.jbofficial.zoomflow.STOP"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val CHANNEL_ID = "zoomflow_recording"
        private const val NOTIFICATION_ID = 1001
    }

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var outputUri: android.net.Uri? = null
    private var outputFd: ParcelFileDescriptor? = null
    private var recording = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {

                val resultCode =
                    intent.getIntExtra(
                        EXTRA_RESULT_CODE,
                        -1
                    )

                val data =
                    if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA,
                            Intent::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA
                        )
                    }

                if (
                    resultCode != -1 &&
                    data != null &&
                    !recording
                ) {
                    startRecording(
                        resultCode,
                        data
                    )
                }
            }

            ACTION_STOP -> {
                stopRecording()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRecording(
        resultCode: Int,
        data: Intent
    ) {

        try {

            val notification =
                createNotification()

            if (Build.VERSION.SDK_INT >= 29) {

                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo
                        .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )

            } else {

                startForeground(
                    NOTIFICATION_ID,
                    notification
                )
            }

            val metrics =
                resources.displayMetrics

            val width =
                metrics.widthPixels

            val height =
                metrics.heightPixels

            val density =
                metrics.densityDpi

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            projection =
                manager.getMediaProjection(
                    resultCode,
                    data
                )

            if (projection == null) {
                stopSelf()
                return
            }

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Video.Media.DISPLAY_NAME,
                        "ZoomFlow_${System.currentTimeMillis()}.mp4"
                    )

                    put(
                        MediaStore.Video.Media.MIME_TYPE,
                        "video/mp4"
                    )

                    if (Build.VERSION.SDK_INT >= 29) {

                        put(
                            MediaStore.Video.Media.RELATIVE_PATH,
                            "Movies/ZoomFlow"
                        )

                        put(
                            MediaStore.Video.Media.IS_PENDING,
                            1
                        )
                    }
                }

            outputUri =
                contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values
                )

            if (outputUri == null) {
                throw Exception(
                    "Could not create output video"
                )
            }

            outputFd =
                contentResolver.openFileDescriptor(
                    outputUri!!,
                    "w"
                )

            if (outputFd == null) {
                throw Exception(
                    "Could not open output file"
                )
            }

            recorder =
                if (Build.VERSION.SDK_INT >= 31) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

            recorder!!.apply {

                setVideoSource(
                    MediaRecorder.VideoSource.SURFACE
                )

                setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
                )

                setVideoEncoder(
                    MediaRecorder.VideoEncoder.H264
                )

                setVideoEncodingBitRate(
                    8_000_000
                )

                setVideoFrameRate(30)

                setVideoSize(
                    width,
                    height
                )

                setOutputFile(
                    outputFd!!.fileDescriptor
                )

                prepare()
            }

            display =
                projection!!.createVirtualDisplay(
                    "ZoomFlow",
                    width,
                    height,
                    density,
                    DisplayManager
                        .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    recorder!!.surface,
                    null,
                    null
                )

            recorder!!.start()

            recording = true

        } catch (e: Exception) {

            cleanup(true)
            stopSelf()
        }
    }

    private fun stopRecording() {

        if (!recording) {
            stopSelf()
            return
        }

        try {
            recorder?.stop()
        } catch (_: Exception) {
        }

        recorder?.reset()
        recorder?.release()
        recorder = null

        display?.release()
        display = null

        projection?.stop()
        projection = null

        outputFd?.close()
        outputFd = null

        outputUri?.let { uri ->

            if (Build.VERSION.SDK_INT >= 29) {

                contentResolver.update(
                    uri,
                    ContentValues().apply {
                        put(
                            MediaStore.Video.Media.IS_PENDING,
                            0
                        )
                    },
                    null,
                    null
                )
            }
        }

        recording = false
        outputUri = null

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        stopSelf()
    }

    private fun cleanup(
        deleteFile: Boolean
    ) {

        recording = false

        display?.release()
        display = null

        try {
            recorder?.reset()
        } catch (_: Exception) {
        }

        recorder?.release()
        recorder = null

        projection?.stop()
        projection = null

        outputFd?.close()
        outputFd = null

        if (deleteFile) {

            outputUri?.let {
                contentResolver.delete(
                    it,
                    null,
                    null
                )
            }

            outputUri = null
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "ZoomFlow Recording",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return if (Build.VERSION.SDK_INT >= 26) {

            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "ZoomFlow"
                )
                .setContentText(
                    "Screen recording is active"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_camera
                )
                .setOngoing(true)
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(
                    "ZoomFlow"
                )
                .setContentText(
                    "Screen recording is active"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_camera
                )
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {

        if (recording) {

            try {
                recorder?.stop()
            } catch (_: Exception) {
            }

            cleanup(false)
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}