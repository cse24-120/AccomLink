package com.example.accomlink.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accomlink.models.ChatMessage
import com.example.accomlink.models.ChatRoom
import com.example.accomlink.models.UserRole
import com.example.accomlink.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    closeRoomOnBack: Boolean
) {
    val rooms by viewModel.rooms.collectAsState()
    val activeRoom by viewModel.activeRoom.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val state by viewModel.state.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val peerName by viewModel.activePeerName.collectAsState()
    var draft by rememberSaveable(activeRoom?.id) { mutableStateOf("") }

    fun handleBack() {
        if (activeRoom != null && closeRoomOnBack) {
            viewModel.closeRoom()
        } else {
            if (activeRoom != null) viewModel.closeRoom()
            onBack()
        }
    }

    BackHandler(enabled = activeRoom != null) { handleBack() }

    if (activeRoom == null) {
        ChatRoomsList(rooms = rooms, currentUserId = currentProfile?.id.orEmpty(), onOpenRoom = viewModel::openRoom)
        return
    }

    val room = activeRoom ?: return
    val title = room.chatTitleFor(currentProfile?.id.orEmpty(), peerName)
    val subtitle = room.listingTitle.ifBlank { "Room" }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ChatHeader(title = title, subtitle = subtitle, onBack = ::handleBack)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item { EmptyState("No messages yet", "Start the conversation.") }
            }
            items(messages, key = { message -> message.id.ifBlank { "${message.senderId}-${message.createdAt}" } }) { message ->
                ChatBubble(
                    message = message,
                    isMine = message.senderId == currentProfile?.id
                )
            }
        }

        if (state.error != null) {
            Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        MessageComposer(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                viewModel.send(draft)
                draft = ""
            },
            enabled = draft.isNotBlank() && !state.loading
        )
    }
}

@Composable
private fun ChatHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(28.dp)
            )
            IconButton(onClick = onSend, enabled = enabled) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatRoomsList(
    rooms: List<ChatRoom>,
    currentUserId: String,
    onOpenRoom: (ChatRoom) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Chat rooms", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        }
        if (rooms.isEmpty()) {
            item { EmptyState("No chat rooms yet", "Open a listing and contact the landlord to start a private room.") }
        }
        items(rooms, key = { room -> room.id }) { room ->
            val peerName = room.peerNameFor(currentUserId)
            Card(
                onClick = { onOpenRoom(room) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(room.chatTitleFor(currentUserId, peerName), fontWeight = FontWeight.Bold)
                    Text(room.listingTitle.ifBlank { "Room" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isMine: Boolean) {
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val timeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            Surface(color = bubbleColor, shape = bubbleShape) {
                Text(
                    text = message.text,
                    color = textColor,
                    modifier = Modifier.widthIn(max = 300.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = message.createdAt.chatTime(),
                color = timeColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
            )
        }
    }
}

private fun ChatRoom.chatTitleFor(currentUserId: String, resolvedPeerName: String): String {
    val peerName = resolvedPeerName.ifBlank { peerNameFor(currentUserId) }
    if (peerName.isNotBlank()) return peerName
    val peerRole = if (currentUserId == landlordId) UserRole.Student.displayName else UserRole.Landlord.displayName
    return "Chat with $peerRole"
}

private fun ChatRoom.peerNameFor(currentUserId: String): String =
    if (currentUserId == landlordId) studentName else landlordName

private fun Long.chatTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))
