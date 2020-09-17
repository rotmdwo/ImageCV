package edu.skku.imagecv

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.luminance
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.NullPointerException


class MainActivity : AppCompatActivity() {
    val REQUEST_CODE = 101

    lateinit var image: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
        }

        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = MediaStore.Images.Media.CONTENT_TYPE
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            startActivityForResult(intent, REQUEST_CODE)
        }

        blurBotton.setOnClickListener {
            blur(image)
        }

        blackAndWhiteButton.setOnClickListener {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) convertToBlackAndWhite(image)
            else Toast.makeText(this, "Unsupported Version", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            var exif: ExifInterface? = null
            var imagePath: String? = ""


            try {
                val fileURI = data?.data

                val filePath = arrayOf(MediaStore.Images.Media.DATA)
                val cursor: Cursor? = fileURI?.let {
                    applicationContext.contentResolver
                        .query(it, filePath, null, null, null)
                }

                cursor?.moveToFirst()
                imagePath = cursor?.getString(cursor.getColumnIndex(filePath[0]))
                cursor?.close()

                imagePath?.let {
                    exif = ExifInterface(imagePath)
                }

            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            val options = BitmapFactory.Options()
            options.inSampleSize = 4
            image = BitmapFactory.decodeFile(imagePath, options)
            val degrees = orientation?.let { exifOrientationToDegrees(it) }
            degrees?.let { image = rotate(image, it) }
            imageView.setImageBitmap(image)
        }
    }

    fun rotate(bitmap: Bitmap, degress: Int): Bitmap {
        if (degress != 0) {
            val matrix = Matrix()
            matrix.setRotate(degress.toFloat(), (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())

            val converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            return converted
        }

        return bitmap
    }

    fun exifOrientationToDegrees(exifOrientation: Int): Int {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) return 90
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) return 180
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) return 270
        else return 0
    }

    fun gaussianFilter3(image: Bitmap): Bitmap {
        val filter = arrayOf(intArrayOf(1, 2, 1), intArrayOf(2, 4, 2), intArrayOf(1, 2, 1))
        val division = 16

        for (i in 1 until image.width - 1) {
            for (j in 1 until image.height - 1) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var alpha = 0

                for (m in -1..1) {
                    for (n in -1..1) {
                        val color = image.getPixel(i + m, j + n)
                        if (m == 0 && n == 0) alpha = (color shr 24) and 0xff
                        val r = (color shr 16) and 0xff
                        val g = (color shr 8) and 0xff
                        val b = (color) and 0xff

                        sumR += filter[m + 1][n + 1] * r
                        sumG += filter[m + 1][n + 1] * g
                        sumB += filter[m + 1][n + 1] * b
                    }
                }

                val avgR = sumR / division
                val avgG = sumG / division
                val avgB = sumB / division

                val gaussianColor = (alpha shl 24) + (avgR shl 16) + (avgG shl 8) + avgB
                image.setPixel(i, j, gaussianColor)
            }
        }

        return image
    }

    fun gaussianFilter5x5(image: Bitmap): Bitmap {
        val filter = arrayOf(intArrayOf(1, 4, 7, 4, 1), intArrayOf(4, 16, 26, 16, 4), intArrayOf(7, 26, 41, 26, 7), intArrayOf(4, 16, 26, 16, 4), intArrayOf(1, 4, 7, 4, 1))
        val division = 273

        for (i in 2 until image.width - 2) {
            for (j in 2 until image.height - 2) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var alpha = 0

                for (m in -2..2) {
                    for (n in -2..2) {
                        val color = image.getPixel(i + m, j + n)
                        if (m == 0 && n == 0) alpha = (color shr 24) and 0xff
                        val r = (color shr 16) and 0xff
                        val g = (color shr 8) and 0xff
                        val b = (color) and 0xff

                        sumR += filter[m + 2][n + 2] * r
                        sumG += filter[m + 2][n + 2] * g
                        sumB += filter[m + 2][n + 2] * b
                    }
                }

                val avgR = sumR / division
                val avgG = sumG / division
                val avgB = sumB / division

                val gaussianColor = (alpha shl 24) + (avgR shl 16) + (avgG shl 8) + avgB
                image.setPixel(i, j, gaussianColor)
            }
        }

        return image
    }

    fun gaussianFilter7x7(image: Bitmap): Bitmap {
        val filter = arrayOf(intArrayOf(0, 0, 1, 2, 1, 0, 0), intArrayOf(0, 3, 13, 22, 13, 3, 0), intArrayOf(1, 13, 59, 97, 59, 13, 1),
            intArrayOf(2, 22, 97, 159, 97, 22, 2), intArrayOf(1, 13, 59, 97, 59, 13, 1), intArrayOf(0, 3, 13, 22, 13, 3, 0), intArrayOf(0, 0, 1, 2, 1, 0, 0))
        val division = 1003

        for (i in 3 until image.width - 3) {
            for (j in 3 until image.height - 3) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var alpha = 0

                for (m in -3..3) {
                    for (n in -3..3) {
                        val color = image.getPixel(i + m, j + n)
                        if (m == 0 && n == 0) alpha = (color shr 24) and 0xff
                        val r = (color shr 16) and 0xff
                        val g = (color shr 8) and 0xff
                        val b = (color) and 0xff

                        sumR += filter[m + 3][n + 3] * r
                        sumG += filter[m + 3][n + 3] * g
                        sumB += filter[m + 3][n + 3] * b
                    }
                }

                val avgR = sumR / division
                val avgG = sumG / division
                val avgB = sumB / division

                val gaussianColor = (alpha shl 24) + (avgR shl 16) + (avgG shl 8) + avgB
                image.setPixel(i, j, gaussianColor)
            }
        }

        return image
    }

    fun blur(image: Bitmap) {
        val gaussianImage = gaussianFilter7x7(image)
        imageView.setImageBitmap(gaussianImage)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertToBlackAndWhite(image: Bitmap) {
        val config = image.config
        val newImage = image.copy(config, true)

        for (i in 0 until image.width) {
            for (j in 0 until image.height) {
                val pixel = image.getPixel(i, j)
                val luminance = pixel.luminance
                val colorInt = Color.argb((pixel.alpha / 255).toFloat(), luminance, luminance, luminance)

                newImage.setPixel(i, j, colorInt)
            }
        }

        imageView.setImageBitmap(newImage)
    }
}