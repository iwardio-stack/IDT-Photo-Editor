package com.idt.photoeditor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private val Lime = Color(0xFF76FF03)
private val Black = Color(0xFF050505)
private val Panel = Color(0xFF171717)
private val Panel2 = Color(0xFF222222)
private val White = Color.White
private val Grey = Color(0xFF999999)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IDTPhotoEditorApp()
        }
    }
}

@Composable
fun IDTPhotoEditorApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            background = Black,
            surface = Panel,
            onPrimary = Black,
            onBackground = White,
            onSurface = White
        )
    ) {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val context = LocalContext.current

    var screen by remember { mutableStateOf("home") }
    var photos by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var sharpness by remember { mutableFloatStateOf(0f) }

    var compareOriginal by remember { mutableStateOf(false) }
    var watermarkOn by remember { mutableStateOf(true) }
    var watermarkSize by remember { mutableFloatStateOf(0.18f) }
    var watermarkOpacity by remember { mutableFloatStateOf(0.85f) }
    var watermarkPosition by remember { mutableStateOf("Bottom Right") }

    var captionInput by remember { mutableStateOf("") }
    var captionOutput by remember { mutableStateOf("") }
    var captionLanguage by remember { mutableStateOf("English") }
    var captionTone by remember { mutableStateOf("Deep") }
    var captionPlatform by remember { mutableStateOf("Instagram") }

    fun resetAdjustments() {
        brightness = 0f
        contrast = 1f
        saturation = 1f
        sharpness = 0f
        compareOriginal = false
    }

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photos = listOf(uri)
            selectedIndex = 0
            resetAdjustments()
            screen = "editor"
        }
    }

    val batchPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photos = uris
            selectedIndex = 0
            resetAdjustments()
            screen = "editor"
        }
    }

    BackHandler(screen != "home") {
        screen = "home"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        when (screen) {
            "home" -> HomeScreen(
                onEdit = { singlePicker.launch("image/*") },
                onBatch = { batchPicker.launch("image/*") },
                onAuto = { singlePicker.launch("image/*") },
                onCaption = { screen = "caption" },
                onWatermark = { singlePicker.launch("image/*") },
                onMyEdits = {
                    Toast.makeText(
                        context,
                        "Your saved photos are in Gallery > IDT Photography",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            "editor" -> EditorScreen(
                photos = photos,
                selectedIndex = selectedIndex,
                onSelectedIndex = {
                    selectedIndex = it
                    resetAdjustments()
                },
                brightness = brightness,
                onBrightness = { brightness = it },
                contrast = contrast,
                onContrast = { contrast = it },
                saturation = saturation,
                onSaturation = { saturation = it },
                sharpness = sharpness,
                onSharpness = { sharpness = it },
                compareOriginal = compareOriginal,
                onCompare = { compareOriginal = it },
                watermarkOn = watermarkOn,
                onWatermarkOn = { watermarkOn = it },
                watermarkSize = watermarkSize,
                onWatermarkSize = { watermarkSize = it },
                watermarkOpacity = watermarkOpacity,
                onWatermarkOpacity = { watermarkOpacity = it },
                watermarkPosition = watermarkPosition,
                onWatermarkPosition = { watermarkPosition = it },
                onReset = { resetAdjustments() },
                onSave = { bitmap ->
                    savePhoto(
                        context,
                        bitmap,
                        watermarkOn,
                        watermarkSize,
                        watermarkOpacity,
                        watermarkPosition
                    )
                },
                onSaveAll = {
                    Toast.makeText(
                        context,
                        "Save photos individually using SAVE PHOTO.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onBack = { screen = "home" }
            )

            "caption" -> CaptionScreen(
                input = captionInput,
                onInput = { captionInput = it },
                output = captionOutput,
                language = captionLanguage,
                onLanguage = { captionLanguage = it },
                tone = captionTone,
                onTone = { captionTone = it },
                platform = captionPlatform,
                onPlatform = { captionPlatform = it },
                onGenerate = {
                    captionOutput = createCaption(
                        captionInput,
                        captionLanguage,
                        captionTone,
                        captionPlatform
                    )
                },
                onBack = { screen = "home" }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    onEdit: () -> Unit,
    onBatch: () -> Unit,
    onAuto: () -> Unit,
    onCaption: () -> Unit,
    onWatermark: () -> Unit,
    onMyEdits: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        Text(
            "I.D.T.",
            color = Lime,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "PHOTOGRAPHY",
            color = White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Text(
            "PHOTO EDIT  •  ENHANCE  •  CREATE",
            color = Grey,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(28.dp))

        val tiles = listOf(
            HomeTileData("EDIT PHOTO", "Single photo", "▣", onEdit),
            HomeTileData("BATCH EDIT", "Multiple photos", "▤", onBatch),
            HomeTileData("AUTO ENHANCE", "Quick improvements", "✦", onAuto),
            HomeTileData("CAPTION & POST", "Social media captions", "T", onCaption),
            HomeTileData("ADD WATERMARK", "Use IDT logo", "◈", onWatermark),
            HomeTileData("MY EDITS", "Saved photographs", "▣", onMyEdits)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(570.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(tiles) { tile ->
                DashboardTile(tile)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "CAPTURING MOMENTS. CREATING MEMORIES.",
            color = Lime,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(25.dp))
    }
}

private data class HomeTileData(
    val title: String,
    val description: String,
    val symbol: String,
    val action: () -> Unit
)

@Composable
private fun DashboardTile(data: HomeTileData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Color(0xFF303030), RoundedCornerShape(18.dp))
            .clickable { data.action() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            data.symbol,
            color = Lime,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            data.title,
            color = Lime,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(5.dp))
        Text(
            data.description,
            color = White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

## Editor, controls, watermark and caption screens

```kotlin
@Composable
private fun EditorScreen(
    photos: List<android.net.Uri>,
    selectedIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    contrast: Float,
    onContrast: (Float) -> Unit,
    saturation: Float,
    onSaturation: (Float) -> Unit,
    sharpness: Float,
    onSharpness: (Float) -> Unit,
    compareOriginal: Boolean,
    onCompare: (Boolean) -> Unit,
    watermarkOn: Boolean,
    onWatermarkOn: (Boolean) -> Unit,
    watermarkSize: Float,
    onWatermarkSize: (Float) -> Unit,
    watermarkOpacity: Float,
    onWatermarkOpacity: (Float) -> Unit,
    watermarkPosition: String,
    onWatermarkPosition: (String) -> Unit,
    onReset: () -> Unit,
    onSave: (Bitmap) -> Unit,
    onSaveAll: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uri = photos.getOrNull(selectedIndex)
    var original by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        original = withContext(Dispatchers.IO) {
            try {
                uri?.let {
                    context.contentResolver.openInputStream(it)?.use(BitmapFactory::decodeStream)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val edited = remember(original, brightness, contrast, saturation, sharpness) {
        original?.let {
            processPhoto(it, brightness, contrast, saturation, sharpness)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Lime, fontSize = 32.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text("PHOTO EDITOR", color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            Text("RESET", color = Lime, modifier = Modifier.clickable { onReset() })
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Panel),
            contentAlignment = Alignment.Center
        ) {
            if (original != null) {
                Image(
                    bitmap = (if (compareOriginal) original else edited ?: original!!).asImageBitmap(),
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (!compareOriginal && watermarkOn) {
                    ActualLogoWatermark(watermarkPosition, watermarkSize, watermarkOpacity)
                }

                Text(
                    if (compareOriginal) "ORIGINAL" else "EDITED",
                    color = Lime,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            } else {
                CircularProgressIndicator(color = Lime)
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onCompare(!compareOriginal) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Panel2)
        ) {
            Text(if (compareOriginal) "SHOW EDITED PHOTO" else "BEFORE / AFTER", color = Lime)
        }

        if (photos.size > 1) {
            Text("PHOTO ${selectedIndex + 1} / ${photos.size}", color = Lime)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (selectedIndex > 0) onSelectedIndex(selectedIndex - 1) },
                    enabled = selectedIndex > 0
                ) { Text("PREVIOUS") }

                OutlinedButton(
                    onClick = { if (selectedIndex < photos.lastIndex) onSelectedIndex(selectedIndex + 1) },
                    enabled = selectedIndex < photos.lastIndex
                ) { Text("NEXT") }
            }
        }

        Spacer(Modifier.height(15.dp))

        Text("PHOTO ADJUSTMENTS", color = Lime, fontWeight = FontWeight.Bold)

        EditorSlider("Brightness", brightness, -100f, 100f, onBrightness)
        EditorSlider("Contrast", contrast, 0.5f, 2f, onContrast)
        EditorSlider("Saturation", saturation, 0f, 2f, onSaturation)
        EditorSlider("Sharpness", sharpness, 0f, 1f, onSharpness)

        Button(
            onClick = {
                onBrightness(8f)
                onContrast(1.1f)
                onSaturation(1.1f)
                onSharpness(0.25f)
                onCompare(false)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Panel2)
        ) {
            Text("✦ AUTO ENHANCE", color = Lime)
        }

        Spacer(Modifier.height(12.dp))

        Text("IDT LOGO WATERMARK", color = Lime, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Watermark", color = White, modifier = Modifier.weight(1f))
            Switch(
                checked = watermarkOn,
                onCheckedChange = onWatermarkOn,
                colors = SwitchDefaults.colors(checkedThumbColor = Lime)
            )
        }

        if (watermarkOn) {
            Text("Position", color = White)

            listOf("Top Left", "Top Right", "Bottom Left", "Bottom Right").forEach { pos ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWatermarkPosition(pos) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = watermarkPosition == pos,
                        onClick = { onWatermarkPosition(pos) },
                        colors = RadioButtonDefaults.colors(selectedColor = Lime)
                    )
                    Text(pos, color = White)
                }
            }

            EditorSlider("Logo Size", watermarkSize, 0.08f, 0.35f, onWatermarkSize)
            EditorSlider("Logo Opacity", watermarkOpacity, 0.1f, 1f, onWatermarkOpacity)
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { edited?.let(onSave) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Lime)
        ) {
            Text("SAVE PHOTO", color = Black, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row {
            Text(label, color = White, modifier = Modifier.weight(1f))
            Text("%.2f".format(value), color = Lime)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = Lime,
                activeTrackColor = Lime,
                inactiveTrackColor = Panel2
            )
        )
    }
}

@Composable
private fun ActualLogoWatermark(
    position: String,
    size: Float,
    opacity: Float
) {
    val context = LocalContext.current
    var logo by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        logo = withContext(Dispatchers.IO) {
            try {
                context.assets.open("idt_logo.png").use(BitmapFactory::decodeStream)
            } catch (_: Exception) {
                null
            }
        }
    }

    val align = when (position) {
        "Top Left" -> Alignment.TopStart
        "Top Right" -> Alignment.TopEnd
        "Bottom Left" -> Alignment.BottomStart
        else -> Alignment.BottomEnd
    }

    logo?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "IDT logo watermark",
            modifier = Modifier
                .align(align)
                .padding(12.dp)
                .fillMaxWidth(size),
            alpha = opacity,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun CaptionScreen(
    input: String,
    onInput: (String) -> Unit,
    output: String,
    language: String,
    onLanguage: (String) -> Unit,
    tone: String,
    onTone: (String) -> Unit,
    platform: String,
    onPlatform: (String) -> Unit,
    onGenerate: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Lime, fontSize = 32.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text("CAPTION & POST", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
        Text("LANGUAGE", color = Lime, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("English", "Afrikaans").forEach {
                ChoiceButton(it, language == it) { onLanguage(it) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("TONE", color = Lime, fontWeight = FontWeight.Bold)

        Column {
            listOf("Deep", "Raw", "Relaxed", "Funny", "Poetic").forEach {
                ChoiceButton(it, tone == it) { onTone(it) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("PLATFORM", color = Lime, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Instagram", "Facebook", "WhatsApp").forEach {
                ChoiceButton(it, platform == it) { onPlatform(it) }
            }
        }

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Describe your photo or message") },
            minLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lime,
                unfocusedBorderColor = Grey,
                focusedTextColor = White,
                unfocusedTextColor = White,
                cursorColor = Lime
            )
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Lime)
        ) {
            Text("GENERATE CAPTION", color = Black, fontWeight = FontWeight.Bold)
        }

        if (output.isNotBlank()) {
            Spacer(Modifier.height(15.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(15.dp)) {
                    Text("YOUR CAPTION", color = Lime, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(output, color = White)
                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("Caption", output)
                            )
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Lime)
                    ) {
                        Text("COPY", color = Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(9.dp),
        color = if (selected) Lime else Panel2
    ) {
        Text(
            label,
            color = if (selected) Black else White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
