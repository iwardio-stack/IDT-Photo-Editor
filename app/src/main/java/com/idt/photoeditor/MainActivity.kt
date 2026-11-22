
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
            IDTPhotoApp(
                onSave = { bitmap -> savePhoto(bitmap) }
            )
        }
    }

    private fun savePhoto(bitmap: Bitmap) {
        try {
            val name = "IDT_EDIT_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
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
                    "Photo saved to gallery",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not save: ${e.message}",
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
    var busy by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    original = bmp
                    edited = bmp
                    brightness = 0f
                    contrast = 1f
                    saturation = 1f
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(original, brightness, contrast, saturation) {
        val source = original
        if (source != null) {
            busy = true
            edited = withContext(Dispatchers.Default) {
                adjustBitmap(source, brightness, contrast, saturation)
            }
            busy = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BG
    ) {
        if (original == null) {
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
                    onClick = { picker.launch("image/*") },
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

                Spacer(Modifier.height(10.dp))

                HomeTool(
                    "PHOTO ENHANCER",
                    "Brightness • Contrast • Saturation"
                ) {
                    picker.launch("image/*")
                }

                HomeTool(
                    "PHOTO EDITOR",
                    "Select and adjust a photograph"
                ) {
                    picker.launch("image/*")
                }

                Spacer(Modifier.height(30.dp))

                Text(
                    "I.D.T. STEELWORKS & ENGINEERING",
                    color = MUTED,
                    fontSize = 10.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "‹  BACK",
                    color = LIME,
                    modifier = Modifier.clickable {
                        original = null
                        edited = null
                    }
                )

                Spacer(Modifier.height(15.dp))

                Text(
                    "PHOTO ENHANCER",
                    color = LIME,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Adjust your image", color = MUTED)

                Spacer(Modifier.height(15.dp))

                edited?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Edited photo preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.Black, RoundedCornerShape(15.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(15.dp))

                AdjustmentSlider(
                    "Brightness",
                    brightness,
                    -100f..100f,
                    brightness.toInt().toString()
                ) { brightness = it }

                AdjustmentSlider(
                    "Contrast",
                    contrast,
                    0.5f..1.8f,
                    "%.2f".format(contrast)
                ) { contrast = it }

                AdjustmentSlider(
                    "Saturation",
                    saturation,
                    0f..2f,
                    "%.2f".format(saturation)
                ) { saturation = it }

                if (busy) {
                    Text("Updating preview...", color = LIME)
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        brightness = 0f
                        contrast = 1f
                        saturation = 1f
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RESET ADJUSTMENTS", color = LIME)
                }

                Button(
                    onClick = { edited?.let { onSave(it) } },
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
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CHOOSE ANOTHER PHOTO", color = LIME)
                }

                Spacer(Modifier.height(20.dp))
            }
        }
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

fun adjustBitmap(
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val width = result.width
    val height = result.height
    val pixels = IntArray(width * height)

    result.getPixels(pixels, 0, width, 0, 0, width, height)

    val hsv = FloatArray(3)
    val offset = brightness

    for (i in pixels.indices) {
        val pixel = pixels[i]

        var r = AColor.red(pixel).toFloat()
        var g = AColor.green(pixel).toFloat()
        var b = AColor.blue(pixel).toFloat()

        r = ((r - 128f) * contrast + 128f + offset).coerceIn(0f, 255f)
        g = ((g - 128f) * contrast + 128f + offset).coerceIn(0f, 255f)
        b = ((b - 128f) * contrast + 128f + offset).coerceIn(0f, 255f)

        AColor.RGBToHSV(r.toInt(), g.toInt(), b.toInt(), hsv)
        hsv[1] = (hsv[1] * saturation).coerceIn(0f, 1f)

        pixels[i] = AColor.HSVToColor(
            AColor.alpha(pixel),
            hsv
        )
    }

    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}
