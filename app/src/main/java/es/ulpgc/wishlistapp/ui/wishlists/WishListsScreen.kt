package es.ulpgc.wishlistapp.ui.wishlists

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import es.ulpgc.wishlistapp.ui.components.SearchField

@Composable
fun WishListsScreen(
    viewModel: WishListsViewModel,
    onWishListClick: (String) -> Unit
) {
    val state by viewModel.state.observeAsState(WishListsState())
    val snackbarHostState = remember { SnackbarHostState() }
    val listsError = state.listsError

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(WishListsEvent.SuccessMessageShown)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SearchField(
                query = state.searchQuery,
                onQueryChange = {
                    viewModel.onEvent(WishListsEvent.SearchQueryChanged(it))
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoadingLists && state.lists.isEmpty() -> {
                        CircularProgressIndicator()
                    }
                    listsError != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = listsError,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = {
                                    viewModel.onEvent(WishListsEvent.RetryLoadingLists)
                                }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                    state.lists.isEmpty() -> {
                        Text("Aún no tienes listas creadas.\n   Pulsa + para añadir una.")
                    }
                    state.filteredLists.isEmpty() -> {
                        Text("No hay listas que coincidan con la búsqueda.")
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 12.dp,
                                end = 16.dp,
                                bottom = 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.filteredLists,
                                key = { list -> list.id }
                            ) { wishList ->
                                WishListRow(
                                    name = wishList.name,
                                    description = wishList.description,
                                    itemCount = wishList.itemCount,
                                    onClick = { onWishListClick(wishList.id) },
                                    onDeleteClick = {
                                        viewModel.onEvent(
                                            WishListsEvent.DeleteListClicked(wishList)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.onEvent(WishListsEvent.AddListClicked) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir lista")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }

    if (state.isAddDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isCreatingList) {
                    viewModel.onEvent(WishListsEvent.DismissAddDialog)
                }
            },
            title = { Text("Nueva lista") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.newListName,
                        onValueChange = {
                            viewModel.onEvent(WishListsEvent.NameChanged(it))
                        },
                        label = { Text("Nombre de la lista") },
                        singleLine = true,
                        isError = state.newListNameError != null,
                        supportingText = if (state.newListNameError != null) { { Text(state.newListNameError!!) } } else null,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.newListDescription,
                        onValueChange = {
                            viewModel.onEvent(WishListsEvent.DescriptionChanged(it))
                        },
                        label = { Text("Descripción (Opcional)") },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.operationError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(WishListsEvent.ConfirmCreateList) },
                    enabled = !state.isCreatingList
                ) {
                    if (state.isCreatingList) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Crear")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(WishListsEvent.DismissAddDialog) },
                    enabled = !state.isCreatingList
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    state.listToDelete?.let { wishList ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(WishListsEvent.DismissDeleteDialog)
            },
            title = { Text("Eliminar lista") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Seguro que quieres eliminar la lista \"${wishList.name}\"?")
                    state.deleteListError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(WishListsEvent.ConfirmDeleteList) },
                    enabled = !state.isDeletingList
                ) {
                    if (state.isDeletingList) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(WishListsEvent.DismissDeleteDialog)
                    },
                    enabled = !state.isDeletingList
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun WishListRow(
    name: String,
    description: String,
    itemCount: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (itemCount == 1) "1 elemento" else "$itemCount elementos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar $name"
                )
            }
        }
    }
}
