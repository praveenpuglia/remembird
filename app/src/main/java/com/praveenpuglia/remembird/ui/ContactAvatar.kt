package com.praveenpuglia.remembird.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveenpuglia.remembird.ui.theme.Handwriting
import com.praveenpuglia.remembird.ui.theme.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** The contact as a tilted polaroid: their photo, or their initial on a pastel picked from their name. */
@Composable
fun PolaroidAvatar(name: String, photoUri: String?, size: Dp, tilt: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, photoUri) {
        value = photoUri?.let { photo ->
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photo))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } catch (_: IOException) {
                    null // photo removed since the query ran; fall back to the initial
                }
            }
        }
    }
    Box(
        modifier
            .rotate(tilt)
            .shadow(3.dp)
            .background(Paper.card)
            .padding(start = 3.dp, top = 3.dp, end = 3.dp, bottom = 10.dp),
    ) {
        Box(
            Modifier.size(size).background(Paper.pastels[Math.floorMod(name.hashCode(), Paper.pastels.size)]),
            contentAlignment = Alignment.Center,
        ) {
            val photo = bitmap
            if (photo != null) {
                Image(photo, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(
                    name.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "#",
                    color = Paper.ink,
                    fontFamily = Handwriting,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                )
            }
        }
    }
}
