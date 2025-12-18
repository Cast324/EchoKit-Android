package com.example.echokit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.echokit.EchoKitClient
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasListScreen(
    viewModel: IdeasViewModel,
    onIdeaClick: (String) -> Unit
) {
    val ideas by viewModel.ideas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    
    var showNewIdeaDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadIdeas()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ideas") },
                actions = {
                    IconButton(onClick = { showNewIdeaDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Idea")
                    }
                }
            )
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isLoading && ideas.isNotEmpty()),
            onRefresh = { viewModel.loadIdeas() },
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Error message
                errorMessage?.let { error ->
                    item {
                        ErrorCard(
                            message = error,
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
                
                // Status filter
                item {
                    StatusFilterSection(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { viewModel.setSelectedStatus(it) }
                    )
                }
                
                // Ideas list
                if (ideas.isEmpty() && isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    items(ideas, key = { it.id }) { idea ->
                        IdeaCard(
                            idea = idea,
                            onIdeaClick = { onIdeaClick(idea.id) },
                            onVoteClick = { viewModel.vote(idea) }
                        )
                    }
                }
            }
        }
    }
    
    if (showNewIdeaDialog) {
        NewIdeaDialog(
            onDismiss = { showNewIdeaDialog = false },
            onSubmit = { title, body ->
                viewModel.submitIdea(title, body)
                showNewIdeaDialog = false
            }
        )
    }
}

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun StatusFilterSection(
    selectedStatus: EchoKitClient.IdeaStatus?,
    onStatusSelected: (EchoKitClient.IdeaStatus?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Filter by Status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusSelected(null) },
                    label = { Text("All") },
                    modifier = Modifier.weight(1f)
                )
                
                EchoKitClient.IdeaStatus.values().forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusSelected(status) },
                        label = { Text(status.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun IdeaCard(
    idea: EchoKitClient.Idea,
    onIdeaClick: () -> Unit,
    onVoteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onIdeaClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title and badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = idea.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                CategoryBadge(category = idea.category)
                StatusBadge(status = idea.status)
                
                if (idea.isApproved) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Approved",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Body
            idea.body?.let { body ->
                if (body.isNotEmpty()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vote button
                Row(
                    modifier = Modifier.clickable(onClick = onVoteClick),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (idea.userHasVoted) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Vote",
                        tint = if (idea.userHasVoted) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = idea.voteCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (idea.userHasVoted) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Comment count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MailOutline,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = idea.commentCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryBadge(category: EchoKitClient.IdeaCategory) {
    val backgroundColor = when (category) {
        EchoKitClient.IdeaCategory.NEW_IDEA -> Color(0xFFE0E0E0)
        EchoKitClient.IdeaCategory.FEATURE -> Color(0xFFE1BEE7)
        EchoKitClient.IdeaCategory.ENHANCEMENT -> Color(0xFFC5CAE9)
        EchoKitClient.IdeaCategory.INTEGRATION -> Color(0xFFF8BBD0)
        EchoKitClient.IdeaCategory.UI_UX -> Color(0xFFB2EBF2)
    }
    
    val textColor = when (category) {
        EchoKitClient.IdeaCategory.NEW_IDEA -> Color(0xFF616161)
        EchoKitClient.IdeaCategory.FEATURE -> Color(0xFF7B1FA2)
        EchoKitClient.IdeaCategory.ENHANCEMENT -> Color(0xFF3F51B5)
        EchoKitClient.IdeaCategory.INTEGRATION -> Color(0xFFC2185B)
        EchoKitClient.IdeaCategory.UI_UX -> Color(0xFF0097A7)
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusBadge(status: EchoKitClient.IdeaStatus) {
    val backgroundColor = when (status) {
        EchoKitClient.IdeaStatus.PENDING -> Color(0xFF2196F3)
        EchoKitClient.IdeaStatus.IN_PROGRESS -> Color(0xFFFF9800)
        EchoKitClient.IdeaStatus.COMPLETED -> Color(0xFF4CAF50)
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
