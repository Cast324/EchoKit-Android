# EchoKit Android SDK

The official Android SDK for [EchoKit](https://www.echokit.app) - a powerful feedback and feature request platform for your Android apps.

A feature request and feedback management library for Android. EchoKit provides drop-in UI components for collecting user ideas, votes, and comments.
[![](https://jitpack.io/v/michaelblades/TestEchoKit.svg)](https://jitpack.io/#michaelblades/TestEchoKit)

## Installation

### Step 1: Add JitPack repository

Add the JitPack repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Or if using `settings.gradle` (Groovy):

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add the dependency

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.cast324:EchoKit-Android:v1.0.0")
}
```

Or if using `build.gradle` (Groovy):

```groovy
dependencies {
    implementation 'com.github.cast324:EchoKit-Android:v1.0.0'
}
```

### Step 3: Add Internet permission

Make sure your `AndroidManifest.xml` includes the Internet permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Usage

### Initialize the Client

Create an `EchoKitClient` instance with your API credentials:

```kotlin
import com.michaelblades.echokit.EchoKitClient

val client = EchoKitClient(
    apiKey = "your_api_key",
    userEmail = "user@example.com",      // optional
    userName = "John Doe",                // optional
    context = applicationContext
)
```

### Option 1: Use Pre-built UI Components

EchoKit includes ready-to-use Compose screens with full functionality.

#### Ideas List Screen

```kotlin
import com.michaelblades.echokit.EchoKitTheme
import com.michaelblades.echokit.ui.IdeasListScreen
import com.michaelblades.echokit.ui.IdeasViewModel

@Composable
fun MyIdeasScreen(client: EchoKitClient) {
    val viewModel: IdeasViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return IdeasViewModel(client) as T
            }
        }
    )

    EchoKitTheme {
        IdeasListScreen(
            viewModel = viewModel,
            onIdeaClick = { ideaId ->
                // Navigate to idea detail
            }
        )
    }
}
```

#### Idea Detail Screen

```kotlin
import com.michaelblades.echokit.ui.IdeaDetailScreen
import com.michaelblades.echokit.ui.IdeaDetailViewModel

@Composable
fun MyIdeaDetailScreen(client: EchoKitClient, ideaId: String) {
    val viewModel: IdeaDetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return IdeaDetailViewModel(client, ideaId) as T
            }
        }
    )

    EchoKitTheme {
        IdeaDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { /* Handle back navigation */ }
        )
    }
}
```

### Option 2: Use the Client Directly

For custom UI implementations, use the `EchoKitClient` API directly:

#### Fetch Ideas

```kotlin
// Get all approved ideas
val ideas = client.getIdeas(onlyApproved = true)

// Filter by status
val pendingIdeas = client.getIdeas(
    status = EchoKitClient.IdeaStatus.PENDING,
    onlyApproved = true
)
```

#### Get Idea Details

```kotlin
val ideaDetail = client.getIdeaDetail(ideaId = "idea_123")

// Access comments
ideaDetail.comments.forEach { comment ->
    println("${comment.createdBy}: ${comment.body}")
}
```

#### Create an Idea

```kotlin
val newIdea = client.createIdea(
    title = "Add dark mode support",
    body = "It would be great to have a dark mode option for the app."
)
```

#### Vote on an Idea

```kotlin
client.vote(ideaId = "idea_123")
```

#### Add a Comment

```kotlin
val comment = client.addComment(
    ideaId = "idea_123",
    body = "Great idea! I'd love to see this implemented."
)
```

### Full Navigation Example

Here's a complete example with navigation between screens:

```kotlin
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michaelblades.echokit.EchoKitClient
import com.michaelblades.echokit.EchoKitTheme
import com.michaelblades.echokit.ui.*

@Composable
fun EchoKitApp(client: EchoKitClient) {
    val navController = rememberNavController()

    EchoKitTheme {
        NavHost(
            navController = navController,
            startDestination = "ideas_list"
        ) {
            composable("ideas_list") {
                val viewModel: IdeasViewModel = viewModel(
                    factory = IdeasViewModelFactory(client)
                )
                IdeasListScreen(
                    viewModel = viewModel,
                    onIdeaClick = { ideaId ->
                        navController.navigate("idea_detail/$ideaId")
                    }
                )
            }

            composable("idea_detail/{ideaId}") { backStackEntry ->
                val ideaId = backStackEntry.arguments?.getString("ideaId") ?: return@composable
                val viewModel: IdeaDetailViewModel = viewModel(
                    factory = IdeaDetailViewModelFactory(client, ideaId)
                )
                IdeaDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ViewModel Factories
class IdeasViewModelFactory(
    private val client: EchoKitClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return IdeasViewModel(client) as T
    }
}

class IdeaDetailViewModelFactory(
    private val client: EchoKitClient,
    private val ideaId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return IdeaDetailViewModel(client, ideaId) as T
    }
}
```

## Data Models

### Idea

```kotlin
data class Idea(
    val id: String,
    val title: String,
    val body: String?,
    val status: IdeaStatus,
    val category: IdeaCategory,
    val isApproved: Boolean,
    val voteCount: Int,
    val commentCount: Int,
    val createdBy: String,
    val createdAt: String?,
    val userHasVoted: Boolean
)
```

### IdeaStatus

```kotlin
enum class IdeaStatus {
    PENDING,      // Awaiting review
    IN_PROGRESS,  // Being worked on
    COMPLETED     // Done
}
```

### IdeaCategory

```kotlin
enum class IdeaCategory {
    NEW_IDEA,     // New idea
    FEATURE,      // Feature request
    ENHANCEMENT,  // Enhancement to existing feature
    INTEGRATION,  // Integration request
    UI_UX         // UI/UX improvement
}
```

### Comment

```kotlin
data class Comment(
    val id: String,
    val body: String,
    val createdBy: String,
    val createdAt: String?
)
```

## Requirements

- Android API 24+ (Android 7.0)
- Kotlin 1.9+
- Jetpack Compose

## Dependencies

The library includes the following dependencies (automatically included):
- OkHttp 4.12.0
- Kotlinx Serialization 1.6.0
- Kotlinx Coroutines 1.7.3
- Jetpack Compose (Material 3)
- Accompanist SwipeRefresh 0.32.0
