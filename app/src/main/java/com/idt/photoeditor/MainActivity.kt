package com.idt.photoeditor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IDTPhotoEditorApp()
        }
    }
}

private val Lime = Color(0xFF76FF00)
private val Black = Color(0xFF080808)
private val Dark = Color(0xFF151515)
private val CardDark = Color(0xFF202020)
private val White = Color.White
private val Grey = Color(0xFFAAAAAA)

@Composable
fun IDTPhotoEditorApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            background = Black,
            surface = Dark,
            onPrimary = Black,
            onBackground = White,
            onSurface = White
        )
    ) {
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf("home") }
    val photos = remember { mutableStateListOf<Bitmap>() }
    var selectedPhoto by remember { mutableIntStateOf(0) }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }

    var compareMode by remember { mutableStateOf(false) }
    var watermarkEnabled by remember { mutableStateOf(false) }
    var watermarkPosition by remember { mutableStateOf("Bottom right") }

    var captionLanguage by remember { mutableStateOf("English") }
    var captionTone by remember { mutableStateOf("Deep") }
    var captionPlatform by remember { mutableStateOf("Instagram") }
    var generatedCaption by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        photos.clear()

        uris.forEach { uri ->
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()

                if (bitmap != null) photos.add(bitmap)
            } catch (_: Exception) {
            }
        }

        selectedPhoto = 0
        brightness = 0f
        contrast = 1f
        saturation = 1f

        if (photos.isNotEmpty()) currentScreen = "editor"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        when (currentScreen) {
            "home" -> HomeScreen(
                onEdit = { picker.launch("image/*") },
                onBatch = { picker.launch("image/*") },
                onCaption = { currentScreen = "caption" },
                onWatermark = { currentScreen = "watermark" }
            )

            "editor" -> EditorScreen(
                photos = photos,
                selectedPhoto = selectedPhoto,
                onPhotoSelected = { selectedPhoto = it },
                brightness = brightness,
                onBrightness = { brightness = it },
                contrast = contrast,
                onContrast = { contrast = it },
                saturation = saturation,
                onSaturation = { saturation = it },
                compareMode = compareMode,
                onCompare = { compareMode = it },
                watermarkEnabled = watermarkEnabled,
                onWatermarkEnabled = { watermarkEnabled = it },
                watermarkPosition = watermarkPosition,
                onWatermarkPosition = { watermarkPosition = it },
                onSave = {
                    if (photos.isNotEmpty()) {
                        val edited = editBitmap(
                            photos[selectedPhoto],
                            brightness,
                            contrast,
                            saturation
                        )

                        val finalBitmap = if (watermarkEnabled) {
                            applyWatermark(edited, watermarkPosition)
                        } else edited

                        val saved = saveBitmap(context, finalBitmap)

                        Toast.makeText(
                            context,
                            if (saved) "Photo saved to gallery" else "Save failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onSaveAll = {
                    var count = 0

                    photos.forEach { original ->
                        val edited = editBitmap(
                            original,
                            brightness,
                            contrast,
                            saturation
                        )

                        val finalBitmap = if (watermarkEnabled) {
                            applyWatermark(edited, watermarkPosition)
                        } else edited

                        if (saveBitmap(context, finalBitmap)) count++
                    }

                    Toast.makeText(
                        context,
                        "$count photos saved",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onCaption = { currentScreen = "caption" },
                onBack = { currentScreen = "home" }
            )

            "caption" -> CaptionScreen(
                language = captionLanguage,
                onLanguage = { captionLanguage = it },
                tone = captionTone,
                onTone = { captionTone = it },
                platform = captionPlatform,
                onPlatform = { captionPlatform = it },
                caption = generatedCaption,
                onCaption = { generatedCaption = it },
                onBack = { currentScreen = "home" }
            )

            "watermark" -> WatermarkScreen(
                enabled = watermarkEnabled,
                onEnabled = { watermarkEnabled = it },
                position = watermarkPosition,
                onPosition = { watermarkPosition = it },
                onBack = { currentScreen = "home" }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onEdit: () -> Unit,
    onBatch: () -> Unit,
    onCaption: () -> Unit,
    onWatermark: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(44.dp))

        Text(
            "I.D.T.",
            color = Lime,
            fontSize = 46.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            "PHOTOGRAPHY",
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "PHOTO EDIT • ENHANCE • CREATE",
            color = Grey,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        DashboardCard(
            "PHOTO ENHANCER",
            "Restore, sharpen and improve your photos",
            onEdit
        )

        DashboardCard(
            "BATCH EDIT",
            "Enhance multiple photos at once",
            onBatch
        )

        DashboardCard(
            "BEFORE & AFTER",
            "Compare your original and edited photos",
            onEdit
        )

        DashboardCard(
            "AI CAPTIONS",
            "Create captions and social media posts",
            onCaption
        )

        DashboardCard(
            "IDT WATERMARK",
            "Add your IDT Photography watermark",
            onWatermark
        )

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            "CREATED BY I.D.T. STEELWORKS & ENGINEERING",
            color = Grey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(25.dp))
    }
}

@Composable
fun DashboardCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                title,
                color = Lime,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(description, color = White, fontSize = 15.sp)
        }
    }
}

