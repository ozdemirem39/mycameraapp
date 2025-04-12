package com.example.mycameraapp

import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.camera2.*
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AnalyzeCameraActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var overlayView: OverlayView
    private lateinit var btnCapture: Button
    private lateinit var resultText: TextView

    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraId: String
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var captureSession: CameraCaptureSession? = null

    private lateinit var channel: String
    private var m: Float = 1f
    private var n: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyze_camera)

        textureView = findViewById(R.id.textureView)
        overlayView = findViewById(R.id.overlayView)
        btnCapture = findViewById(R.id.btnCapture)
        resultText = findViewById(R.id.resultText)

        // Gelen parametreleri al
        channel = intent.getStringExtra("channel") ?: "R"
        m = intent.getFloatExtra("m", 1f)
        n = intent.getFloatExtra("n", 0f)

        textureView.surfaceTextureListener = textureListener

        btnCapture.setOnClickListener {
            captureAndAnalyze()
        }
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture) = false
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }

    private fun openCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 101)
                return
            }
            cameraManager.openCamera(cameraId, stateCallback, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    private fun startPreview() {
        val texture = textureView.surfaceTexture!!
        texture.setDefaultBufferSize(textureView.width, textureView.height)
        val surface = Surface(texture)

        captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
        }

        cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                captureSession!!.setRepeatingRequest(captureRequestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@AnalyzeCameraActivity, "Kamera yapılandırılamadı", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun captureAndAnalyze() {
        val bitmap = textureView.bitmap ?: return
        val bounds = overlayView.getRectBounds()
        val cropped = Bitmap.createBitmap(bitmap, bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1])

        var sum = 0f
        var count = 0

        for (y in 0 until cropped.height step 10) {
            for (x in 0 until cropped.width step 10) {
                val pixel = cropped.getPixel(x, y)
                val value = when (channel) {
                    "R" -> Color.red(pixel).toFloat()
                    "G" -> Color.green(pixel).toFloat()
                    "B" -> Color.blue(pixel).toFloat()
                    else -> {
                        val hsv = FloatArray(3)
                        Color.RGBToHSV(Color.red(pixel), Color.green(pixel), Color.blue(pixel), hsv)
                        when (channel) {
                            "H" -> hsv[0]
                            "S" -> hsv[1] * 100
                            "V" -> hsv[2] * 100
                            else -> 0f
                        }
                    }
                }
                sum += value
                count++
            }
        }

        val y = sum / count
        if (m == 0f) {
            Toast.makeText(this, "m değeri sıfır olamaz!", Toast.LENGTH_SHORT).show()
            return
        }
    }

    override fun onPause() {
        cameraDevice?.close()
        super.onPause()
    }
}
