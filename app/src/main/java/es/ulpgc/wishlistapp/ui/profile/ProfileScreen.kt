package es.ulpgc.wishlistapp.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.ulpgc.wishlistapp.domain.model.EditableProfile
import es.ulpgc.wishlistapp.domain.model.UserProfile

private val hobbies = listOf(
    "Leer",
    "Música",
    "Cine y series",
    "Videojuegos",
    "Cocina",
    "Viajar",
    "Fotografía",
    "Jardinería",
    "Escritura",
    "Crochet",
    "Pintar",
    "Dibujar",
    "Cerámica",
    "Senderismo",
)

private val shirtSizes = listOf("XS", "S", "M", "L", "XL", "XXL")
private val pantsSizes = (34..56 step 2).map(Int::toString)
private val shoeSizes = (35..48).map(Int::toString)

private data class AvatarOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val avatarOptions = listOf(
    AvatarOption("person", "Persona", Icons.Default.AccountCircle),
    AvatarOption("face", "Cara", Icons.Default.Face),
    AvatarOption("pets", "Mascota", Icons.Default.Pets),
    AvatarOption("music", "Música", Icons.Default.MusicNote),
    AvatarOption("sports", "Deporte", Icons.Default.SportsSoccer),
    AvatarOption("favorite", "Favorito", Icons.Default.Favorite)
)

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.state.observeAsState(ProfileState())

    when {
        state.isLoading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        state.profile == null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.error ?: "No se pudo cargar el perfil.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { viewModel.onEvent(ProfileEvent.RetryLoading) }) {
                Text("Reintentar")
            }
        }

        else -> ProfileContent(
            state = state,
            isOwnProfile = true,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileState,
    isOwnProfile: Boolean,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = state.profile ?: return
    val isEditing = isOwnProfile && state.isEditing
    val displayedProfile = if (isEditing) {
        state.draft ?: profile.toEditableProfile()
    } else {
        profile.toEditableProfile()
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var showRegenerateConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 20.dp, top = 32.dp, end = 20.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                avatarId = displayedProfile.avatarId,
                modifier = Modifier.size(64.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = displayedProfile.username,
                        onValueChange = { onEvent(ProfileEvent.UsernameChanged(it)) },
                        label = { Text("Nombre de usuario") },
                        singleLine = true,
                        isError = state.usernameError != null,
                        supportingText = state.usernameError?.let { message ->
                            { Text(message) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = displayedProfile.name,
                        onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
                        label = { Text("Nombre") },
                        singleLine = true,
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { message ->
                            { Text(message) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = displayedProfile.username.takeIf(String::isNotBlank)?.let { "@$it" }
                            ?: "Usuario",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayedProfile.name.ifBlank { "Nombre" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (isOwnProfile) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones del perfil")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (isEditing) {
                            DropdownMenuItem(
                                text = { Text("Guardar cambios") },
                                enabled = !state.isSaving,
                                onClick = {
                                    menuExpanded = false
                                    onEvent(ProfileEvent.SaveChanges)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancelar edición") },
                                enabled = !state.isSaving,
                                onClick = {
                                    menuExpanded = false
                                    onEvent(ProfileEvent.CancelEditing)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Editar perfil") },
                                onClick = {
                                    menuExpanded = false
                                    onEvent(ProfileEvent.StartEditing)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isEditing) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Avatar", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    avatarOptions.forEach { option ->
                        AvatarPickerItem(
                            option = option,
                            selected = displayedProfile.avatarId == option.id,
                            onClick = { onEvent(ProfileEvent.AvatarChanged(option.id)) }
                        )
                    }
                }
            }
        }

        if (isOwnProfile) {
            ProfileSection(title = "Código de amigo", contentSpacing = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.friendCode,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText("Código de amigo", profile.friendCode)
                            )
                            Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar código de amigo"
                        )
                    }
                    IconButton(
                        onClick = { showRegenerateConfirmation = true },
                        enabled = !state.isRegeneratingFriendCode && !isEditing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerar código de amigo"
                        )
                    }
                    if (state.isRegeneratingFriendCode) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                Text(
                    text = "Comparte este código para que puedan agregarte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }

        ProfileSection(title = "Descripción") {
            if (isEditing) {
                OutlinedTextField(
                    value = displayedProfile.description,
                    onValueChange = { onEvent(ProfileEvent.DescriptionChanged(it)) },
                    placeholder = { Text("Cuéntales algo sobre ti") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        hintLocales = LocaleList(Locale("es"))
                    ),
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = displayedProfile.description.ifBlank { "Sin descripción." },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        ProfileSection(title = "Aficiones") {
            ProfileTagSection(
                options = hobbies,
                selectedOptions = displayedProfile.hobbies,
                enabled = isEditing,
                onToggle = { onEvent(ProfileEvent.HobbyToggled(it)) }
            )
        }

        ProfileSection(title = "Tallas") {
            ProfileSizeField(
                label = "Camiseta",
                value = displayedProfile.shirtSize,
                options = shirtSizes,
                enabled = isEditing,
                onValueSelected = { onEvent(ProfileEvent.ShirtSizeChanged(it)) }
            )
            ProfileSizeField(
                label = "Pantalones (EU)",
                value = displayedProfile.pantsSize,
                options = pantsSizes,
                enabled = isEditing,
                onValueSelected = { onEvent(ProfileEvent.PantsSizeChanged(it)) }
            )
            ProfileSizeField(
                label = "Zapatos (EU)",
                value = displayedProfile.shoeSize,
                options = shoeSizes,
                enabled = isEditing,
                onValueSelected = { onEvent(ProfileEvent.ShoeSizeChanged(it)) }
            )
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = { onEvent(ProfileEvent.CancelEditing) },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onEvent(ProfileEvent.SaveChanges) },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }

    if (showRegenerateConfirmation && isOwnProfile) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirmation = false },
            title = { Text("Regenerar código") },
            text = {
                Text("El código actual dejará de funcionar. ¿Quieres generar uno nuevo?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateConfirmation = false
                        onEvent(ProfileEvent.RegenerateFriendCode)
                    }
                ) {
                    Text("Regenerar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProfileAvatar(
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
private fun AvatarPickerItem(
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

@Composable
private fun ProfileSection(
    title: String,
    contentSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun ProfileTagSection(
    options: List<String>,
    selectedOptions: List<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!enabled && selectedOptions.isEmpty()) {
            Text(
                text = "Sin seleccionar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (if (enabled) options else selectedOptions).chunked(2).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { option ->
                            FilterChip(
                                selected = option in selectedOptions,
                                onClick = { onToggle(option) },
                                enabled = enabled,
                                label = {
                                    Text(
                                        text = option,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSizeField(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    if (enabled) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$label: ${value.ifBlank { "Sin especificar" }}",
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Sin especificar") },
                    onClick = {
                        expanded = false
                        onValueSelected("")
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onValueSelected(option)
                        }
                    )
                }
            }
        }
    } else {
        Text(
            text = "$label: ${value.ifBlank { "Sin especificar" }}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun UserProfile.toEditableProfile() = EditableProfile(
    username = username,
    name = name,
    description = description,
    hobbies = hobbies,
    avatarId = avatarId,
    shirtSize = shirtSize,
    pantsSize = pantsSize,
    shoeSize = shoeSize
)
