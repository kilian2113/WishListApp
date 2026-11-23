package es.ulpgc.wishlistapp.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

@Composable
fun FriendsScreen(viewModel: FriendsViewModel) {
    val state by viewModel.state.observeAsState(FriendsState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(FriendsEvent.SuccessMessageShown)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Pulsa + para añadir un amigo.",
            modifier = Modifier.align(Alignment.Center)
        )
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
