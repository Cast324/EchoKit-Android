package com.michaelblades.testechokit

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelblades.testechokit.client.EchoKitClient
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Ideas list.
 * (Rewritten to avoid delegated State<T> properties that can trigger K2/FIR crashes)
 */
class IdeasViewModel(private val client: EchoKitClient) : ViewModel() {

    private val _ideas = mutableStateOf<List<EchoKitClient.Idea>>(emptyList())
    val ideas: List<EchoKitClient.Idea>
        get() = _ideas.value

    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean
        get() = _isLoading.value

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: String?
        get() = _errorMessage.value

    private val _selectedStatus = mutableStateOf<EchoKitClient.IdeaStatus?>(null)
    val selectedStatus: EchoKitClient.IdeaStatus?
        get() = _selectedStatus.value

    init {
        loadIdeas()
    }

    fun onStatusSelected(status: EchoKitClient.IdeaStatus?) {
        _selectedStatus.value = status
        loadIdeas()
    }

    fun loadIdeas() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _ideas.value = client.getIdeas(status = selectedStatus, onlyApproved = true)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load ideas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitIdea(title: String, body: String?) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val newIdea = client.createIdea(title, body)
                _ideas.value = listOf(newIdea) + ideas
            } catch (e: Exception) {
                _errorMessage.value = "Failed to submit idea: ${e.message}"
            }
        }
    }

    fun vote(ideaId: String) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                client.vote(ideaId)
                loadIdeas()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to vote: ${e.message}"
            }
        }
    }
}

/**
 * ViewModel for managing Idea detail.
 * (Rewritten to avoid delegated State<T> properties that can trigger K2/FIR crashes)
 */
class IdeaDetailViewModel(
    private val client: EchoKitClient,
    private val ideaId: String
) : ViewModel() {

    private val _ideaDetail = mutableStateOf<EchoKitClient.IdeaDetail?>(null)
    val ideaDetail: EchoKitClient.IdeaDetail?
        get() = _ideaDetail.value

    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean
        get() = _isLoading.value

    private val _isSubmittingComment = mutableStateOf(false)
    val isSubmittingComment: Boolean
        get() = _isSubmittingComment.value

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: String?
        get() = _errorMessage.value

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _ideaDetail.value = client.getIdeaDetail(ideaId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load idea: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun vote() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                client.vote(ideaId)
                loadDetail()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to vote: ${e.message}"
            }
        }
    }

    fun addComment(body: String) {
        if (body.isBlank()) return

        viewModelScope.launch {
            try {
                _isSubmittingComment.value = true
                _errorMessage.value = null
                client.addComment(ideaId, body)
                loadDetail()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add comment: ${e.message}"
            } finally {
                _isSubmittingComment.value = false
            }
        }
    }
}

/**
 * Main Ideas List Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasListScreen(viewModel: IdeasViewModel) {
    var showNewIdeaDialog by remember { mutableStateOf(false) }

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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            viewModel.errorMessage?.let { ErrorMessageCard(it) }

            StatusFilterTabs(
                // Use values() for broad compatibility (instead of Enum.entries)
                allStatuses = EchoKitClient.IdeaStatus.values().toList(),
                selectedStatus = viewModel.selectedStatus,
                onStatusSelected = viewModel::onStatusSelected
            )

            if (viewModel.isLoading && viewModel.ideas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.ideas) { idea ->
                        IdeaCard(
                            idea = idea,
                            onVote = { viewModel.vote(idea.id) },
                            onClick = { /* TODO: Navigate to detail */ }
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
fun StatusFilterTabs(
    allStatuses: List<EchoKitClient.IdeaStatus>,
    selectedStatus: EchoKitClient.IdeaStatus?,
    onStatusSelected: (EchoKitClient.IdeaStatus?) -> Unit
) {
    val selectedTabIndex = if (selectedStatus == null) 0 else allStatuses.indexOf(selectedStatus) + 1

    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth()
    ) {
        Tab(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            text = { Text("All") }
        )
        allStatuses.forEach { status ->
            Tab(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                text = { Text(status.displayName) }
            )
        }
    }
}

@Composable
fun ErrorMessageCard(error: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaCard(
    idea: EchoKitClient.Idea,
    onVote: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = idea.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(idea.category)
                    StatusBadge(idea.status)
                    if (idea.isApproved) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Approved",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            idea.body?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoteButton(
                    voteCount = idea.voteCount,
                    userHasVoted = idea.userHasVoted,
                    onVote = onVote
                )
                CommentCount(idea.commentCount)
            }
        }
    }
}

@Composable
fun VoteButton(voteCount: Int, userHasVoted: Boolean, onVote: () -> Unit) {
    OutlinedButton(
        onClick = onVote,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (userHasVoted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            Icons.Default.KeyboardArrowUp,
            contentDescription = "Vote",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("$voteCount")
    }
}

@Composable
fun CommentCount(commentCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.Email,
            contentDescription = "Comments",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$commentCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryBadge(category: EchoKitClient.IdeaCategory) {
    val color = when (category) {
        EchoKitClient.IdeaCategory.NEW_IDEA -> Color(0xFF9E9E9E)
        EchoKitClient.IdeaCategory.FEATURE -> Color(0xFF9C27B0)
        EchoKitClient.IdeaCategory.ENHANCEMENT -> Color(0xFF3F51B5)
        EchoKitClient.IdeaCategory.INTEGRATION -> Color(0xFFE91E63)
        EchoKitClient.IdeaCategory.UI_UX -> Color(0xFF00BCD4)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = "${category.icon} ${category.displayName}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatusBadge(status: EchoKitClient.IdeaStatus) {
    val color = when (status) {
        EchoKitClient.IdeaStatus.PENDING -> Color(0xFF2196F3)
        EchoKitClient.IdeaStatus.IN_PROGRESS -> Color(0xFFFF9800)
        EchoKitClient.IdeaStatus.COMPLETED -> Color(0xFF4CAF50)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color
    ) {
        Text(
            text = status.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewIdeaDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Idea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, body.ifBlank { null }) },
                enabled = title.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaDetailScreen(viewModel: IdeaDetailViewModel, onNavigateBack: () -> Unit) {
    var commentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Idea Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Use Default.ArrowBack (avoids AutoMirrored dependency/version issues)
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (viewModel.isLoading && viewModel.ideaDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            viewModel.ideaDetail?.let {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    viewModel.errorMessage?.let { item { ErrorMessageCard(it) } }

                    // TODO: Implement the rest of the Idea Detail UI
                }
            }
        }
    }
}
