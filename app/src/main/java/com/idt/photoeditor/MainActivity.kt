package com.idt.photoeditor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream

private val Lime = Color(0xFF76FF00)
private val Bg = Color(0xFF050505)
private val SurfaceDark = Color(0xFF171717)
private val BorderDark = Color(0xFF343434)
private val TextGrey = Color(0xFFAAAAAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IDTPhotoEditorApp() }
    }
}


@Composable
private fun IDTPhotoEditorApp() {
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = SurfaceDark, primary = Lime)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            var screen by remember { mutableStateOf("home") }
            var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var batchBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
            var batchIndex by remember { mutableIntStateOf(0) }
            var openCaption by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { selectedBitmap = loadBitmap(context, it); batchBitmaps = emptyList(); screen = "editor" }
            }
            val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                val loaded = uris.mapNotNull { loadBitmap(context, it) }
                if (loaded.isNotEmpty()) { batchBitmaps = loaded; batchIndex = 0; selectedBitmap = loaded[0]; screen = "batch" }
            }
            when (screen) {
                "editor" -> EditorScreen(selectedBitmap, onBack = { screen = "home" }, onCaption = { openCaption = true })
                "batch" -> BatchScreen(batchBitmaps, batchIndex, onIndex = { batchIndex = it; selectedBitmap = batchBitmaps.getOrNull(it) }, onBack = { screen = "home" })
                else -> HomeScreen(
                    onSingle = { singlePicker.launch("image/*") },
                    onBatch = { multiPicker.launch("image/*") },
                    onAuto = { singlePicker.launch("image/*") },
                    onCaption = { openCaption = true },
                    onWatermark = { singlePicker.launch("image/*") },
                    onSaved = { Toast.makeText(context, "Saved photos are in your Gallery under Pictures/IDT Photography", Toast.LENGTH_LONG).show() }
                )
            }
            if (openCaption) CaptionDialog(onClose = { openCaption = false })
        }
    }
}


