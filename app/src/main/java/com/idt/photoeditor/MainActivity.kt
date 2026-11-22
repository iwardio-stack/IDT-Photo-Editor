
package com.idt.photoeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF0B0B0B)
private val PANEL = Color(0xFF191919)
private val LIME = Color(0xFFB7FF00)
private val WHITE = Color(0xFFF5F5F5)
private val MUTED = Color(0xFF929292)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IDTPhotoApp(
                onPickPhoto = { },
                onPickBatch = { }
            )
        }
    }
}

@Composable
fun IDTPhotoApp(
    onPickPhoto: () -> Unit,
    onPickBatch: () -> Unit
) {
    var screen by remember { mutableStateOf("home") }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var captionInput by remember { mutableStateOf("") }
    var captionOutput by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("English") }

    val context = androidx.compose.ui.platform.LocalContext.current

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    selectedImage = BitmapFactory.decodeStream(it)
                }
                screen = "photo"
            } catch (_: Exception) {
                selectedImage = null
            }
        }
    }

    val batchPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = uris
        screen = "batch"
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BG
        ) {
            when (screen) {
                "photo" -> PhotoScreen(
                    bitmap = selectedImage,
                    onBack = { screen = "home" },
                    onChooseAgain = {
                        singlePicker.launch("image/*")
                    }
                )

                "batch" -> BatchScreen(
                    images = selectedImages,
                    onBack = { screen = "home" },
                    onChooseAgain = {
                        batchPicker.launch("image/*")
                    }
                )

                "captions" -> CaptionScreen(
                    input = captionInput,
                    onInputChange = { captionInput = it },
                    output = captionOutput,
                    language = language,
                    onLanguageChange = { language = it },
                    onGenerate = {
                        captionOutput = generateCaption(captionInput, language)
                    },
                    onBack = { screen = "home" }
                )

                else -> HomeScreen(
                    onPhoto = { singlePicker.launch("image/*") },
                    onBatch = { batchPicker.launch("image/*") },
                    onCaptions = { screen = "captions" },
                    onBeforeAfter = { screen = "compare" },
                    onWatermark = { screen = "watermark" },
                    onCompareScreen = { screen = "compare" },
                    onWatermarkScreen = { screen = "watermark" }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onPhoto: () -> Unit,
    onBatch: () -> Unit,
    onCaptions: () -> Unit,
    onBeforeAfter: () -> Unit,
    onWatermark: () -> Unit,
    onCompareScreen: () -> Unit,
    onWatermarkScreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(34.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "I.D.T.",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = LIME
                )
                Text(
                    "PHOTOGRAPHY",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = WHITE
                )
                Text(
                    "EDIT  •  ENHANCE  •  CREATE",
                    color = MUTED,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(65.dp)
                    .border(1.dp, LIME, RoundedCornerShape(16.dp))
                    .background(PANEL, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "IDT",
                    color = LIME,
                    fontWeight = FontWeight.Black,
                    fontSize = 21.sp
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        Text(
            "YOUR CREATIVE STUDIO",
            color = LIME,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Text(
            "Bring your photos to life.",
            color = WHITE,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .background(PANEL, RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFF292929), RoundedCornerShape(22.dp))
                .clickable { onPhoto() }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("＋", fontSize = 42.sp, color = LIME)
                Text(
                    "START EDITING",
                    color = LIME,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Choose a photo from your gallery",
                    color = MUTED,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "PHOTO TOOLS",
            color = WHITE,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        ToolCard(
            title = "PHOTO ENHANCER",
            description = "Improve clarity, light, colour and detail",
            icon = "✦",
            onClick = onPhoto
        )

        ToolCard(
            title = "BATCH EDIT",
            description = "Select and work with multiple photographs",
            icon = "▧",
            onClick = onBatch
        )

        ToolCard(
            title = "BEFORE & AFTER",
            description = "Compare original and edited photographs",
            icon = "◐",
            onClick = onBeforeAfter
        )

        ToolCard(
            title = "AI CAPTION STUDIO",
            description = "Create captions, posts and hashtags",
            icon = "✎",
            onClick = onCaptions
        )

        ToolCard(
            title = "IDT WATERMARK",
            description = "Brand your photographs with IDT",
            icon = "◈",
            onClick = onWatermark
        )

        Spacer(Modifier.height(25.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414), RoundedCornerShape(14.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "I.D.T. STEELWORKS & ENGINEERING",
                color = MUTED,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(25.dp))
    }
}

@Composable
fun ToolCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(PANEL, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF292929), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF242424), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = LIME, fontSize = 24.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = LIME,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                description,
                color = MUTED,
                fontSize = 12.sp
            )
        }

        Text("›", color = LIME, fontSize = 25.sp)
    }
}

@Composable
fun PhotoScreen(
    bitmap: Bitmap?,
    onBack: () -> Unit,
    onChooseAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("‹  BACK", color = LIME, modifier = Modifier.clickable { onBack() })
        Spacer(Modifier.height(20.dp))

        Text("PHOTO ENHANCER", color = LIME, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("Your selected photo", color = MUTED)

        Spacer(Modifier.height(20.dp))

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected photograph",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(PANEL, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Unable to load image", color = MUTED)
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onChooseAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LIME, contentColor = BG)
        ) {
            Text("SELECT ANOTHER PHOTO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BatchScreen(
    images: List<Uri>,
    onBack: () -> Unit,
    onChooseAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("‹  BACK", color = LIME, modifier = Modifier.clickable { onBack() })
        Spacer(Modifier.height(20.dp))
        Text("BATCH EDIT", color = LIME, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("${images.size} photos selected", color = MUTED)

        Spacer(Modifier.height(15.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images) { uri ->
                Column(
                    modifier = Modifier
                        .background(PANEL, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text("PHOTO", color = LIME, fontSize = 11.sp)
                    Text(uri.lastPathSegment ?: "Selected image", color = WHITE, fontSize = 10.sp)
                }
            }
        }

        Button(
            onClick = onChooseAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LIME, contentColor = BG)
        ) {
            Text("SELECT PHOTOS", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CaptionScreen(
    input: String,
    onInputChange: (String) -> Unit,
    output: String,
    language: String,
    onLanguageChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("‹  BACK", color = LIME, modifier = Modifier.clickable { onBack() })
        Spacer(Modifier.height(20.dp))

        Text("CAPTION STUDIO", color = LIME, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Create words to go with your photographs.", color = MUTED)

        Spacer(Modifier.height(20.dp))

        Text("LANGUAGE", color = WHITE, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("English", "Afrikaans").forEach { lang ->
                FilterChip(
                    selected = language == lang,
                    onClick = { onLanguageChange(lang) },
                    label = { Text(lang) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LIME,
                        selectedLabelColor = BG
                    )
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Describe your photo or mood") },
            placeholder = { Text("A sunset, a journey, a memory...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WHITE,
                unfocusedTextColor = WHITE,
                focusedBorderColor = LIME,
                unfocusedBorderColor = MUTED,
                focusedLabelColor = LIME,
                unfocusedLabelColor = MUTED
            )
        )

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LIME, contentColor = BG)
        ) {
            Text("GENERATE CAPTION", fontWeight = FontWeight.Bold)
        }

        if (output.isNotBlank()) {
            Spacer(Modifier.height(20.dp))

            Text("YOUR CAPTION", color = LIME, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = PANEL),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(output, color = WHITE)

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val clipboard = androidx.compose.ui.platform.LocalClipboardManager
                            // Copy action is handled in the next functionality stage.
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303030))
                    ) {
                        Text("CAPTION READY", color = LIME)
                    }
                }
            }
        }
    }
}

fun generateCaption(text: String, language: String): String {
    if (text.isBlank()) return ""

    return if (language == "Afrikaans") {
        "Elke oomblik het sy eie storie. ${text.trim()} #Fotografie #Herinneringe"
    } else {
        "Every moment has its own story. ${text.trim()} #Photography #Memories"
    }
}
