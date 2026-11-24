package es.ulpgc.wishlistapp.ui.wishlistdetail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.ulpgc.wishlistapp.domain.model.Gift
import es.ulpgc.wishlistapp.domain.model.GiftPriority
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListDetailScreen(
    listId: String,
    viewModel: WishListDetailViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.observeAsState(WishListDetailState())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onEvent(WishListDetailEvent.GiftImageUrlChanged(it.toString()))
        }
    }

    LaunchedEffect(listId) {
        viewModel.initList(listId)
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(WishListDetailEvent.SuccessMessageShown)
        }
    }

    val wishList = state.wishList
    val error = state.error

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = wishList?.name ?: "Detalle de Lista",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (wishList != null) {
                        IconButton(onClick = { viewModel.onEvent(WishListDetailEvent.EditHeaderClicked) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar información de la lista")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (wishList != null && !state.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(WishListDetailEvent.AddGiftClicked) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir regalo")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null && wishList == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(WishListDetailEvent.RetryLoading) }) {
                            Text("Reintentar")
                        }
                    }
                }
                wishList != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Tarjeta de Información de la Lista y Fecha del Evento
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (wishList.description.isNotBlank()) {
                                    Text(
                                        text = wishList.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    val formattedDate = wishList.eventDate.ifBlank { "Sin fecha de evento" }
                                    Text(
                                        text = "Fecha del evento: $formattedDate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Sección de Regalos
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.gifts.isEmpty()) {
                                Text(
                                    text = "Esta lista aún no contiene regalos.\nPulsa + para añadir uno.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        top = 8.dp,
                                        end = 16.dp,
                                        bottom = 88.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = state.gifts,
                                        key = { gift -> gift.id }
                                    ) { gift ->
                                        GiftRow(
                                            gift = gift,
                                            onClick = { viewModel.onEvent(WishListDetailEvent.EditGiftClicked(gift)) },
                                            onDeleteClick = { viewModel.onEvent(WishListDetailEvent.DeleteGiftClicked(gift)) },
                                            onOpenUrlClick = { url ->
                                                try {
                                                    val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                        "https://$url"
                                                    } else {
                                                        url
                                                    }
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo Editar Cabecera de Lista
    if (state.isEditHeaderDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(WishListDetailEvent.DismissEditHeaderDialog) },
            title = { Text("Editar información de la lista") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.editName,
                        onValueChange = { viewModel.onEvent(WishListDetailEvent.EditNameChanged(it)) },
                        label = { Text("Nombre de la lista") },
                        singleLine = true,
                        isError = state.editNameError != null,
                        supportingText = if (state.editNameError != null) { { Text(state.editNameError!!) } } else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.editDescription,
                        onValueChange = { viewModel.onEvent(WishListDetailEvent.EditDescriptionChanged(it)) },
                        label = { Text("Descripción (Opcional)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.editEventDate,
                        onValueChange = { viewModel.onEvent(WishListDetailEvent.EditEventDateChanged(it)) },
                        label = { Text("Fecha del evento (DD/MM/AAAA)") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        isError = state.editEventDateError != null,
                        supportingText = if (state.editEventDateError != null) { { Text(state.editEventDateError!!) } } else null,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(WishListDetailEvent.ConfirmSaveHeader) },
                    enabled = !state.isSavingHeader
                ) {
                    if (state.isSavingHeader) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(WishListDetailEvent.DismissEditHeaderDialog) },
                    enabled = !state.isSavingHeader
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo Crear/Editar Regalo
    if (state.isGiftDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(WishListDetailEvent.DismissGiftDialog) },
            title = {
                Text(if (state.editingGift == null) "Añadir regalo" else "Editar regalo")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.giftName,
                        onValueChange = { viewModel.onEvent(WishListDetailEvent.GiftNameChanged(it)) },
                        label = { Text("Nombre del regalo") },
                        singleLine = true,
                        isError = state.giftNameError != null,
                        supportingText = if (state.giftNameError != null) { { Text(state.giftNameError!!) } } else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.giftPrice,
                        onValueChange = { viewModel.onEvent(WishListDetailEvent.GiftPriceChanged(it)) },
                        label = { Text("Precio estimado (€)") },
                        singleLine = true,
                        isError = state.giftPriceError != null,
                        supportingText = if (state.giftPriceError != null) { { Text(state.giftPriceError!!) } } else null,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Prioridad", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GiftPriority.entries.forEach { priority ->
                            FilterChip(
                                selected = state.giftPriority == priority,
                                onClick = { viewModel.onEvent(WishListDetailEvent.GiftPriorityChanged(priority)) },
                                label = { Text(priority.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.giftDescription,
                        onValueChange = { viewModel.onEvent(WishListDetailEvent.GiftDescriptionChanged(it)) },
                        label = { Text("Descripción / Notas") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.giftProductUrl,
                        onValueChange = { viewModel.onEvent(WishListDetailEvent.GiftProductUrlChanged(it)) },
                        label = { Text("Enlace del producto (URL)") },
                        singleLine = true,
                        isError = state.giftUrlError != null,
                        supportingText = if (state.giftUrlError != null) { { Text(state.giftUrlError!!) } } else null,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Selector de Imagen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(if (state.giftImageUrl.isBlank()) "Seleccionar imagen" else "Cambiar imagen")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(WishListDetailEvent.ConfirmSaveGift) },
                    enabled = !state.isSavingGift
                ) {
                    if (state.isSavingGift) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(WishListDetailEvent.DismissGiftDialog) },
                    enabled = !state.isSavingGift
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo Eliminar Regalo
    state.giftToDelete?.let { gift ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(WishListDetailEvent.DismissDeleteGiftDialog) },
            title = { Text("Eliminar regalo") },
            text = { Text("¿Seguro que quieres eliminar \"${gift.name}\" de la lista?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(WishListDetailEvent.ConfirmDeleteGift) },
                    enabled = !state.isDeletingGift
                ) {
                    if (state.isDeletingGift) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(WishListDetailEvent.DismissDeleteGiftDialog) },
                    enabled = !state.isDeletingGift
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun GiftRow(
    gift: Gift,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenUrlClick: (String) -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = gift.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    // Badge de Prioridad
                    val chipColors = when (gift.priority) {
                        GiftPriority.HIGH -> SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                        GiftPriority.MEDIUM -> SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        GiftPriority.LOW -> SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(gift.priority.label, style = MaterialTheme.typography.labelSmall) },
                        colors = chipColors
                    )
                }

                if (gift.price > 0.0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f €", gift.price),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (gift.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = gift.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (gift.productUrl.isNotBlank()) {
                IconButton(onClick = { onOpenUrlClick(gift.productUrl) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Abrir enlace del producto"
                    )
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar ${gift.name}"
                )
            }
        }
    }
}