@Composable
private fun HomeScreen(
    onSingle: () -> Unit, onBatch: () -> Unit, onAuto: () -> Unit, onCaption: () -> Unit,
    onWatermark: () -> Unit, onSaved: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)) {
            LogoHeader()
            Spacer(Modifier.height(20.dp))
            Text("PHOTO EDIT", color = TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(18.dp))
            Text("YOUR PHOTOS.\nYOUR STYLE.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp)
            Spacer(Modifier.height(8.dp))
            Text("Enhance, edit and make every moment yours.", color = TextGrey, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            val cards = listOf(
                HomeCard("Edit Photo", "Single photo", Icons.Default.AddPhotoAlternate, onSingle),
                HomeCard("Batch Edit", "Multiple photos", Icons.Default.Collections, onBatch),
                HomeCard("Auto Enhance", "AI Improve & Restore", Icons.Default.AutoFixHigh, onAuto),
                HomeCard("Caption & Post", "Write for social media", Icons.Default.TextFields, onCaption),
                HomeCard("Add Watermark", "Add IDT logo to photos", Icons.Default.Brush, onWatermark),
                HomeCard("My Edits", "Saved photos", Icons.Default.PhotoLibrary, onSaved)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cards.chunked(2).forEach { rowCards ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowCards.forEach { card -> HomeCardView(card, Modifier.weight(1f)) }
                        if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("IDT PHOTOGRAPHY  •  CAPTURING MOMENTS", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
        }
    }
}

private data class HomeCard(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)

@Composable
private fun HomeCardView(card: HomeCard, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(17.dp)).background(SurfaceDark).border(1.dp, BorderDark, RoundedCornerShape(17.dp)).clickable { card.action() }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Icon(card.icon, contentDescription = card.title, tint = Lime, modifier = Modifier.size(33.dp))
        Column {
            Text(card.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(card.subtitle, color = TextGrey, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}


@Composable
private fun LogoHeader() {
    val context = LocalContext.current
    val logo = remember { loadLogo(context) }
    Column(horizontalAlignment = Alignment.Start) {
        if (logo != null) {
            Image(bitmap = logo.asImageBitmap(), contentDescription = "IDT Photography Logo", modifier = Modifier.size(108.dp))
        } else {
            Text("IDT", color = Lime, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Text("P H O T O G R A P H Y", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("PHOTO EDIT", color = TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}


@Composable
private fun EditorScreen(bitmap: Bitmap?, onBack: () -> Unit, onCaption: () -> Unit) {
    val context = LocalContext.current
    var before by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var sharpness by remember { mutableFloatStateOf(0f) }
    var autoMode by remember { mutableStateOf(false) }
    var watermark by remember { mutableStateOf(false) }
    var logoCorner by remember { mutableStateOf(4) }
    var opacity by remember { mutableFloatStateOf(0.8f) }
    var wmScale by remember { mutableFloatStateOf(0.22f) }
    val edited = remember(bitmap, brightness, contrast, saturation, sharpness, before) {
        if (bitmap == null || before) bitmap else processBitmap(bitmap, brightness, contrast, saturation, sharpness)
    }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Lime) }
            Text("Edit Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (bitmap != null) saveEdited(context, edited ?: bitmap, watermark, logoCorner, opacity, wmScale) }) { Icon(Icons.Default.Save, "Save", tint = Lime) }
            IconButton(onClick = onCaption) { Icon(Icons.Default.Share, "Caption", tint = Lime) }
        }
        if (bitmap == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No photo selected", color = TextGrey) }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                Image((edited ?: bitmap).asImageBitmap(), contentDescription = "Edited preview", modifier = Modifier.fillMaxSize())
                if (watermark) WatermarkOverlay(logoCorner, wmScale, opacity)
                Row(Modifier.align(Alignment.TopCenter).padding(10.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xAA111111)).clickable { before = !before }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(if (before) "BEFORE • Tap to view after" else "AFTER • Tap to view before", color = Color.White, fontSize = 11.sp)
                }
            }
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    EditorAction("Auto", Icons.Default.AutoFixHigh, Modifier.weight(1f)) { autoMode = true; brightness = 0.08f; contrast = 1.12f; saturation = 1.12f; sharpness = 0.25f }
                    EditorAction("Reset", Icons.Default.RestartAlt, Modifier.weight(1f)) { autoMode = false; brightness = 0f; contrast = 1f; saturation = 1f; sharpness = 0f; before = false }
                    EditorAction("Caption", Icons.Default.TextFields, Modifier.weight(1f), onCaption)
                }
                SliderLine("Brightness", brightness, -0.5f, 0.5f, { brightness = it })
                SliderLine("Contrast", contrast, 0.5f, 1.8f, { contrast = it })
                SliderLine("Saturation", saturation, 0f, 2f, { saturation = it })
                SliderLine("Sharpness", sharpness, 0f, 1f, { sharpness = it })
                Row(verticalAlignment = Alignment.CenterVertically) { Text("IDT Watermark", color = Color.White, modifier = Modifier.weight(1f)); Switch(watermark, { watermark = it }, colors = SwitchDefaults.colors(checkedThumbColor = Lime, checkedTrackColor = Lime.copy(alpha = .4f))) }
                if (watermark) {
                    Text("Position", color = TextGrey, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("↖", "↗", "↙", "↘", "◎").forEachIndexed { index, label ->
                            Text(label, color = if (logoCorner == index) Lime else Color.White, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceDark).clickable { logoCorner = index }.padding(12.dp), fontSize = 18.sp)
                        }
                    }
                    SliderLine("Logo size", wmScale, .08f, .45f, { wmScale = it })
                    SliderLine("Opacity", opacity, .1f, 1f, { opacity = it })
                }
                Button(onClick = { saveEdited(context, edited ?: bitmap, watermark, logoCorner, opacity, wmScale) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)) { Text("SAVE HD JPG", fontWeight = FontWeight.Bold) }
            }
        }
    }
}


@Composable
private fun EditorAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, action: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark).clickable { action() }.padding(vertical = 12.dp).then(modifier), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = Lime, modifier = Modifier.size(24.dp)); Spacer(Modifier.height(5.dp)); Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun SliderLine(label: String, value: Float, min: Float, max: Float, onValue: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(76.dp))
        Slider(value, onValue, valueRange = min..max, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Lime, activeTrackColor = Lime))
    }
}


