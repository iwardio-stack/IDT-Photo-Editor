package com.idt.photoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IDTPhotoEditorApp()
        }
    }
}

private val Black = Color(0xFF101010)
private val CardBlack = Color(0xFF1B1B1B)
private val NeonGreen = Color(0xFFB6FF00)
private val White = Color.White
private val Grey = Color(0xFFAAAAAA)

@Composable
fun IDTPhotoEditorApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(25.dp))

                Text(
                    text = "I.D.T.",
                    color = NeonGreen,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "PHOTOGRAPHY",
                    color = White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "PHOTO EDIT • ENHANCE • CREATE",
                    color = Grey,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(30.dp))

                FeatureCard(
                    title = "PHOTO ENHANCER",
                    description = "Restore, sharpen and improve your photos"
                )

                FeatureCard(
                    title = "BATCH EDIT",
                    description = "Enhance multiple photos at once"
                )

                FeatureCard(
                    title = "BEFORE & AFTER",
                    description = "Compare your original and edited photos"
                )

                FeatureCard(
                    title = "AI CAPTIONS",
                    description = "Create captions and social media posts"
                )

                FeatureCard(
                    title = "IDT WATERMARK",
                    description = "Add your IDT logo to your photographs"
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "CREATED BY I.D.T. STEELWORKS & ENGINEERING",
                    color = Grey,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBlack
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                color = NeonGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = description,
                color = White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
