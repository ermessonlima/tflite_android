package com.example.tflite_android

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import com.example.tflite_android.ml.Android
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.IOException
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE  = 1
    val REQUEST_PICK_IMAGE = 2
    lateinit var imageProcessor: ImageProcessor
    lateinit var model: Android
    lateinit var imageView: ImageView
    lateinit var  buttonSelectImage:  Button
    lateinit var  buttonStart: Button
    lateinit var labels:List<String>

    val paint = Paint()
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        get_permissions()

        labels = FileUtil.loadLabels(this, "labels.txt")
        buttonSelectImage = findViewById(R.id.button_select_image)
        buttonStart = findViewById(R.id.button_start)
        buttonSelectImage.setOnClickListener {
            Log.d("TFLite-ODT", "Detected object:  ")
            dispatchTakePictureIntent()
        }

        buttonStart.setOnClickListener {
            Log.d("TFLite-ODT", "Detected object:  ")
            dispatchPickPictureIntent()
        }
        imageView = findViewById(R.id.imageView)

        val imageBitmap = getBitmapFromAsset("teste.jpg")
        try {
            imageProcessor = ImageProcessor.Builder().add(ResizeOp(192, 192, ResizeOp.ResizeMethod.BILINEAR)).build()
            model = Android.newInstance(this)

            if (imageBitmap != null) {
                runObjectDetection(imageBitmap)
            }

        } catch (e: Exception) {

            Log.e("Erro ao carregar modelo","=>", e)
        }


    }


    fun getBitmapFromAsset(assetName: String): Bitmap? {
        return try {
            val assetManager = assets
            val inputStream = assetManager.open(assetName)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            Log.d("AssetLoader", "Imagem carregada com sucesso.")
            bitmap
        } catch (e: IOException) {
            Log.e("AssetLoader", "Erro ao carregar imagem: ${e.message}")
            null
        }
    }



    @SuppressLint("MissingPermission")
    private fun dispatchTakePictureIntent() {
        Log.d("TFLite-ODddT", "Detected object:  ")
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        Log.d("TFdsfdsfLite-ODddT", "Detected object: ${REQUEST_IMAGE_CAPTURE}  ")
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE )

    }

    // Abre a galeria para selecionar uma imagem
    private fun dispatchPickPictureIntent() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { pickPictureIntent ->
            pickPictureIntent.type = "image/*"
            startActivityForResult(pickPictureIntent, REQUEST_PICK_IMAGE)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }



    fun get_permissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }
    override fun onRequestPermissionsResult(  requestCode: Int, permissions: Array<out String>, grantResults: IntArray  ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED) get_permissions()
    }

    private fun runObjectDetection(bitmap: Bitmap) {

        val image = TensorImage.fromBitmap(bitmap)
        val outputs = model.process(image)
        val locations = outputs.locationAsTensorBuffer.floatArray
        val classes = outputs.categoryAsTensorBuffer.floatArray
        val scores = outputs.scoreAsTensorBuffer.floatArray
        var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        val h = mutable.height
        val w = mutable.width
        paint.textSize = h/15f
        paint.strokeWidth = h/85f
        var x = 0
        scores.forEachIndexed { index, fl ->
            x = index
            x *= 4
            if(fl > 0.5){
                paint.setColor(colors.get(index))
                paint.style = Paint.Style.STROKE
                canvas.drawRect(RectF(locations.get(x+1)*w, locations.get(x)*h, locations.get(x+3)*w, locations.get(x+2)*h), paint)
                paint.style = Paint.Style.FILL
                canvas.drawText(labels.get(classes.get(index).toInt())+" "+fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
            }
        }

        imageView.setImageBitmap(mutable)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val imageBitmap: Bitmap? = when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> data?.extras?.get("data") as? Bitmap
                REQUEST_PICK_IMAGE -> {
                    val imageUri = data?.data
                    try {
                        MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        null
                    }
                }
                else -> null
            }

            imageBitmap?.let {
                runObjectDetection(it)
            }
        }
    }
}