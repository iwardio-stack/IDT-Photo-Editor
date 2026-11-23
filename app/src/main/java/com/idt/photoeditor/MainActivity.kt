package com.idt.photoeditor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

private val Lime = Color(0xFF39FF14)
private val Black = Color(0xFF080808)
private val Panel = Color(0xFF171717)
private val Panel2 = Color(0xFF222222)
private val White = Color.White
private val Muted = Color(0xFFAAAAAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IDTPhotoApp()
        }
    }
}

@Composable
fun IDTPhotoApp() {
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf("home") }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var selectedBatch by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var batchIndex by remember { mutableIntStateOf(0) }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var sharpness by remember { mutableFloatStateOf(0f) }

    var showBefore by remember { mutableStateOf(false) }
    var watermarkEnabled by remember { mutableStateOf(true) }
    var watermarkCorner by remember { mutableStateOf("Bottom right") }
    var watermarkSize by remember { mutableFloatStateOf(0.22f) }
    var watermarkOpacity by remember { mutableFloatStateOf(0.75f) }

    var captionText by remember { mutableStateOf("") }
    var captionLanguage by remember { mutableStateOf("English") }
    var captionStyle by remember { mutableStateOf("Natural") }

    LaunchedEffect(Unit) {
        logoBitmap = try {
            context.assets.open("idt_logo_transparent.png").use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) {
            null
        }
    }

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                if (bitmap != null) {
                    originalBitmap = bitmap
                    currentBitmap = bitmap
                    brightness = 0f
                    contrast = 1f
                    saturation = 1f
                    sharpness = 0f
                    currentScreen = "editor"
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val batchPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val list = uris.mapNotNull { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) {
                null
            }
        }
        if (list.isNotEmpty()) {
            selectedBatch = list
            batchIndex = 0
            originalBitmap = list[0]
            currentBitmap = list[0]
            currentScreen = "batch"
        }
    }

    BackHandler(currentScreen != "home") {
        currentScreen = "home"
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Black,
            surface = Panel,
            primary = Lime,
            onPrimary = Black,
            onBackground = White,
            onSurface = White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Black
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    onSingle = { singlePicker.launch("image/*") },
                    onBatch = { batchPicker.launch("image/*") },
                    onCaptions = { currentScreen = "captions" },
                    onEdits = {
                        Toast.makeText(
                            context,
                            "Your saved photos are in your Gallery.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                "editor" -> EditorScreen(
                    original = originalBitmap,
                    logo = logoBitmap,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    sharpness = sharpness,
                    showBefore = showBefore,
                    watermarkEnabled = watermarkEnabled,
                    watermarkCorner = watermarkCorner,
                    watermarkSize = watermarkSize,
                    watermarkOpacity = watermarkOpacity,
                    onBrightness = { brightness = it },
                    onContrast = { contrast = it },
                    onSaturation = { saturation = it },
                    onSharpness = { sharpness = it },
                    onBefore = { showBefore = it },
                    onWatermark = { watermarkEnabled = it },
                    onCorner = { watermarkCorner = it },
                    onSize = { watermarkSize = it },
                    onOpacity = { watermarkOpacity = it },
                    onHome = { currentScreen = "home" },
                    onSave = {
                        val source = originalBitmap
                        if (source != null) {
                            val edited = processBitmap(
                                source, brightness, contrast, saturation, sharpness
                            )
                            val finalBitmap = if (watermarkEnabled && logoBitmap != null) {
                                applyWatermark(
                                    edited, logoBitmap!!, watermarkCorner,
                                    watermarkSize, watermarkOpacity
                                )
                            } else edited

                            saveBitmap(context, finalBitmap)
                        }
                    }
                )

                "batch" -> BatchScreen(
                    photos = selectedBatch,
                    index = batchIndex,
                    logo = logoBitmap,
                    onIndex = { newIndex ->
                        batchIndex = newIndex
                        originalBitmap = selectedBatch[newIndex]
                        currentBitmap = selectedBatch[newIndex]
                    },
                    onHome = { currentScreen = "home" },
                    onEditCurrent = {
                        originalBitmap = selectedBatch[batchIndex]
                        currentScreen = "editor"
                    }
                )

                "captions" -> CaptionScreen(
                    text = captionText,
                    language = captionLanguage,
                    style = captionStyle,
                    onText = { captionText = it },
                    onLanguage = { captionLanguage = it },
                    onStyle = { captionStyle = it },
                    onHome = { currentScreen = "home" }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onSingle: () -> Unit,
    onBatch: () -> Unit,
    onCaptions: () -> Unit,
    onEdits: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            "IDT",
            color = Lime,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "PHOTOGRAPHY",
            color = White,
            fontSize = 14.sp,
            letterSpacing = 5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "PHOTO EDITOR",
            color = Muted,
            fontSize = 13.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "YOUR PHOTOS.\nYOUR STYLE.",
            color = White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Enhance, edit and make every moment yours.",
            color = Muted,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            item {
                DashboardCard(
                    icon = "✦",
                    title = "Single Photo",
                    description = "Edit one photo",
                    onClick = onSingle
                )
            }
            item {
                DashboardCard(
                    icon = "▣",
                    title = "Batch Photos",
                    description = "Select multiple photos",
                    onClick = onBatch
                )
            }
            item {
                DashboardCard(
                    icon = "✧",
                    title = "Auto Enhance",
                    description = "Use quick edit presets",
                    onClick = onSingle
                )
            }
            item {
                DashboardCard(
                    icon = "Aa",
                    title = "Caption & Post",
                    description = "Create captions",
                    onClick = onCaptions
                )
            }
            item {
                DashboardCard(
                    icon = "◈",
                    title = "Watermark",
                    description = "Logo controls in editor",
                    onClick = onSingle
                )
            }
            item {
                DashboardCard(
                    icon = "▤",
                    title = "My Edits",
                    description = "View saved photos",
                    onClick = onEdits
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "IDT PHOTOGRAPHY  •  CAPTURING MOMENTS",
            color = Lime,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DashboardCard(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .background(Panel, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF303030), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            icon,
            color = Lime,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Column {
            Text(
                title,
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun EditorScreen(
    original: Bitmap?,
    logo: Bitmap?,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpness: Float,
    showBefore: Boolean,
    watermarkEnabled: Boolean,
    watermarkCorner: String,
    watermarkSize: Float,
    watermarkOpacity: Float,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onSharpness: (Float) -> Unit,
    onBefore: (Boolean) -> Unit,
    onWatermark: (Boolean) -> Unit,
    onCorner: (String) -> Unit,
    onSize: (Float) -> Unit,
    onOpacity: (Float) -> Unit,
    onHome: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    val edited = remember(original, brightness, contrast, saturation, sharpness) {
        original?.let {
            processBitmap(it, brightness, contrast, saturation, sharpness)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "← BACK",
                color = Lime,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onHome() }
            )
            Text(
                "PHOTO EDITOR",
                color = White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(14.dp))

        if (original != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 440.dp)
                    .background(Panel, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = (if (showBefore) original else edited ?: original).asImageBitmap(),
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { onBefore(!showBefore) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime)
        ) {
            Text(if (showBefore) "SHOW EDITED" else "BEFORE / ORIGINAL")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                onBrightness(0.05f)
                onContrast(1.12f)
                onSaturation(1.12f)
                onSharpness(0.2f)
                onBefore(false)
                Toast.makeText(context, "Enhance preset applied", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Black
            )
        ) {
            Text("✦  AUTO ENHANCE", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        Text("ADJUSTMENTS", color = Lime, fontWeight = FontWeight.Bold)
        AdjustmentSlider("Brightness", brightness, -0.5f, 0.5f, onBrightness)
        AdjustmentSlider("Contrast", contrast, 0.5f, 1.8f, onContrast)
        AdjustmentSlider("Saturation", saturation, 0f, 2f, onSaturation)
        AdjustmentSlider("Sharpness", sharpness, 0f, 1f, onSharpness)

        Spacer(Modifier.height(12.dp))

        Text("WATERMARK", color = Lime, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use IDT logo", color = White)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = watermarkEnabled,
                onCheckedChange = onWatermark,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Lime,
                    checkedTrackColor = Color(0xFF315B24)
                )
            )
        }

        if (watermarkEnabled) {
            Text("Position", color = White, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Top left", "Top right", "Bottom left", "Bottom right").forEach { corner ->
                    FilterChip(
                        selected = watermarkCorner == corner,
                        onClick = { onCorner(corner) },
                label = { Text(corner, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Lime,
                            selectedLabelColor = Black
                        )
                    )
                }
            }

            AdjustmentSlider("Logo size", watermarkSize, 0.08f, 0.45f, onSize)
            AdjustmentSlider("Logo opacity", watermarkOpacity, 0.1f, 1f, onOpacity)

            if (logo == null) {
                Text(
                    "Logo asset not loaded. Check assets folder.",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Black
            )
        ) {
            Text("SAVE HD PHOTO", fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AdjustmentSlider(
    label: String,
    value: Float,
    minimum: Float,
    maximum: Float,
    onValue: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = White, fontSize = 13.sp)
            Text(
                String.format("%.2f", value),
                color = Lime,
                fontSize = 12.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = minimum..maximum,
            colors = SliderDefaults.colors(
                thumbColor = Lime,
                activeTrackColor = Lime,
                inactiveTrackColor = Panel2
            )
        )
    }
}

@Composable
fun BatchScreen(
    photos: List<Bitmap>,
    index: Int,
    logo: Bitmap?,
    onIndex: (Int) -> Unit,
    onHome: () -> Unit,
    onEditCurrent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "← BACK",
            color = Lime,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onHome() }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "BATCH PHOTOS",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "${photos.size} photos selected",
            color = Muted
        )

        Spacer(Modifier.height(12.dp))

        if (photos.isNotEmpty()) {
            Image(
                bitmap = photos[index].asImageBitmap(),
                contentDescription = "Selected photo ${index + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { if (index > 0) onIndex(index - 1) },
                enabled = index > 0
            ) {
                Text("PREVIOUS")
            }

            Text(
                "${index + 1} / ${photos.size}",
                color = Lime,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            OutlinedButton(
                onClick = { if (index < photos.lastIndex) onIndex(index + 1) },
                enabled = index < photos.lastIndex
            ) {
                Text("NEXT")
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onEditCurrent,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Black
            )
        ) {
            Text("EDIT THIS PHOTO", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun CaptionScreen(
    text: String,
    language: String,
    style: String,
    onText: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onStyle: (String) -> Unit,
    onHome: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "← BACK",
            color = Lime,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onHome() }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "CAPTION & POST",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(18.dp))

        Text("Language", color = White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("English", "Afrikaans").forEach {
                FilterChip(
                    selected = language == it,
                    onClick = { onLanguage(it) },
                    label = { Text(it) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Style", color = White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Natural", "Poetic", "Bold", "Funny").forEach {
                FilterChip(
                    selected = style == it,
                    onClick = { onStyle(it) },
                    label = { Text(it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onText(makeCaption(language, style)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Black
            )
        ) {
            Text("GENERATE CAPTION", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            label = { Text("Your caption") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lime,
                focusedLabelColor = Lime,
                cursorColor = Lime
            )
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                clipboard.setText(AnnotatedString(text))
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Black
            )
        ) {
            Text("COPY CAPTION")
        }

        Spacer(Modifier.height(24.dp))
    }
}

fun makeCaption(language: String, style: String): String {
    val english = when (style) {
        "Poetic" -> listOf(
            "Some moments speak quietly, but stay forever.",
            "Light, life and little pieces of forever.",
            "A memory made of light and feeling."
        ).random()

        "Bold" -> listOf(
            "Made to be remembered.",
            "No ordinary moments.",
            "This is what life looks like."
        ).random()

        "Funny" -> listOf(
            "Proof that I was outside and being productive.",
            "Professional memory collector. Amateur everything else.",
            "Camera roll doing the heavy lifting again."
        ).random()

        else -> listOf(
            "A moment worth keeping.",
            "Life lately, through my lens.",
            "Little moments. Big memories."
        ).random()
    }

    val afrikaans = when (style) {
        "Poetic" -> listOf(
            "Sommige oomblikke praat sag, maar bly vir altyd.",
            "Lig, lewe en klein stukkies ewigheid.",
            "‘n Herinnering gevang in lig en gevoel."
        ).random()

        "Bold" -> listOf(
            "Gemaak om onthou te word.",
            "Geen gewone oomblikke nie.",
            "Dis hoe die lewe lyk."
        ).random()

        "Funny" -> listOf(
            "Bewys dat ek buite was en iets gedoen het.",
            "Professionele herinnering-versamelaar.",
            "My kamera doen die harde werk."
        ).random()

        else -> listOf(
            "‘n Oomblik wat die moeite werd is om te hou.",
            "Die lewe, deur my lens.",
            "Klein oomblikke. Groot herinneringe."
        ).random()
    }

    return if (language == "Afrikaans") afrikaans else english
}

fun processBitmap(
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpness: Float
): Bitmap {
    val output = Bitmap.createBitmap(
        source.width,
        source.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val matrix = android.graphics.ColorMatrix()

    val satMatrix = android.graphics.ColorMatrix()
    satMatrix.setSaturation(saturation)

    val contrastScale = contrast
    val offset = brightness * 255f

    val adjustment = android.graphics.ColorMatrix(
        floatArrayOf(
            contrastScale, 0f, 0f, 0f, offset,
            0f, contrastScale, 0f, 0f, offset,
            0f, 0f, contrastScale, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )

    matrix.postConcat(satMatrix)
    matrix.postConcat(adjustment)

    paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)

    canvas.drawBitmap(source, 0f, 0f, paint)

    if (sharpness > 0.01f) {
        val sharpened = sharpenBitmap(output, sharpness)
        output.recycle()
        return sharpened
    }

    return output
}

fun sharpenBitmap(source: Bitmap, amount: Float): Bitmap {
    val width = source.width
    val height = source.height

    val input = IntArray(width * height)
    source.getPixels(input, 0, width, 0, 0, width, height)

    val output = input.copyOf()
    val strength = amount * 0.7f

    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val i = y * width + x

            for (channel in 0..2) {
                val shift = when (channel) {
                    0 -> 16
                    1 -> 8
                    else -> 0
                }

                val center = (input[i] shr shift) and 255
                val top = (input[i - width] shr shift) and 255
                val bottom = (input[i + width] shr shift) and 255
                val left = (input[i - 1] shr shift) and 255
                val right = (input[i + 1] shr shift) and 255

                val value = (
                    center * (1f + 4f * strength) -
                    (top + bottom + left + right) * strength
                ).toInt().coerceIn(0, 255)

                output[i] = (output[i] and (255 shl shift).inv()) or
                    (value shl shift)
            }
        }
    }

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
        it.setPixels(output, 0, width, 0, 0, width, height)
    }
}

fun applyWatermark(
    photo: Bitmap,
    logo: Bitmap,
    corner: String,
    sizeFraction: Float,
    opacity: Float
): Bitmap {
    val result = photo.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val targetWidth = (photo.width * sizeFraction).toInt()
    val targetHeight = (targetWidth.toFloat() * logo.height / logo.width).toInt()

    val margin = (photo.width * 0.03f).toInt()

    val left = when {
        corner.contains("left") -> margin
        else -> photo.width - targetWidth - margin
    }

    val top = when {
        corner.contains("Top") -> margin
        else -> photo.height - targetHeight - margin
    }

    val destination = Rect(
        left,
        top,
        left + targetWidth,
        top + targetHeight
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = (opacity * 255).toInt().coerceIn(0, 255)
    }

    canvas.drawBitmap(logo, null, destination, paint)
    return result
}

fun saveBitmap(context: android.content.Context, bitmap: Bitmap) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IDT_Edit_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/IDT Photography")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Toast.makeText(
                context,
                "Saved to Pictures/IDT Photography",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "Could not save photo", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