@Composable
private fun WatermarkOverlay(corner: Int, scale: Float, opacity: Float) {
    val context = LocalContext.current
    val logo = remember { loadLogo(context) } ?: return
    val align = when (corner) { 0 -> Alignment.TopStart; 1 -> Alignment.TopEnd; 2 -> Alignment.BottomStart; 3 -> Alignment.BottomEnd; else -> Alignment.Center }
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = align) {
        Image(logo.asImageBitmap(), contentDescription = "IDT watermark", modifier = Modifier.fillMaxWidth(scale).graphicsLayer(alpha = opacity))
    }
}


@Composable
private fun BatchScreen(bitmaps: List<Bitmap>, index: Int, onIndex: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Lime) }; Text("Batch Photos", color = Color.White, fontWeight = FontWeight.Bold) }
        if (bitmaps.isEmpty()) Text("No photos selected", color = TextGrey)
        else {
            Image(bitmaps[index].asImageBitmap(), contentDescription = "Batch preview", modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)))
            Text("Photo ${index + 1} of ${bitmaps.size}", color = TextGrey, modifier = Modifier.padding(vertical = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
                Button(onClick = { onIndex((index - 1).coerceAtLeast(0)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Previous") }
                Button(onClick = { onIndex((index + 1).coerceAtMost(bitmaps.lastIndex)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)) { Text("Next") }
            }
            Button(onClick = { bitmaps.forEach { saveEdited(context, it, false, 4, 1f, .22f) }; Toast.makeText(context, "Saved selected photos", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)) { Text("SAVE ALL PHOTOS") }
        }
    }
}


@Composable
private fun CaptionDialog(onClose: () -> Unit) {
    var language by remember { mutableStateOf("English") }
    var tone by remember { mutableStateOf("Natural") }
    var platform by remember { mutableStateOf("Instagram") }
    var caption by remember { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onClose, containerColor = SurfaceDark, title = { Text("Caption & Post", color = Lime) }, text = {
        Column {
            Text("Language", color = TextGrey); Row { listOf("English", "Afrikaans").forEach { Text(it, color = if (language == it) Lime else Color.White, modifier = Modifier.clickable { language = it }.padding(8.dp)) } }
            Text("Style", color = TextGrey); Row { listOf("Natural", "Bold", "Emotional").forEach { Text(it, color = if (tone == it) Lime else Color.White, modifier = Modifier.clickable { tone = it }.padding(6.dp)) } }
            Text("Platform", color = TextGrey); Row { listOf("Instagram", "Facebook", "WhatsApp").forEach { Text(it, color = if (platform == it) Lime else Color.White, modifier = Modifier.clickable { platform = it }.padding(6.dp)) } }
            OutlinedTextField(caption, { caption = it }, label = { Text("Your caption") }, minLines = 3)
            Button(onClick = { caption = captionTemplate(language, tone, platform) }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black), modifier = Modifier.fillMaxWidth()) { Text("Generate Caption") }
            Button(onClick = { val clip = android.content.ClipData.newPlainText("Caption", caption); (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip); Toast.makeText(context, "Caption copied", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Copy Caption", color = Lime) }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Close", color = Lime) } })
}


private fun captionTemplate(language: String, tone: String, platform: String): String {
    if (language == "Afrikaans") return when (tone) {
        "Bold" -> "Elke foto vertel 'n storie. Hierdie is myne. 📸 #IDTPhotography"
        "Emotional" -> "Klein oomblikke, groot herinneringe. Dankbaar vir elkeen wat ek kan vasvang. 📸"
        else -> "Lig, oomblik en herinnering—vasgevang vir altyd. 📸 #IDTPhotography"
    }
    return when (tone) {
        "Bold" -> "No filters on the moment. Just a frame worth remembering. 📸 #IDTPhotography"
        "Emotional" -> "Some moments pass. The feeling deserves to stay. 📸"
        else -> "A moment captured, a memory kept. 📸 #IDTPhotography"
    }
}


private fun loadBitmap(context: Context, uri: Uri): Bitmap? = try { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }
private fun loadLogo(context: Context): Bitmap? = try { context.assets.open("idt_logo_transparent.png").use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { try { context.assets.open("idt_logo.png").use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null } }


private fun processBitmap(src: Bitmap, brightness: Float, contrast: Float, saturation: Float, sharpness: Float): Bitmap {
    val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val matrix = android.graphics.ColorMatrix().apply {
        val s = saturation
        val sr = 0.213f * (1 - s); val sg = 0.715f * (1 - s); val sb = 0.072f * (1 - s)
        set(floatArrayOf(sr+s, sg, sb, 0f, brightness*255, sr, sg+s, sb, 0f, brightness*255, sr, sg, sb+s, 0f, brightness*255, 0f,0f,0f,1f,0f))
        postConcat(android.graphics.ColorMatrix().apply { set(floatArrayOf(contrast,0f,0f,0f,128f*(1-contrast),0f,contrast,0f,0f,128f*(1-contrast),0f,0f,contrast,0f,128f*(1-contrast),0f,0f,0f,1f,0f) })
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = android.graphics.ColorMatrixColorFilter(matrix) }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return if (sharpness > 0.01f) sharpen(output, sharpness) else output
}

private fun sharpen(bitmap: Bitmap, amount: Float): Bitmap {
    if (bitmap.width < 3 || bitmap.height < 3 || amount <= 0f) return bitmap
    val src = bitmap.getPixelsArray(); val dst = src.copyOf(); val w = bitmap.width; val h = bitmap.height
    val a = amount.coerceIn(0f, 1f)
    for (y in 1 until h-1) for (x in 1 until w-1) {
        val i = y*w+x
        for (shift in 0..2) {
            val c = (src[i] ushr (shift*8)) and 255
            val n = ((src[i-w] ushr (shift*8)) and 255); val s = ((src[i+w] ushr (shift*8)) and 255)
            val l = ((src[i-1] ushr (shift*8)) and 255); val r = ((src[i+1] ushr (shift*8)) and 255)
            val v = (c + (c*4 - n-s-l-r)*a).toInt().coerceIn(0,255)
            dst[i] = (dst[i] and (0xFF shl (shift*8)).inv()) or (v shl (shift*8))
        }
    }
    val out = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); out.setPixels(dst,0,w,0,0,w,h); return out
}
private fun Bitmap.getPixelsArray(): IntArray { val arr = IntArray(width*height); getPixels(arr,0,width,0,0,width,height); return arr }


private fun saveEdited(context: Context, photo: Bitmap, addLogo: Boolean, corner: Int, opacity: Float, scale: Float) {
    val finalBitmap = if (addLogo) applyWatermark(context, photo, corner, opacity, scale) else photo
    val filename = "IDT_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/IDT Photography")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    try {
        uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) } }
        Toast.makeText(context, "Saved to Pictures/IDT Photography", Toast.LENGTH_LONG).show()
    } catch (e: Exception) { Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show() }
}
private fun applyWatermark(context: Context, photo: Bitmap, corner: Int, opacity: Float, scale: Float): Bitmap {
    val out = photo.copy(Bitmap.Config.ARGB_8888, true); val canvas = Canvas(out); val logo = loadLogo(context) ?: return out
    val width = (photo.width*scale).toInt().coerceAtLeast(60); val height = (width.toFloat()*logo.height/logo.width).toInt(); val margin = (photo.width*.035f).toInt()
    val left = when(corner) { 0,2 -> margin; 1,3 -> photo.width-width-margin; else -> (photo.width-width)/2 }
    val top = when(corner) { 0,1 -> margin; 2,3 -> photo.height-height-margin; else -> (photo.height-height)/2 }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = (opacity*255).toInt().coerceIn(0,255) }
    canvas.drawBitmap(logo, null, Rect(left,top,left+width,top+height), paint); return out
}
