package com.elbin.plaga_limones

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.elbin.plaga_limones.databinding.AppMenuBarBinding
import com.elbin.plaga_limones.ui.theme.Categorizar
import com.google.android.material.navigation.NavigationView
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: AppMenuBarBinding

    private lateinit var mCategorizar: Categorizar
    private lateinit var mBitmap: Bitmap
    private val mCameraRequestCode = 0
    private val mGalleryRequestCode = 2
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    //private val mInputSize = 224
    //private val mInputSize = 128
    private val mInputSize = 128
    //private val mModelPath = "modelo.tflite"
    //private val mLabelPath = "etiquetas_plagas.txt"
    private val mLabelPath = "abecedario.txt"
    //private val mLabelPath  = "alfabeto_n_sergio.txt"
    private val mModelPath = "alexnet_model.tflite"
    private val mSamplePath = "logo_app.jpg"

    lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AppMenuBarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicitar permisos si no están concedidos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }

        mCategorizar = Categorizar(assets, mModelPath, mLabelPath, mInputSize)

        resources.assets.open(mSamplePath).use {
            mBitmap = BitmapFactory.decodeStream(it)
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mInputSize, mInputSize, true)
            binding.mLabelImagenMuestra.setImageBitmap(mBitmap)
        }

        binding.mBotonCamara.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(callCameraIntent, mCameraRequestCode)
        }

        binding.mBotonGaleria.setOnClickListener {
            val callGalleryIntent = Intent(Intent.ACTION_PICK)
            callGalleryIntent.type = "image/*"
            startActivityForResult(callGalleryIntent, mGalleryRequestCode)
        }

        binding.mBotonDetectar.setOnClickListener {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setTitle("Por favor, espera")
            progressDialog.setMessage("Espera mientras se detecta...")
            val handler = Handler()
            handler.postDelayed({
                progressDialog.dismiss()
                val results = mCategorizar.recognizeImage(mBitmap).firstOrNull()
                binding.mResultado.text = results?.titulo + "\n Precisión:" + results?.confianza +"%"
            }, 2000)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mCameraRequestCode) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val extras = data.extras
                if (extras != null) {
                    mBitmap = extras.getParcelable("data") ?: return
                    mBitmap = scaleImage(mBitmap)
                    binding.mLabelImagenMuestra.setImageBitmap(mBitmap)
                    binding.mResultado.text = "Tu foto se ha cargado, presiona Detectar"
                } else {
                    Toast.makeText(this, "No se pudo obtener la imagen de la cámara", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Cancelando...", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == mGalleryRequestCode) {
            if (data != null) {
                val uri = data.data
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                mBitmap = scaleImage(mBitmap)
                binding.mLabelImagenMuestra.setImageBitmap(mBitmap)
            }
        }
    }

    private fun scaleImage(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / originalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
    }
}
