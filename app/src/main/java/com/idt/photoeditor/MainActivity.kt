package com.idt.photoeditor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

private val IDTGreen = Color(0xFF39FF14)
private val IDTBlack = Color(0xFF0B0B0B)
private val IDTPanel = Color(0xFF1B1B1B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IDTApp()
        }
    }
}

@Composable
fun IDTApp() {
    val context = LocalContext.current

    var original by remember { mutableStateOf<Bitmap?>(null) }
    var edited by remember { mutableStateOf<Bitmap?>(null) }
    var showOriginal by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var watermarkEnabled by remember { mutableStateOf(false) }
    var watermarkSize by remember { mutableFloatStateOf(0.15f) }
    var watermarkPosition by remember { mutableStateOf("Bottom Right") }
    var message by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                original = bitmap
                edited = bitmap
                brightness = 0f
                contrast = 1f
                saturation = 1f
                showOriginal = false
                message = "Photo loaded"
            } catch (e: Exception) {
                message = "Could not open photo"
            }
        }
    }

    fun resetPhoto() {
        val bitmap = original
        if (bitmap != null) {
            edited = bitmap
            brightness = 0f
            contrast = 1f
            saturation = 1f
            message = "Reset complete"
        }
    }

    fun applyEdit() {
        val bitmap = original ?: return
        edited = adjustImage(bitmap, brightness, contrast, saturation)
        showOriginal = false
        message = "Adjustments applied"
    }

    fun saveImage() {
        val bitmap = edited ?: return

        try {
            val finalBitmap = if (watermarkEnabled) {
                addTextWatermark(
                    bitmap,
                    watermarkSize,
                    watermarkPosition
                )
            } else bitmap

            val filename = "IDT_Photo_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/IDT Photography"
                    )
            }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                message = "Photo saved to Pictures/IDT Photography"
            } else {
                message = "Save failed"
            }
        } catch (e: Exception) {
            message = "Save error: ${e.message}"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = IDTGreen,
            background = IDTBlack,
            surface = IDTPanel
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = IDTBlack
        ) {
            if (original == null) {
                IDTHome(
                    onOpenPhoto = { photoPicker.launch("image/*") }
                )
            } else {
                IDTEditor(
                    original = original!!,
                    edited = edited ?: original!!,
                    showOriginal = showOriginal,
                    onCompare = { showOriginal = it },
                    brightness = brightness,
                    onBrightness = { brightness = it },
                    contrast = contrast,
                    onContrast = { contrast = it },
                    saturation = saturation,
                    onSaturation = { saturation = it },
                    watermarkEnabled = watermarkEnabled,
                    onWatermark = { watermarkEnabled = it },
                    watermarkSize = watermarkSize,
                    onWatermarkSize = { watermarkSize = it },
                    watermarkPosition = watermarkPosition,
                    onWatermarkPosition = { watermarkPosition = it },
                    onApply = { applyEdit() },
                    onReset = { resetPhoto() },
                    onSave = { saveImage() },
                    onNewPhoto = { photoPicker.launch("image/*") },
                    message = message
                )
            }
        }
    }
}

