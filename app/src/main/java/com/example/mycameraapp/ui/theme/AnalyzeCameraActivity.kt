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
import android.util.Log
import org.mariuszgromada.math.mxparser.*

class AnalyzeCameraActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var overlayView: OverlayView
    private lateinit var btnCapture: Button
    private lateinit var resultText: TextView

    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraId: String
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var captureSession: CameraCaptureSession? = null

    private lateinit var formula: String // Kullanıcının girdiği formül
    private var m: Float = 1f
    private var n: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyze_camera)

        textureView = findViewById(R.id.textureView)
        overlayView = findViewById(R.id.overlayView)
        btnCapture = findViewById(R.id.btnCapture)
        resultText = findViewById(R.id.resultText)

        // Kullanıcının girdilerini al
        formula = intent.getStringExtra("formula") ?: "R"
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

        var sum: Double = 0.0
        var count = 0

        for (y in 0 until cropped.height step 10) {
            for (x in 0 until cropped.width step 10) {
                val pixel = cropped.getPixel(x, y)

                // RGB (hepsi Int)
                val rInt = Color.red(pixel)
                val gInt = Color.green(pixel)
                val bInt = Color.blue(pixel)

                val hsv = FloatArray(3)
                Color.RGBToHSV(rInt, gInt, bInt, hsv)

                val h = hsv[0].toDouble()
                val s = hsv[1].toDouble() * 100
                val v = hsv[2].toDouble() * 100

                // Kullanıcının girdisini işlemek için Expression kullanılıyor
                val expression = Expression(formula)
                expression.addArguments(
                    Argument("R", rInt.toDouble()),
                    Argument("G", gInt.toDouble()),
                    Argument("B", bInt.toDouble()),
                    Argument("H", h),
                    Argument("S", s),
                    Argument("V", v)
                )
                val value = expression.calculate()
                sum += value
                count++
            }
        }

        val y = sum / count
        if (m == 0f) {
            Toast.makeText(this, "m değeri sıfır olamaz!", Toast.LENGTH_SHORT).show()
            return
        }

        val x = (y - n) / m

        Log.d("ANALYZE", "Ortalama $formula: $y | m: $m | n: $n | x: $x | count: $count")

        // Dinamik olarak formülü göstermek için güncellendi
        resultText.text = "Ortalama $formula: ${"%.2f".format(y)}\nTahmini Derişim: ${"%.2f".format(x)}"
    }

    override fun onPause() {
        cameraDevice?.close()
        super.onPause()
    }
}