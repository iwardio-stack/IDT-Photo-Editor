package com.idt.photoeditor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

private val Lime = Color(0xFF39FF14)
private val Dark = Color(0xFF101010)
private val Panel = Color(0xFF1C1C1C)

data class PhotoEntry(
    val uri: Uri,
    val original: Bitmap,
    var edited: Bitmap
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IDTPhotoApp(this)
        }
    }
}

@Composable
fun IDTPhotoApp(activity: ComponentActivity) {

    val context = activity
    val photos = remember { mutableStateListOf<PhotoEntry>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf("home") }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }

    var watermark by remember { mutableStateOf(false) }
    var watermarkSize by remember { mutableFloatStateOf(0.18f) }
    var watermarkCorner by remember { mutableStateOf("Bottom Right") }

    var compare by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->

        photos.clear()

        uris.forEach { uri ->
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }

                if (bitmap != null) {
                    photos.add(
                        PhotoEntry(
                            uri = uri,
                            original = bitmap,
                            edited = bitmap
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        if (photos.isNotEmpty()) {
            currentIndex = 0
            screen = "editor"
            brightness = 0f
            contrast = 1f
            saturation = 1f
            statusText = ""
        }
    }

    fun applyAdjustments() {
        if (photos.isEmpty()) return

        photos.forEach { photo ->
            photo.edited = adjustBitmap(
                photo.original,
                brightness,
                contrast,
                saturation
            )
        }

        statusText = "Adjustments applied"
    }

    fun resetAdjustments() {
        brightness = 0f
        contrast = 1f
        saturation = 1f

        photos.forEach {
            it.edited = it.original
        }

        statusText = "Photo reset to original"
    }

    fun savePhoto(photo: PhotoEntry) {
        try {
            val finalBitmap = if (watermark) {
                addWatermark(
                    context,
                    photo.edited,
                    watermarkSize,
                    watermarkCorner
                )
            } else {
                photo.edited
            }

            val filename = "IDT_Photo_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    finalBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        output
                    )
                }

                statusText = "Saved to Pictures/IDT Photography"
            } else {
                statusText = "Could not save photo"
            }

        } catch (e: Exception) {
            statusText = "Save failed: ${e.message}"
        }
    }

    fun saveAll() {
        photos.forEach { savePhoto(it) }
        statusText = "${photos.size} photos saved to your gallery"
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            background = Dark,
            surface = Panel
        )
    ) {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Dark
        ) {

            if (screen == "home") {

                HomeDashboard(
                    onSingle = {
                    picker.launch("image/*")
                    },
                    onBatch = {
                    picker.launch("image/*")
                    }
                )

            } else {

                EditorScreen(
                    photos = photos,
                    currentIndex = currentIndex,
                    onSelectPhoto = { currentIndex = it },
                    brightness = brightness,
                    onBrightness = { brightness = it },
                    contrast = contrast,
                    onContrast = { contrast = it },
                    saturation = saturation,
                    onSaturation = { saturation = it },
                    watermark = watermark,
                    onWatermark = { watermark = it },
                    watermarkSize = watermarkSize,
                    onWatermarkSize = { watermarkSize = it },
                    watermarkCorner = watermarkCorner,
                    onWatermarkCorner = { watermarkCorner = it },
                    compare = compare,
                    onCompare = { compare = it },
                    onApply = { applyAdjustments() },
                    onReset = { resetAdjustments() },
                    onSave = {
                        if (photos.isNotEmpty()) savePhoto(photos[currentIndex])
                    },
                    onSaveAll = { saveAll() },
                    onBack = { screen = "home" },
                    statusText = statusText
                )
            }
        }
    }
}

@Composable
fun HomeDashboard(
    onSingle: () -> Unit,
    onBatch: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Center
    ) {

        Spacer(modifier = Modifier.height(45.dp))

        Text(
            text = "I.D.T.",
            color = Lime,
            fontSize = 44.sp,
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "PHOTOGRAPHY",
            color = Color.White,
            fontSize = 22.sp
        )

        Text(
            text = "PHOTO EDIT • ENHANCE • CREATE",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(35.dp))

        DashboardCard(
            title = "PHOTO ENHANCER",
            description = "Restore, sharpen and improve your photos",
            onClick = onSingle
        )

        DashboardCard(
            title = "BATCH EDIT",
            description = "Enhance multiple photos at once",
            onClick = onBatch
        )

        DashboardCard(
            title = "BEFORE & AFTER",
            description = "Compare your original and edited photos",
            onClick = onSingle
        )

        DashboardCard(
            title = "AI CAPTIONS",
            description = "Create captions and social media posts",
            onClick = onSingle
        )

        DashboardCard(
            title = "IDT WATERMARK",
            description = "Add your IDT logo to your photographs",
            onClick = onSingle
        )

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "CREATED BY I.D.T. STEELWORKS & ENGINEERING",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(25.dp))
    }
}

