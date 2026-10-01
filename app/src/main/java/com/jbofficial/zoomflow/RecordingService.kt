package com.jbofficial.zoomflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder

class RecordingService : Service() {

    companion object {
        private const val CHANNEL_ID = "zoomflow_recording"
        private const val NOTIFICATION_ID = 1001
    }

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra("resultCode", 0)

        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                "data",
                Intent::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>("data")
        }

        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startRecording(resultCode, data)

        return START_NOT_STICKY
    }

    private fun startRecording(
        resultCode: Int,
        data: Intent
    ) {

        val projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        mediaProjection =
            projectionManager.getMediaProjection(
                resultCode,
                data
            )

        val width = 1080
        val height = 1920
        val dpi = resources.displayMetrics.densityDpi

        val outputFile =
            "${externalCacheDir?.absolutePath}/zoomflow_recording.mp4"

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {

            setVideoSource(
                MediaRecorder.VideoSource.SURFACE
            )

            setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            setVideoEncoder(
                MediaRecorder.VideoEncoder.H264
            )

            setVideoEncodingBitRate(8_000_000)

            setVideoFrameRate(30)

            setVideoSize(
                width,
                height
            )

            setOutputFile(outputFile)

            prepare()
        }

        val surface =
            mediaRecorder?.surface

        if (surface == null) {
            stopSelf()
            return
        }

        mediaProjection?.createVirtualDisplay(
            "ZoomFlowScreen",
            width,
            height,
            dpi,
            0,
            surface,
            null,
            null
        )

        mediaRecorder?.start()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZoomFlow Recording",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ZoomFlow")
                .setContentText("Screen recording is active")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("ZoomFlow")
                .setContentText("Screen recording is active")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }

        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null

        mediaProjection?.stop()
        mediaProjection = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

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
    private var outputUri: Uri? = null
    private var outputFd: android.os.ParcelFileDescriptor? = null
    private var recording = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                val resultCode =
                    intent.getIntExtra(EXTRA_RESULT_CODE, -1)

                val data =
                    if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA,
                            Intent::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(EXTRA_RESULT_DATA)
                    }

                if (resultCode != -1 && data != null && !recording) {
                    startRecording(resultCode, data)
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

            val notification = buildNotification()

            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification
                )
            }

            val metrics = resources.displayMetrics

            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val manager =
                getSystemService(MEDIA_PROJECTION_SERVICE)
                        as MediaProjectionManager

            projection =
                manager.getMediaProjection(
                    resultCode,
                    data
                )

            val values = ContentValues().apply {

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
                throw IllegalStateException(
                    "Unable to create video file"
                )
            }

            outputFd =
                contentResolver.openFileDescriptor(
                    outputUri!!,
                    "w"
                )

            recorder =
                MediaRecorder().apply {

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
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
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

        cleanup(false)

        outputUri?.let {

            if (Build.VERSION.SDK_INT >= 29) {

                contentResolver.update(
                    it,
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

        outputUri = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup(deleteFile: Boolean) {

        recording = false

        display?.release()
        display = null

        recorder?.reset()
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
        }

        if (deleteFile) {
            outputUri = null
        }
    }

    private fun buildNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("ZoomFlow recording")
            .setContentText(
                "Screen recording is active"
            )
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "ZoomFlow Recording",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
