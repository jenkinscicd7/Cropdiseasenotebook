package com.example.crdisease

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.crdisease.adapter.HistoryAdapter
import com.example.crdisease.data.AppDatabase
import com.example.crdisease.model.DiseaseRecord
import com.example.crdisease.utils.UserUtils.getLoggedInUserId
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var camera: Button
    private lateinit var gallery: Button
    private lateinit var imageView: ImageView
    private lateinit var result: TextView
    private val imageSize = 224
    private lateinit var interpreter: Interpreter
    private var userId by Delegates.notNull<Long>()
    private lateinit var database: AppDatabase
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyButton: Button
    private lateinit var adapter: HistoryAdapter

    private val diseaseRecommendations = mapOf(
        "potato_early_blight" to "Remove infected leaves and use fungicides like Mancozeb. Rotate crops regularly.",
        "potato_healthy" to "Your crop is healthy. Continue regular monitoring and best practices.",
        "potato_late_blight" to "Apply copper-based fungicides. Remove infected plants. Improve soil drainage and avoid overhead irrigation."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        camera = findViewById(R.id.button)
        gallery = findViewById(R.id.button2)
        result = findViewById(R.id.result)
        imageView = findViewById(R.id.imageView)
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        historyButton = findViewById(R.id.history_button)

        userId = getLoggedInUserId(this)
        database = AppDatabase.getDatabase(this)

        adapter = HistoryAdapter(mutableListOf())
        historyRecyclerView.adapter = adapter
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set up swipe to delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val record = adapter.getRecordAt(position)

                lifecycleScope.launch {
                    database.diseaseRecordDao().deleteRecord(record)
                    runOnUiThread {
                        adapter.removeRecordAt(position)
                        Toast.makeText(this@MainActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(historyRecyclerView)

        historyButton.setOnClickListener {
            if (historyRecyclerView.visibility == View.VISIBLE) {
                historyRecyclerView.visibility = View.GONE
                historyButton.text = "View History"
            } else {
                lifecycleScope.launch {
                    val history = database.diseaseRecordDao().getRecordsByUserId(userId)
                    runOnUiThread {
                        if (history.isNotEmpty()) {
                            adapter.updateRecords(history)
                            historyRecyclerView.visibility = View.VISIBLE
                            historyButton.text = "Hide History"
                        } else {
                            Toast.makeText(this@MainActivity, "No history found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        try {
            interpreter = Interpreter(loadModelFile("model.tflite"))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        camera.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 3)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            }
        }

        gallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1)
        }
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun classifyImage(bitmap: Bitmap) {
        val resized = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false)
        val byteBuffer = convertBitmapToByteBuffer(resized)

        val output = Array(1) { FloatArray(3) }
        interpreter.run(byteBuffer, output)

        val confidences = output[0]
        val labels = arrayOf("potato_early_blight", "potato_late_blight", "potato_healthy")
        val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: -1
        val predictedLabel = labels[maxIndex]
        val recommendation = diseaseRecommendations[predictedLabel] ?: "No specific recommendation available."

        result.text = "Disease: $predictedLabel\n\nRecommendation:\n$recommendation"

        val diseaseRecord = DiseaseRecord(
            userId = userId,
            disease = predictedLabel,
            recommendation = recommendation,
            timestamp = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            database.diseaseRecordDao().insertRecord(diseaseRecord)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(imageSize * imageSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixelIndex = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val pixel = intValues[pixelIndex++]
                byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                byteBuffer.putFloat((pixel and 0xFF) / 255f)
            }
        }

        return byteBuffer
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                3 -> {
                    var image = data?.extras?.get("data") as Bitmap
                    val dimension = image.width.coerceAtMost(image.height)
                    image = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
                    imageView.setImageBitmap(image)
                    classifyImage(image)
                }

                1 -> {
                    val uri = data?.data
                    val image: Bitmap? = try {
                        MediaStore.Images.Media.getBitmap(this.contentResolver, uri!!)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        null
                    }

                    image?.let {
                        imageView.setImageBitmap(it)
                        classifyImage(it)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter.close()
    }
}



    



