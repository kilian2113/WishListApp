package es.ulpgc.wishlistapp.ui.friends

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.ulpgc.wishlistapp.ui.components.ProfileAvatar
import es.ulpgc.wishlistapp.ui.components.SearchField

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onFriendClick: (String) -> Unit
) {
    val state by viewModel.state.observeAsState(FriendsState())
    val snackbarHostState = remember { SnackbarHostState() }
    val friendsError = state.friendsError

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(FriendsEvent.SuccessMessageShown)
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
                    viewModel.onEvent(FriendsEvent.SearchQueryChanged(it))
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoadingFriends && state.friends.isEmpty() -> {
                        CircularProgressIndicator()
                    }
                    friendsError != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = friendsError,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = {
                                    viewModel.onEvent(FriendsEvent.RetryLoadingFriends)
                                }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                    state.friends.isEmpty() -> {
                        Text("Aún no tienes amigos. Pulsa + para añadir uno.")
                    }
                    state.filteredFriends.isEmpty() -> {
                        Text("No hay amigos que coincidan con la búsqueda.")
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
                                items = state.filteredFriends,
                                key = { friend -> friend.uid }
                            ) { friend ->
                                FriendRow(
                                    name = friend.name,
                                    username = friend.username,
                                    avatarId = friend.avatarId,
                                    onClick = { onFriendClick(friend.uid) }
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { viewModel.onEvent(FriendsEvent.AddFriendClicked) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir amigo")
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
                if (!state.isAddingFriend) {
                    viewModel.onEvent(FriendsEvent.DismissAddDialog)
                }
            },
            title = { Text("Añadir amigo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.friendCode,
                        onValueChange = {
                            viewModel.onEvent(FriendsEvent.FriendCodeChanged(it))
                        },
                        label = { Text("Código de amigo") },
                        singleLine = true,
                        isError = state.friendCodeError != null,
                        supportingText = state.friendCodeError?.let { error ->
                            { Text(error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        )
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
                    onClick = { viewModel.onEvent(FriendsEvent.ConfirmAddFriend) },
                    enabled = !state.isAddingFriend
                ) {
                    if (state.isAddingFriend) {
                        CircularProgressIndicator()
                    } else {
                        Text("Añadir")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(FriendsEvent.DismissAddDialog) },
                    enabled = !state.isAddingFriend
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FriendRow(
    name: String,
    username: String,
    avatarId: String,
    onClick: () -> Unit
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
            ProfileAvatar(
                avatarId = avatarId,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
