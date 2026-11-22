
package com.idt.photoeditor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BG = Color(0xFF0B0B0B)
private val PANEL = Color(0xFF191919)
private val LIME = Color(0xFFB7FF00)
private val WHITE = Color(0xFFF5F5F5)
private val MUTED = Color(0xFF929292)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IDTPhotoApp { bitmap -> savePhoto(bitmap) }
        }
    }

    private fun savePhoto(bitmap: Bitmap) {
        try {
            val filename = "IDT_EDIT_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/IDT Photo Editor"
                )
            }

            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }

                Toast.makeText(
                    this,
                    "Saved to Pictures/IDT Photo Editor",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Could not save photo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Save failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
fun IDTPhotoApp(onSave: (Bitmap) -> Unit) {
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var edited by remember { mutableStateOf<Bitmap?>(null) }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var sharpen by remember { mutableFloatStateOf(0f) }

    var showOriginal by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    original = bitmap
                    edited = bitmap
                    brightness = 0f
                    contrast = 1f
                    saturation = 1f
                    sharpen = 0f
                    showOriginal = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Re-process the original image whenever an adjustment changes.
    LaunchedEffect(original, brightness, contrast, saturation, sharpen) {
        val source = original
        if (source != null) {
            busy = true
            edited = withContext(Dispatchers.Default) {
                adjustBitmap(
                    source,
                    brightness,
                    contrast,
                    saturation,
                    sharpen
                )
            }
            busy = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BG
    ) {
        if (original == null) {
            HomeScreen {
                picker.launch("image/*")
            }
        } else {
            EditorScreen(
                original = original!!,
                edited = edited,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                sharpen = sharpen,
                showOriginal = showOriginal,
                busy = busy,
                onBrightness = { brightness = it },
                onContrast = { contrast = it },
                onSaturation = { saturation = it },
                onSharpen = { sharpen = it },
                onCompare = { showOriginal = !showOriginal },
                onReset = {
                    brightness = 0f
                    contrast = 1f
                    saturation = 1f
                    sharpen = 0f
                    showOriginal = false
                },
                onAutoEnhance = {
                    val settings = autoEnhanceSettings(original!!)
                    brightness = settings.first
                    contrast = settings.second
                    saturation = settings.third
                    sharpen = 0.35f
                    showOriginal = false
                },
                onBack = {
                    original = null
                    edited = null
                    showOriginal = false
                },
                onSave = {
                    edited?.let { onSave(it) }
                },
                onChoosePhoto = {
                    picker.launch("image/*")
                }
            )
        }
    }
}

@Composable
fun HomeScreen(onPhoto: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Spacer(Modifier.height(30.dp))

        Text("I.D.T.", color = LIME, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Text("PHOTOGRAPHY", color = WHITE, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("EDIT  •  ENHANCE  •  CREATE", color = MUTED, fontSize = 11.sp)

        Spacer(Modifier.height(35.dp))

        Text("YOUR CREATIVE STUDIO", color = LIME, fontWeight = FontWeight.Bold)
        Text(
            "Bring your photos to life.",
            color = WHITE,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PANEL)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("＋", fontSize = 42.sp, color = LIME)
                Text("OPEN PHOTO EDITOR", color = LIME, fontWeight = FontWeight.Bold)
                Text("Choose a photo from your gallery", color = MUTED)
            }
        }

        Spacer(Modifier.height(25.dp))

        Text("PHOTO TOOLS", color = WHITE, fontWeight = FontWeight.Bold)

        HomeTool(
            "PHOTO ENHANCER",
            "Brightness • Contrast • Saturation • Sharpen",
            onPhoto
        )

        HomeTool(
            "AUTO ENHANCE",
            "Automatically improve a photo",
            onPhoto
        )

        HomeTool(
            "BEFORE & AFTER",
            "Compare original and edited images",
            onPhoto
        )

        Spacer(Modifier.height(30.dp))

        Text(
            "I.D.T. STEELWORKS & ENGINEERING",
            color = MUTED,
            fontSize = 10.sp
        )
    }
}

@Composable
fun HomeTool(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(PANEL, RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(title, color = LIME, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(description, color = MUTED, fontSize = 12.sp)
    }
}

@Composable
fun EditorScreen(
    original: Bitmap,
    edited: Bitmap?,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpen: Float,
    showOriginal: Boolean,
    busy: Boolean,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onSharpen: (Float) -> Unit,
    onCompare: () -> Unit,
    onReset: () -> Unit,
    onAutoEnhance: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onChoosePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "‹  BACK",
            color = LIME,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "PHOTO ENHANCER",
            color = LIME,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Fine-tune your photograph", color = MUTED)

        Spacer(Modifier.height(15.dp))

        val preview = if (showOriginal) original else edited

        preview?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = if (showOriginal) "Original photograph" else "Edited photograph",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black, RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            if (showOriginal) "ORIGINAL • UNEDITED" else "EDITED PREVIEW",
            color = if (showOriginal) WHITE else LIME,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onCompare,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PANEL,
                contentColor = LIME
            )
        ) {
            Text(if (showOriginal) "SHOW EDITED PHOTO" else "SHOW ORIGINAL PHOTO")
        }

        Spacer(Modifier.height(8.dp))

        // AUTO ENHANCE
        Button(
            onClick = onAutoEnhance,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LIME,
                contentColor = BG
            )
        ) {
            Text("✨  AUTO ENHANCE", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        AdjustmentSlider(
            label = "Brightness",
            value = brightness,
            range = -100f..100f,
            display = brightness.toInt().toString(),
            onChange = onBrightness
        )

        AdjustmentSlider(
            label = "Contrast",
            value = contrast,
            range = 0.5f..1.8f,
            display = "%.2f".format(contrast),
            onChange = onContrast
        )

        AdjustmentSlider(
            label = "Saturation",
            value = saturation,
            range = 0f..2f,
            display = "%.2f".format(saturation),
            onChange = onSaturation
        )

        AdjustmentSlider(
            label = "Sharpen",
            value = sharpen,
            range = 0f..2f,
            display = "%.2f".format(sharpen),
            onChange = onSharpen
        )

        if (busy) {
            Text("Processing photo...", color = LIME)
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("RESET ALL ADJUSTMENTS", color = LIME)
        }

        Button(
            onClick = onSave,
            enabled = edited != null && !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LIME,
                contentColor = BG
            )
        ) {
            Text("SAVE EDITED PHOTO", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onChoosePhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CHOOSE ANOTHER PHOTO", color = LIME)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = WHITE, fontWeight = FontWeight.Bold)
            Text(display, color = LIME)
        }

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = LIME,
                activeTrackColor = LIME,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}

// Brightness, contrast, saturation and sharpening processing.
fun adjustBitmap(
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpen: Float
): Bitmap {
    val width = source.width
    val height = source.height
    val input = IntArray(width * height)

    source.getPixels(input, 0, width, 0, 0, width, height)

    val colorAdjusted = IntArray(input.size)
    val hsv = FloatArray(3)

    for (i in input.indices) {
        val pixel = input[i]

        var r = AColor.red(pixel).toFloat()
        var g = AColor.green(pixel).toFloat()
        var b = AColor.blue(pixel).toFloat()

        r = ((r - 128f) * contrast + 128f + brightness).coerceIn(0f, 255f)
        g = ((g - 128f) * contrast + 128f + brightness).coerceIn(0f, 255f)
        b = ((b - 128f) * contrast + 128f + brightness).coerceIn(0f, 255f)

        AColor.RGBToHSV(r.toInt(), g.toInt(), b.toInt(), hsv)
        hsv[1] = (hsv[1] * saturation).coerceIn(0f, 1f)

        colorAdjusted[i] = AColor.HSVToColor(AColor.alpha(pixel), hsv)
    }

    // Sharpening filter. At zero, the colour-adjusted image is unchanged.
    if (sharpen <= 0f) {
        return Bitmap.createBitmap(colorAdjusted, width, height, Bitmap.Config.ARGB_8888)
    }

    val amount = sharpen * 0.35f
    val output = IntArray(colorAdjusted.size)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            val center = colorAdjusted[index]

            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                output[index] = center
                continue
            }

            val top = colorAdjusted[index - width]
            val bottom = colorAdjusted[index + width]
            val left = colorAdjusted[index - 1]
            val right = colorAdjusted[index + 1]

            val rr = (AColor.red(center) * (1f + 4f * amount)
                    - AColor.red(top) * amount
                    - AColor.red(bottom) * amount
                    - AColor.red(left) * amount
                    - AColor.red(right) * amount).toInt().coerceIn(0, 255)

            val gg = (AColor.green(center) * (1f + 4f * amount)
                    - AColor.green(top) * amount
                    - AColor.green(bottom) * amount
                    - AColor.green(left) * amount
                    - AColor.green(right) * amount).toInt().coerceIn(0, 255)

            val bb = (AColor.blue(center) * (1f + 4f * amount)
                    - AColor.blue(top) * amount
                    - AColor.blue(bottom) * amount
                    - AColor.blue(left) * amount
                    - AColor.blue(right) * amount).toInt().coerceIn(0, 255)

            output[index] = AColor.argb(AColor.alpha(center), rr, gg, bb)
        }
    }

    return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}