@Composable
fun DashboardCard(
    title: String,
    description: String,
    onClick: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .background(Panel, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(22.dp)
    ) {

        Text(
            title,
            color = Lime,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            description,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun EditorScreen(
    photos: List<PhotoEntry>,
    currentIndex: Int,
    onSelectPhoto: (Int) -> Unit,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    contrast: Float,
    onContrast: (Float) -> Unit,
    saturation: Float,
    onSaturation: (Float) -> Unit,
    watermark: Boolean,
    onWatermark: (Boolean) -> Unit,
    watermarkSize: Float,
    onWatermarkSize: (Float) -> Unit,
    watermarkCorner: String,
    onWatermarkCorner: (String) -> Unit,
    compare: Boolean,
    onCompare: (Boolean) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onSaveAll: () -> Unit,
    onBack: () -> Unit,
    statusText: String
){
    if (photos.isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No photos selected", color = Color.White)
        }
        return
    }

    val photo = photos[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹ BACK",
                color = Lime,
                modifier = Modifier.clickable { onBack() }
            )

            Text(
                "IDT PHOTO EDITOR",
                color = Lime,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(Panel, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = (if (compare) photo.original else photo.edited)
                    .asImageBitmap(),
                contentDescription = "Selected photo",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PHOTO ${currentIndex + 1} / ${photos.size}", color = Lime)

            Text(
                if (compare) "ORIGINAL" else "EDITED",
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow {
            itemsIndexed(photos) { index, item ->
                Image(
                    bitmap = item.edited.asImageBitmap(),
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(4.dp)
                        .background(Panel, RoundedCornerShape(8.dp))
                        .clickable { onSelectPhoto(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("PHOTO ADJUSTMENTS", color = Lime, fontSize = 18.sp)

        AdjustmentSlider(
            label = "Brightness",
            value = brightness,
            range = -100f..100f,
            onChange = onBrightness
        )

        AdjustmentSlider(
            label = "Contrast",
            value = contrast,
            range = 0.5f..2f,
            onChange = onContrast
        )

        AdjustmentSlider(
            label = "Colour / Saturation",
            value = saturation,
            range = 0f..2f,
            onChange = onSaturation
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Lime,
                    contentColor = Color.Black
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("APPLY")
            }

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Panel,
                    contentColor = Lime
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("RESET")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BEFORE / AFTER", color = Color.White)

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = compare,
                onCheckedChange = onCompare,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Lime,
                    checkedTrackColor = Lime.copy(alpha = 0.5f)
                )
            )
        }

        Divider(color = Lime.copy(alpha = 0.3f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("IDT WATERMARK", color = Lime)

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = watermark,
                onCheckedChange = onWatermark
            )
        }

        if (watermark) {

            AdjustmentSlider(
                label = "Watermark size",
                value = watermarkSize,
                range = 0.05f..0.4f,
                onChange = onWatermarkSize
            )

            Text("Position", color = Color.White)

            val corners = listOf(
                "Top Left",
                "Top Right",
                "Bottom Left",
                "Bottom Right"
            )

            corners.forEach { corner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = watermarkCorner == corner,
                        onClick = { onWatermarkCorner(corner) }
                    )
                    Text(corner, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Color.Black
            )
        ) {
            Text("SAVE PHOTO TO GALLERY")
        }

        if (photos.size > 1) {
            Button(
                onClick = onSaveAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Panel,
                    contentColor = Lime
                )
            ) {
                Text("SAVE ALL PHOTOS")
            }
        }

        if (statusText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusText, color = Lime)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
){
    Column {
        Text(
            "$label: ${String.format("%.2f", value)}",
            color = Color.White,
            fontSize = 14.sp
        )

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Lime,
                activeTrackColor = Lime,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}

fun adjustBitmap(
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {

    val output = Bitmap.createBitmap(
        source.width,
        source.height,
        Bitmap.Config.ARGB_8888
    )

    val pixels = IntArray(source.width * source.height)
    source.getPixels(
        pixels,
        0,
        source.width,
        0,
        0,
        source.width,
        source.height
    )

    for (i in pixels.indices) {

        val color = pixels[i]

        var r = android.graphics.Color.red(color).toFloat()
        var g = android.graphics.Color.green(color).toFloat()
        var b = android.graphics.Color.blue(color).toFloat()

        r = ((r - 128f) * contrast + 128f + brightness)
        g = ((g - 128f) * contrast + 128f + brightness)
        b = ((b - 128f) * contrast + 128f + brightness)

        val gray = 0.299f * r + 0.587f * g + 0.114f * b

        r = gray + (r - gray) * saturation
        g = gray + (g - gray) * saturation
        b = gray + (b - gray) * saturation

        pixels[i] = android.graphics.Color.argb(
            android.graphics.Color.alpha(color),
            r.toInt().coerceIn(0, 255),
            g.toInt().coerceIn(0, 255),
            b.toInt().coerceIn(0, 255)
        )
    }

    output.setPixels(
        pixels,
        0,
        source.width,
        0,
        0,
        source.width,
        source.height
    )

    return output
}

fun addWatermark(
    context: android.content.Context,
    photo: Bitmap,
    sizeFraction: Float,
    corner: String
): Bitmap {

    val result = photo.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val logo = try {
        context.assets.open("idt_logo.png").use {
            BitmapFactory.decodeStream(it)
        }
    } catch (_: Exception) {
        null
    }

    if (logo == null) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.GREEN
            textSize = photo.width * 0.05f
            alpha = 220
            isFakeBoldText = true
        }

        val x = if (corner.contains("Left")) 20f
        else photo.width - paint.measureText("I.D.T.") - 20f

        val y = if (corner.contains("Top")) paint.textSize + 20f
        else photo.height - 20f

        canvas.drawText("I.D.T.", x, y, paint)
        return result
    }

    val targetWidth = (photo.width * sizeFraction).toInt()
    val targetHeight = (targetWidth * logo.height / logo.width).toInt()

    val left = if (corner.contains("Left")) 20
    else photo.width - targetWidth - 20

    val top = if (corner.contains("Top")) 20
    else photo.height - targetHeight - 20

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 220
    }

    canvas.drawBitmap(
        logo,
        null,
        Rect(left, top, left + targetWidth, top + targetHeight),
        paint
    )

    return result
}
