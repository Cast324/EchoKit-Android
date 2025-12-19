package com.michaelblades.echokit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelblades.echokit.EchoKitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the list of ideas
 */
class IdeasViewModel(private val client: EchoKitClient) : ViewModel() {

    private val _ideas = MutableStateFlow<List<EchoKitClient.Idea>>(emptyList())
    val ideas: StateFlow<List<EchoKitClient.Idea>> = _ideas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedStatus = MutableStateFlow<EchoKitClient.IdeaStatus?>(null)
    val selectedStatus: StateFlow<EchoKitClient.IdeaStatus?> = _selectedStatus.asStateFlow()

    fun loadIdeas() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = client.getIdeas(
                    status = _selectedStatus.value,
                    onlyApproved = true
                )
                _ideas.value = result
            } catch (e: CancellationException) {
                // Ignore cancellation
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
                val newIdea = client.createIdea(title, body)
                _ideas.value = listOf(newIdea) + _ideas.value
            } catch (e: CancellationException) {
                // Ignore cancellation
            } catch (e: Exception) {
                _errorMessage.value = "Failed to submit idea: ${e.message}"
            }
        }
    }

    fun vote(idea: EchoKitClient.Idea) {
        viewModelScope.launch {
            try {
                client.vote(idea.id)
                loadIdeas()
            } catch (e: CancellationException) {
                // Ignore cancellation
            } catch (e: Exception) {
                _errorMessage.value = "Failed to vote: ${e.message}"
            }
        }
    }

    fun setSelectedStatus(status: EchoKitClient.IdeaStatus?) {
        _selectedStatus.value = status
        loadIdeas()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * ViewModel for managing idea detail view
 */
class IdeaDetailViewModel(
    private val client: EchoKitClient,
    private val ideaId: String
) : ViewModel() {

    private val _ideaDetail = MutableStateFlow<EchoKitClient.IdeaDetail?>(null)
    val ideaDetail: StateFlow<EchoKitClient.IdeaDetail?> = _ideaDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSubmittingComment = MutableStateFlow(false)
    val isSubmittingComment: StateFlow<Boolean> = _isSubmittingComment.asStateFlow()

    private var loadJob: Job? = null

    fun loadDetail(forceRefresh: Boolean = false) {
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val detail = client.getIdeaDetail(ideaId)
                _ideaDetail.value = detail
            } catch (e: CancellationException) {
                // Ignore cancellation
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
                client.vote(ideaId)
                loadDetail()
            } catch (e: CancellationException) {
                // Ignore cancellation
            } catch (e: Exception) {
                _errorMessage.value = "Failed to vote: ${e.message}"
            }
        }
    }

    fun addComment(body: String) {
        if (body.isEmpty()) return

        viewModelScope.launch {
            _isSubmittingComment.value = true
            _errorMessage.value = null

            try {
                client.addComment(ideaId, body)
                loadDetail()
            } catch (e: CancellationException) {
                // Ignore cancellation
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add comment: ${e.message}"
            } finally {
                _isSubmittingComment.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