// Estimate automatic corrections from sampled pixels.
fun autoEnhanceSettings(bitmap: Bitmap): Triple<Float, Float, Float> {
    val width = bitmap.width
    val height = bitmap.height

    var totalBrightness = 0.0
    var totalSaturation = 0.0
    var samples = 0

    val stepX = (width / 80).coerceAtLeast(1)
    val stepY = (height / 80).coerceAtLeast(1)
    val hsv = FloatArray(3)

    for (y in 0 until height step stepY) {
        for (x in 0 until width step stepX) {
            val pixel = bitmap.getPixel(x, y)

            val r = AColor.red(pixel)
            val g = AColor.green(pixel)
            val b = AColor.blue(pixel)

            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            totalBrightness += luminance

            AColor.RGBToHSV(r, g, b, hsv)
            totalSaturation += hsv[1]

            samples++
        }
    }

    if (samples == 0) return Triple(0f, 1f, 1f)

    val avgBrightness = totalBrightness / samples
    val avgSaturation = totalSaturation / samples

    // Aim toward a balanced brightness without overcorrecting.
    val brightnessAdjustment = ((128.0 - avgBrightness) * 0.30)
        .coerceIn(-35.0, 35.0).toFloat()

    // Slight contrast boost.
    val contrastAdjustment = 1.12f

    // Lift colour gently when the image is dull, while limiting oversaturation.
    val saturationAdjustment = (1.18 - avgSaturation * 0.20)
        .coerceIn(1.0, 1.25).toFloat()

    return Triple(
        brightnessAdjustment,
        contrastAdjustment,
        saturationAdjustment
    )
}