@Composable
fun EditorScreen(
    photos: List<Bitmap>,
    selectedPhoto: Int,
    onPhotoSelected: (Int) -> Unit,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    contrast: Float,
    onContrast: (Float) -> Unit,
    saturation: Float,
    onSaturation: (Float) -> Unit,
    compareMode: Boolean,
    onCompare: (Boolean) -> Unit,
    watermarkEnabled: Boolean,
    onWatermarkEnabled: (Boolean) -> Unit,
    watermarkPosition: String,
    onWatermarkPosition: (String) -> Unit,
    onSave: () -> Unit,
    onSaveAll: () -> Unit,
    onCaption: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Header("PHOTO EDITOR", onBack)

        if (photos.isEmpty()) {
            Text("No photos selected", color = White)
            return@Column
        }

        Text(
            "Photo ${selectedPhoto + 1} of ${photos.size}",
            color = Lime,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Color.Black, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val original = photos[selectedPhoto]

            val edited = remember(original, brightness, contrast, saturation) {
                editBitmap(original, brightness, contrast, saturation)
            }

            Image(
                bitmap = (if (compareMode) original else edited).asImageBitmap(),
                contentDescription = "Photo preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            if (watermarkEnabled && !compareMode) {
                WatermarkOverlay(watermarkPosition)
            }

            if (compareMode) {
                Text(
                    "ORIGINAL",
                    color = Lime,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Black.copy(alpha = 0.75f))
                        .padding(8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onCompare(!compareMode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (compareMode) Lime else CardDark
                )
            ) {
                Text(
                    if (compareMode) "SHOW EDITED" else "BEFORE / AFTER",
                    color = if (compareMode) Black else Lime
                )
            }

            Button(
                onClick = onCaption,
                colors = ButtonDefaults.buttonColors(containerColor = CardDark)
            ) {
                Text("CAPTION", color = Lime)
            }
        }

        if (photos.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))

            Text("SELECT PHOTO", color = Lime)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(photos) { index, bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                2.dp,
                                if (index == selectedPhoto) Lime else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onPhotoSelected(index)
                                onCompare(false)
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("PHOTO ADJUSTMENTS", color = Lime, fontWeight = FontWeight.Bold)

        AdjustmentSlider(
            label = "Brightness",
            value = brightness,
            valueRange = -100f..100f,
            onValueChange = onBrightness
        )

        AdjustmentSlider(
            label = "Contrast",
            value = contrast,
            valueRange = 0.5f..2f,
            onValueChange = onContrast
        )

        AdjustmentSlider(
            label = "Saturation",
            value = saturation,
            valueRange = 0f..2f,
            onValueChange = onSaturation
        )

        Button(
            onClick = {
                onBrightness(8f)
                onContrast(1.12f)
                onSaturation(1.08f)
                onCompare(false)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("AUTO ENHANCE", color = Lime)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("WATERMARK", color = Lime, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = watermarkEnabled,
                onCheckedChange = onWatermarkEnabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = Lime,
                    checkmarkColor = Black
                )
            )
            Text("Add IDT Photography watermark", color = White)
        }

        if (watermarkEnabled) {
            DropdownSelector(
                title = "Watermark position",
                options = listOf(
                    "Bottom right",
                    "Bottom left",
                    "Top right",
                    "Top left"
                ),
                selected = watermarkPosition,
                onSelect = onWatermarkPosition
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = Lime),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SAVE EDITED PHOTO", color = Black, fontWeight = FontWeight.Bold)
        }

        if (photos.size > 1) {
            Button(
                onClick = onSaveAll,
                colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SAVE ALL PHOTOS", color = Lime)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun AdjustmentSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = White)
            Text(String.format("%.2f", value), color = Lime)
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Lime,
                activeTrackColor = Lime,
                inactiveTrackColor = CardDark
            )
        )
    }
}

@Composable
fun WatermarkOverlay(position: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        val alignment = when (position) {
            "Bottom left" -> Alignment.BottomStart
            "Top right" -> Alignment.TopEnd
            "Top left" -> Alignment.TopStart
            else -> Alignment.BottomEnd
        }

        Text(
            "IDT PHOTOGRAPHY",
            color = Lime.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(alignment)
                .padding(12.dp)
                .background(Black.copy(alpha = 0.6f))
                .padding(6.dp)
        )
    }
}

@Composable
fun WatermarkScreen(
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    position: String,
    onPosition: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(20.dp)
    ) {
        Header("IDT WATERMARK", onBack)

        Text("WATERMARK SETTINGS", color = Lime, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = enabled,
                onCheckedChange = onEnabled,
                colors = CheckboxDefaults.colors(checkedColor = Lime)
            )
            Text("Enable watermark", color = White)
        }

        DropdownSelector(
            title = "Position",
            options = listOf(
                "Bottom right",
                "Bottom left",
                "Top right",
                "Top left"
            ),
            selected = position,
            onSelect = onPosition
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(8.dp)
            ) {
                Text(
                    "PHOTO PREVIEW",
                    color = Grey,
                    modifier = Modifier.align(Alignment.Center)
                )

                if (enabled) WatermarkOverlay(position)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Watermark settings will be applied when saving edited photos.",
            color = Grey
        )
    }
}

