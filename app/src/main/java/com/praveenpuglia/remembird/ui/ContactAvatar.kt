package com.praveenpuglia.remembird.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.praveenpuglia.remembird.R
import com.praveenpuglia.remembird.ui.theme.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Portraits that stand in for contacts without a photo; drawn by scripts/draw-avatar-birds.py. */
private val BIRDS = listOf(
    R.drawable.avatar_myna,
    R.drawable.avatar_parakeet,
    R.drawable.avatar_kingfisher,
    R.drawable.avatar_pigeon,
    R.drawable.avatar_owlet,
    R.drawable.avatar_hoopoe,
    R.drawable.avatar_bulbul,
)

/** The contact as a tilted polaroid: their photo, or a bird picked from their name so it stays the same each time. */
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
        val photo = bitmap
        if (photo != null) {
            Image(photo, contentDescription = null, Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(BIRDS[Math.floorMod(name.hashCode(), BIRDS.size)]), contentDescription = null, Modifier.size(size))
        }
    }
}
