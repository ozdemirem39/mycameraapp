package com.example.mycameraapp

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.camera2.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.Collections
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.*


class CameraActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var overlayView: OverlayView
    private lateinit var btnCapture: Button
    private lateinit var btnNext: Button
    private lateinit var btnFinishSend: Button

    private val REQUEST_CAMERA_PERMISSION = 200
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var textCounter: TextView


    private val rgbList = ArrayList<String>()
    private val hsvList = ArrayList<String>()
    private val colorDataList = mutableListOf<ColorData>()
    private var photoCount = 0



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Kamera izni reddedildi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textureView = findViewById(R.id.textureView)
        overlayView = findViewById(R.id.overlayView)
        btnCapture = findViewById(R.id.btnCapture)
        btnNext = findViewById(R.id.btnNext)
        btnFinishSend = findViewById(R.id.btnFinishSend)

        // Kamera izni kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            // Eğer TextureView zaten hazırsa, doğrudan kamerayı aç
            if (textureView.isAvailable) {
                openCamera()
            } else {
                textureView.surfaceTextureListener = textureListener
            }
        }

        btnCapture.setOnClickListener { capturePhoto() }
        btnNext.setOnClickListener {
            Toast.makeText(this, "Veriler kaydedildi. Yeni çekime hazır.", Toast.LENGTH_SHORT).show()
        }
        btnFinishSend.setOnClickListener { promptEmailAndSend() }
        textCounter = findViewById(R.id.textCounter)

        textCounter = findViewById(R.id.textCounter)
        textCounter.text = "0 ölçüm alındı"


    }

    private fun generateXLSX(): File {
        val file = File(getExternalFilesDir(null), "data.xlsx")

        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Veriler")

        // Başlıklar
        val header = listOf(
            "R1", "G1", "B1", "R2", "G2", "B2", "R3", "G3", "B3",
            "H1", "S1", "V1", "H2", "S2", "V2", "H3", "S3", "V3"
        )
        val headerRow = sheet.createRow(0)
        for ((i, title) in header.withIndex()) {
            headerRow.createCell(i).setCellValue(title)
        }

        // Veri satırları
        var rowIndex = 1
        for (i in colorDataList.indices step 3) {
            if (i + 2 >= colorDataList.size) break
            val row = sheet.createRow(rowIndex++)

            for (j in 0..2) {
                val d = colorDataList[i + j]
                val baseIndex = j * 3
                row.createCell(baseIndex).setCellValue(d.r.toDouble())
                row.createCell(baseIndex + 1).setCellValue(d.g.toDouble())
                row.createCell(baseIndex + 2).setCellValue(d.b.toDouble())
            }

            for (j in 0..2) {
                val d = colorDataList[i + j]
                val baseIndex = 9 + j * 3
                row.createCell(baseIndex).setCellValue(d.h.toDouble())
                row.createCell(baseIndex + 1).setCellValue(d.s.toDouble())
                row.createCell(baseIndex + 2).setCellValue(d.v.toDouble())
            }
        }

        // Yaz ve dosyayı oluştur
        file.outputStream().use { workbook.write(it) }
        workbook.close()

        return file
    }

    private fun sendEmailWithAttachment(email: String, file: File) {
        val fileUri = FileProvider.getUriForFile(
            this@CameraActivity,
            "com.example.mycameraapp.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "RGB ve HSV Verileri")
            putExtra(Intent.EXTRA_TEXT, "Ekli dosyada çekilen veriler yer almaktadır.")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "E-posta gönder..."))
        } else {
            Toast.makeText(this, "Mail uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptEmailAndSend() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Mail adresinizi girin")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Gönder") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                val file = generateXLSX()
                sendEmailWithAttachment(email, file)

                // 🔄 Verileri gönderimden sonra temizle
                colorDataList.clear()
                rgbList.clear()
                hsvList.clear()
                photoCount = 0
                textCounter.text = "0 ölçüm alındı"
            } else {
                Toast.makeText(this, "Lütfen geçerli bir e-posta adresi girin", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
        builder.setNegativeButton("İptal", null)
        builder.show()
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
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0] // Arka kamera
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
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
            startCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    private fun startCameraPreview() {
        try {
            val texture = textureView.surfaceTexture ?: return
            texture.setDefaultBufferSize(textureView.width, textureView.height)
            val surface = Surface(texture)

            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }

            cameraDevice!!.createCaptureSession(
                Collections.singletonList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(this@CameraActivity, "Kamera yapılandırma hatası", Toast.LENGTH_SHORT).show()
                    }
                },
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Kamera önizleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            captureSession?.setRepeatingRequest(captureRequestBuilder.build(), null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun capturePhoto() {
        val bitmap = textureView.bitmap ?: return
        val rectBounds = overlayView.getRectBounds()
        val left = rectBounds[0]
        val top = rectBounds[1]
        val right = rectBounds[2]
        val bottom = rectBounds[3]

        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)

        val partWidth = cropped.width / 3

        for (i in 0 until 3) {
            var sumR = 0L
            var sumG = 0L
            var sumB = 0L
            var sumH = 0L
            var sumS = 0L
            var sumV = 0L
            var count = 0

            val startX = i * partWidth
            val endX = if (i == 2) cropped.width else startX + partWidth

            for (y in 0 until cropped.height step 10) {
                for (x in startX until endX step 10) {
                    val pixel = cropped.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    sumR += r
                    sumG += g
                    sumB += b

                    val hsv = FloatArray(3)
                    Color.RGBToHSV(r, g, b, hsv)
                    sumH += hsv[0].toLong()
                    sumS += (hsv[1] * 100).toLong()
                    sumV += (hsv[2] * 100).toLong()
                    count++
                }
            }

            if (count > 0) {
                val avgR = (sumR / count).toInt()
                val avgG = (sumG / count).toInt()
                val avgB = (sumB / count).toInt()
                val avgH = sumH.toFloat() / count
                val avgS = sumS.toFloat() / 100 / count
                val avgV = sumV.toFloat() / 100 / count

                // Verileri listeye ekle
                colorDataList.add(ColorData(avgR, avgG, avgB, avgH, avgS, avgV))

                // Ayrıca görsel olarak göstermek için string listene de eklemek istersen:
                rgbList.add("R: $avgR, G: $avgG, B: $avgB")
                hsvList.add("H: $avgH, S: $avgS, V: $avgV")
            }
        }

        Toast.makeText(this, "3 set renk verisi kaydedildi", Toast.LENGTH_SHORT).show()

        photoCount++
        textCounter.text = "$photoCount fotoğraf çekildi"


    }


    override fun onPause() {
        cameraDevice?.close()
        super.onPause()
    }

    data class ColorData(
        val r: Int,
        val g: Int,
        val b: Int,
        val h: Float,
        val s: Float,
        val v: Float
    )
}