@Composable
fun IDTHome(onOpenPhoto: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IDTBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.Center
    ) {
        Spacer(Modifier.height(35.dp))

        Text(
            "I.D.T.",
            fontSize = 42.sp,
            color = IDTGreen
        )
        Text(
            "PHOTOGRAPHY",
            fontSize = 22.sp,
            color = Color.White
        )
        Text(
            "PHOTO EDIT • ENHANCE • CREATE",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(35.dp))

        DashboardTile(
            "PHOTO ENHANCER",
            "Restore, sharpen and improve your photos",
            onOpenPhoto
        )
        DashboardTile(
            "BATCH EDIT",
            "Select photos to edit",
            onOpenPhoto
        )
        DashboardTile(
            "BEFORE & AFTER",
            "Compare original and edited photos",
            onOpenPhoto
        )
        DashboardTile(
            "AI CAPTIONS",
            "Caption tools coming with AI connection",
            onOpenPhoto
        )
        DashboardTile(
            "IDT WATERMARK",
            "Add IDT branding to your photographs",
            onOpenPhoto
        )

        Spacer(Modifier.height(45.dp))

        Text(
            "CREATED BY I.D.T. STEELWORKS & ENGINEERING",
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun DashboardTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .background(IDTPanel, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(22.dp)
    ) {
        Text(title, color = IDTGreen, fontSize = 19.sp)
        Spacer(Modifier.height(10.dp))
        Text(subtitle, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
fun IDTEditor(
    original: Bitmap,
    edited: Bitmap,
    showOriginal: Boolean,
    onCompare: (Boolean) -> Unit,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    contrast: Float,
    onContrast: (Float) -> Unit,
    saturation: Float,
    onSaturation: (Float) -> Unit,
    watermarkEnabled: Boolean,
    onWatermark: (Boolean) -> Unit,
    watermarkSize: Float,
    onWatermarkSize: (Float) -> Unit,
    watermarkPosition: String,
    onWatermarkPosition: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onNewPhoto: () -> Unit,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IDTBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("‹ BACK TO PHOTOS", color = IDTGreen,
            modifier = Modifier.clickable { onNewPhoto() })

        Spacer(Modifier.height(14.dp))

        Text("IDT PHOTO EDITOR", color = IDTGreen, fontSize = 22.sp)

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(IDTPanel, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = (if (showOriginal) original else edited).asImageBitmap(),
                contentDescription = "Photo preview",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("BEFORE / AFTER", color = Color.White)
            Switch(
                checked = showOriginal,
                onCheckedChange = onCompare
            )
        }

        Spacer(Modifier.height(15.dp))

        Text("PHOTO ADJUSTMENTS", color = IDTGreen, fontSize = 18.sp)

        PhotoSlider("Brightness", brightness, -100f..100f, onBrightness)
        PhotoSlider("Contrast", contrast, 0.5f..2f, onContrast)
        PhotoSlider("Saturation", saturation, 0f..2f, onSaturation)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IDTGreen,
                    contentColor = Color.Black
                )
            ) {
                Text("APPLY")
            }

            Button(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IDTPanel,
                    contentColor = IDTGreen
                )
            ) {
                Text("RESET")
            }
        }

        Spacer(Modifier.height(15.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("IDT WATERMARK", color = Color.White)
            Switch(
                checked = watermarkEnabled,
                onCheckedChange = onWatermark
            )
        }

        if (watermarkEnabled) {
            PhotoSlider(
                "Watermark size",
                watermarkSize,
                0.05f..0.40f,
                onWatermarkSize
            )

            Text("Position", color = IDTGreen)

            listOf(
                "Top Left",
                "Top Right",
                "Bottom Left",
                "Bottom Right"
            ).forEach { position ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = watermarkPosition == position,
                        onClick = { onWatermarkPosition(position) }
                    )
                    Text(position, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = IDTGreen,
                contentColor = Color.Black
            )
        ) {
            Text("SAVE PHOTO TO GALLERY")
        }

        Button(
            onClick = onNewPhoto,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = IDTPanel,
                contentColor = IDTGreen
            )
        ) {
            Text("OPEN ANOTHER PHOTO")
        }

        Spacer(Modifier.height(10.dp))
        Text(message, color = IDTGreen)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun PhotoSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(
            "$label: ${"%.2f".format(value)}",
            color = Color.White,
            fontSize = 14.sp
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = IDTGreen,
                activeTrackColor = IDTGreen
            )
        )
    }
}

fun adjustImage(
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {
    val result = Bitmap.createBitmap(
        source.width,
        source.height,
        Bitmap.Config.ARGB_8888
    )

    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

    for (i in pixels.indices) {
        val c = pixels[i]

        var r = android.graphics.Color.red(c).toFloat()
        var g = android.graphics.Color.green(c).toFloat()
        var b = android.graphics.Color.blue(c).toFloat()

        r = ((r - 128f) * contrast + 128f + brightness)
        g = ((g - 128f) * contrast + 128f + brightness)
        b = ((b - 128f) * contrast + 128f + brightness)

        val gray = 0.299f * r + 0.587f * g + 0.114f * b

        r = gray + (r - gray) * saturation
        g = gray + (g - gray) * saturation
        b = gray + (b - gray) * saturation

        pixels[i] = android.graphics.Color.argb(
            android.graphics.Color.alpha(c),
            r.toInt().coerceIn(0, 255),
            g.toInt().coerceIn(0, 255),
            b.toInt().coerceIn(0, 255)
        )
    }

    result.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    return result
}

fun addTextWatermark(
    bitmap: Bitmap,
    sizeFraction: Float,
    position: String
): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.GREEN
        alpha = 220
        textSize = bitmap.width * sizeFraction
        isFakeBoldText = true
    }

    val text = "I.D.T."
    val padding = bitmap.width * 0.03f

    val textWidth = paint.measureText(text)
    val left = if (position.contains("Left")) padding
    else bitmap.width - textWidth - padding

    val baseline = if (position.contains("Top")) {
        paint.textSize + padding
    } else {
        bitmap.height - padding
    }

    canvas.drawText(text, left, baseline, paint)
    return result
}
