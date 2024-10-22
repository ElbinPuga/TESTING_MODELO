package com.elbin.plaga_limones.ui.theme

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.PriorityQueue


class Categorizar(assetManager: AssetManager, modelPath: String, labelPath: String, inputSize: Int) {

    private val GVN_INP_SZ = inputSize
    private val DESVIACION_FOTO = 255.0f
    private val MEJOR_RESULTADO_MAX = 3
    private val PITNR: Interpreter
    private val ROW_LINE: List<String>
    private val pixels_Imagen: Int =3
    private val PHOTO_MEN = 128.0f
    private val POINT_THRHLDD = 0.4f

    data class Categorizar(
        var id: String = "",
        var titulo: String = "",
        var confianza: Float = 0F

    ){
        init {
            confianza = (confianza * 100).let { String.format("%.3f", it).toFloat() }
        }
        override fun toString(): String {
            return "Titulo = $titulo, Confianza = %.3f".format(confianza)
        }
    }
    init {
        PITNR = Interpreter(loadModelFile(assetManager, modelPath))
        ROW_LINE = loadlabelList(assetManager, labelPath)

    }

    private fun loadlabelList(assetManager: AssetManager, labelPath: String): List<String> {

        return assetManager.open(labelPath).bufferedReader().useLines { it.toList() }

    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer{

        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

    }


    fun recognizeImage(bitmap: Bitmap): List<com.elbin.plaga_limones.ui.theme.Categorizar.Categorizar>{
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, GVN_INP_SZ, GVN_INP_SZ, false)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)
        val result = Array(1){FloatArray(ROW_LINE.size)}
        PITNR.run(byteBuffer, result)
        return  getSortedResult(result)
    }

    private fun getSortedResult(result: Array<FloatArray>): List<com.elbin.plaga_limones.ui.theme.Categorizar.Categorizar> {

        val pq = PriorityQueue(
            MEJOR_RESULTADO_MAX,
            kotlin.Comparator<com.elbin.plaga_limones.ui.theme.Categorizar.Categorizar> {
                (_, _, confianza1), (_, _, confianza2)
            -> java.lang.Float.compare(confianza1,confianza2)* -1}
        )

        for ( i in ROW_LINE.indices){
            val confianza = result[0][i]
            if (confianza >= POINT_THRHLDD){
                pq.add(com.elbin.plaga_limones.ui.theme.Categorizar.Categorizar(
                    ""+i,
                    if(ROW_LINE.size>i) ROW_LINE[i] else "Desconocido", confianza
                ))
            }
        }

        val recognitions = ArrayList<com.elbin.plaga_limones.ui.theme.Categorizar.Categorizar>()
        val recognitionsSize = Math.min(pq.size, MEJOR_RESULTADO_MAX)
        for ( i in 0 until recognitionsSize){
            recognitions.add(pq.poll())
        }
        return recognitions
    }

    private fun convertBitmapToByteBuffer(scaledBitmap: Bitmap?): ByteBuffer{

        val byteBuffer = ByteBuffer.allocateDirect(4*GVN_INP_SZ*GVN_INP_SZ*pixels_Imagen)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValue = IntArray(GVN_INP_SZ*GVN_INP_SZ)

        scaledBitmap!!.getPixels(intValue, 0, scaledBitmap.width,0,0,scaledBitmap.width,scaledBitmap.height)
        var pixel = 0
        for (i in 0 until GVN_INP_SZ){
            for (j in 0 until GVN_INP_SZ){
                val valorPixel = intValue[pixel++]

                byteBuffer.putFloat((((valorPixel.shr(16) and 0xFF)-PHOTO_MEN)/DESVIACION_FOTO))
                byteBuffer.putFloat((((valorPixel.shr(8) and 0xFF)-PHOTO_MEN)/DESVIACION_FOTO))
                byteBuffer.putFloat((((valorPixel and 0xFF)-PHOTO_MEN)/DESVIACION_FOTO))
            }
        }

        return byteBuffer

    }

}