@Composable
fun CaptionScreen(
    language: String,
    onLanguage: (String) -> Unit,
    tone: String,
    onTone: (String) -> Unit,
    platform: String,
    onPlatform: (String) -> Unit,
    caption: String,
    onCaption: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Header("CAPTION CREATOR", onBack)

        Text("LANGUAGE", color = Lime)

        Row {
            listOf("English", "Afrikaans").forEach {
                FilterButton(it, language == it) { onLanguage(it) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("TONE", color = Lime)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Deep", "Raw", "Relaxed", "Funny", "Poetic").forEach {
                FilterButton(it, tone == it) { onTone(it) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("PLATFORM", color = Lime)

        Row {
            listOf("Instagram", "Facebook", "WhatsApp").forEach {
                FilterButton(it, platform == it) { onPlatform(it) }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { onCaption(generateCaption(language, tone, platform)) },
            colors = ButtonDefaults.buttonColors(containerColor = Lime),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GENERATE CAPTION", color = Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = onCaption,
            label = { Text("Your caption") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lime,
                focusedLabelColor = Lime,
                cursorColor = Lime,
                focusedTextColor = White,
                unfocusedTextColor = White
            )
        )

        Button(
            onClick = {
                val clipboard = context.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as android.content.ClipboardManager

                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("Caption", caption)
                )

                Toast.makeText(context, "Caption copied", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("COPY CAPTION", color = Lime)
        }
    }
}

@Composable
fun FilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(3.dp)
            .clickable { onClick() },
        color = if (selected) Lime else CardDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            label,
            color = if (selected) Black else White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun DropdownSelector(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(title, color = White)

        Box {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = CardDark)
            ) {
                Text(selected, color = Lime)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Header(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "‹",
            color = Lime,
            fontSize = 34.sp,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            title,
            color = Lime,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun generateCaption(
    language: String,
    tone: String,
    platform: String
): String {
    val english = when (tone) {
        "Raw" ->
            "Not every moment is perfect. Some are messy, real and unforgettable. And those are the moments worth keeping."

        "Relaxed" ->
            "Taking a moment, breathing it in, and letting life happen one frame at a time."

        "Funny" ->
            "Proof that I was here, the camera was working, and somehow I got the shot."

        "Poetic" ->
            "Light finds its way into the smallest moments, leaving memories where time once stood."

        else ->
            "Some moments pass quietly. A photograph gives them a way to stay."
    }

    val afrikaans = when (tone) {
        "Raw" ->
            "Nie elke oomblik is perfek nie. Party is deurmekaar, eg en onvergeetlik. En dis juis die oomblikke wat die moeite werd is om te bewaar."

        "Relaxed" ->
            "Net 'n oomblik om asem te haal, die lewe te geniet en alles een foto op 'n slag vas te vang."

        "Funny" ->
            "Bewys dat ek hier was, die kamera gewerk het en ek op een of ander manier die foto gekry het."

        "Poetic" ->
            "Lig vind sy pad na die kleinste oomblikke en laat herinneringe agter waar tyd verbygaan."

        else ->
            "Sommige oomblikke gaan stil verby. 'n Foto gee hulle 'n manier om te bly."
    }

    val text = if (language == "Afrikaans") afrikaans else english

    val hashtags = when (platform) {
        "Instagram" ->
            "\n\n#IDTPhotography #CapturedMoments #Photography #Memories"

        "Facebook" ->
            "\n\n#IDTPhotography #CapturedMoments"

        else -> ""
    }

    return text + hashtags
}

fun editBitmap(
    original: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {
    val output = Bitmap.createBitmap(
        original.width,
        original.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val matrix = android.graphics.ColorMatrix()

    val satMatrix = android.graphics.ColorMatrix().apply {
        setSaturation(saturation)
    }

    val contrastMatrix = android.graphics.ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
    )

    matrix.postConcat(satMatrix)
    matrix.postConcat(contrastMatrix)

    paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(original, 0f, 0f, paint)

    return output
}

fun applyWatermark(
    bitmap: Bitmap,
    position: String
): Bitmap {
    val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(118, 255, 0)
        textSize = (bitmap.width * 0.035f).coerceAtLeast(22f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 2f, 2f, AndroidColor.BLACK)
    }

    val text = "IDT PHOTOGRAPHY"
    val margin = bitmap.width * 0.04f

    val bounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val x = when (position) {
        "Bottom left", "Top left" -> margin
        else -> bitmap.width - bounds.width() - margin
    }

    val y = when (position) {
        "Top left", "Top right" -> margin + bounds.height()
        else -> bitmap.height - margin
    }

    canvas.drawText(text, x, y, paint)

    return output
}

fun saveBitmap(
    context: Context,
    bitmap: Bitmap
): Boolean {
    return try {
        val filename = "IDT_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/IDT Photography"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return false

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completed = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completed, null, null)
        }

        true
    } catch (_: Exception) {
        false
    }
}
