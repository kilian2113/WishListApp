package es.ulpgc.wishlistapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal data class AvatarOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

internal val avatarOptions = listOf(
    AvatarOption("person", "Persona", Icons.Default.AccountCircle),
    AvatarOption("face", "Cara", Icons.Default.Face),
    AvatarOption("pets", "Mascota", Icons.Default.Pets),
    AvatarOption("music", "Música", Icons.Default.MusicNote),
    AvatarOption("sports", "Deporte", Icons.Default.SportsSoccer),
    AvatarOption("favorite", "Favorito", Icons.Default.Favorite)
)

@Composable
internal fun ProfileAvatar(
    avatarId: String,
    modifier: Modifier = Modifier
) {
    val option = avatarOptions.firstOrNull { it.id == avatarId } ?: avatarOptions.first()
    val colors = avatarColors(avatarId)

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.first
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = colors.second,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
internal fun AvatarPickerItem(
    option: AvatarOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = avatarColors(option.id)
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.first)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = colors.second
            )
        }
    }
}

@Composable
private fun avatarColors(avatarId: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (avatarId) {
        "face" -> scheme.secondaryContainer to scheme.onSecondaryContainer
        "pets" -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        "music" -> scheme.primaryContainer to scheme.onPrimaryContainer
        "sports" -> scheme.secondaryContainer to scheme.onSecondaryContainer
        "favorite" -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
}
