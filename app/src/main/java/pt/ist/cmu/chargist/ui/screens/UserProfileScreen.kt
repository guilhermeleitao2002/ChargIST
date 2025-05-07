package pt.ist.cmu.chargist.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.ui.viewmodel.UserViewModel

/* ──────────────────── small helpers ──────────────────── */

private fun String.normalisedEmail(): String = trim().lowercase()

private fun String.isValidEmail(): Boolean =
    isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(normalisedEmail()).matches()

/* ─────────────────────────  MAIN SCREEN  ───────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBackClick: () -> Unit,
    viewModel: UserViewModel = koinViewModel()
) {
    val userState by viewModel.userState.collectAsState()

    /* local UI state */
    var username       by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var showSignUpForm by remember { mutableStateOf(false) }
    var showLoginForm  by remember { mutableStateOf(false) }

    val snack     = remember { SnackbarHostState() }
    val coroutine = rememberCoroutineScope()

    /* surface ViewModel errors */
    LaunchedEffect(userState.error) {
        userState.error?.let { coroutine.launch { snack.showSnackbar(it) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.Center
        ) {
            when {
                userState.isLoading -> Text("Loading…")

                userState.user != null -> LoggedInContent(
                    username = userState.user!!.username,
                    uid      = userState.user!!.id,
                    onLogout = { viewModel.logout() }
                )

                showSignUpForm -> SignUpForm(
                    email       = email,
                    onEmail     = { email = it },
                    password    = password,
                    onPassword  = { password = it },
                    username    = username,
                    onUsername  = { username = it },
                    onCancel    = { showSignUpForm = false },
                    onSubmit    = {
                        val mail = email.normalisedEmail()
                        if (mail.isValidEmail()) {
                            viewModel.signUp(username.trim(), mail, password.trim())
                            showSignUpForm = false
                        } else {
                            coroutine.launch { snack.showSnackbar("Please enter a valid e‑mail") }
                        }
                    },
                    switchToLogin = {
                        showSignUpForm = false
                        showLoginForm  = true
                    }
                )

                showLoginForm -> LoginForm(
                    email      = email,
                    onEmail    = { email = it },
                    password   = password,
                    onPassword = { password = it },
                    onCancel   = { showLoginForm = false },
                    onSubmit   = {
                        val mail = email.normalisedEmail()
                        if (mail.isValidEmail()) {
                            viewModel.login(mail, password.trim())
                            showLoginForm = false
                        } else {
                            coroutine.launch { snack.showSnackbar("Please enter a valid e‑mail") }
                        }
                    },
                    switchToSignUp = {
                        showLoginForm  = false
                        showSignUpForm = true
                    }
                )

                else -> WelcomeScreen(
                    onCreateAccount = { showSignUpForm = true },
                    onLogin         = { showLoginForm  = true }
                )
            }
        }
    }
}

/* ─────────────────────────  SUB‑COMPOSABLES  ───────────────────────── */

@Composable
private fun LoggedInContent(
    username: String,
    uid: String,
    onLogout: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AccountCircle, null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(16.dp))

        Text(username, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("ID: ${uid.take(8)}…", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(Modifier.height(32.dp))

        ProfileMenuItem(Icons.Default.Favorite, "My Favorites",
            "View and manage your favorite charging stations") { }
        Spacer(Modifier.height(16.dp))
        ProfileMenuItem(Icons.Default.Create, "My Contributions",
            "Charging stations and reports you've added") { }
        Spacer(Modifier.height(16.dp))
        ProfileMenuItem(Icons.Default.Settings, "Settings",
            "App preferences and account settings") { }

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick  = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Logout") }
    }
}

@Composable
private fun WelcomeScreen(onCreateAccount: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AccountCircle, null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(16.dp))

        Text("Welcome to ChargIST",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        Text(
            "Create an account to save your favorite charging stations " +
                    "and contribute to the community",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
            Text("Create account")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Log in")
        }
    }
}

/* ───────────── Sign‑up / Log‑in forms reuse generic AuthForm ───────────── */

@Composable
private fun SignUpForm(
    email: String,
    onEmail: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    username: String,
    onUsername: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    switchToLogin: () -> Unit
) {
    AuthForm(
        title        = "Create Account",
        email        = email,      onEmail     = onEmail,
        password     = password,   onPassword  = onPassword,
        extraLabel   = "Username", extraValue  = username,
        extraOnChange= onUsername,
        submitText   = "Sign up",
        onCancel     = onCancel,
        onSubmit     = onSubmit,
        bottomLink   = "Already have an account? Log in",
        onBottomLink = switchToLogin,
        submitEnabled= email.isValidEmail() &&
                password.isNotBlank() &&
                username.isNotBlank()
    )
}

@Composable
private fun LoginForm(
    email: String,
    onEmail: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    switchToSignUp: () -> Unit
) {
    AuthForm(
        title        = "Log in",
        email        = email,     onEmail     = onEmail,
        password     = password,  onPassword  = onPassword,
        submitText   = "Log in",
        onCancel     = onCancel,
        onSubmit     = onSubmit,
        bottomLink   = "Need an account? Sign up",
        onBottomLink = switchToSignUp,
        submitEnabled= email.isValidEmail() && password.isNotBlank()
    )
}

/* ───────────────────── generic Auth form UI ───────────────────── */

@Composable
private fun AuthForm(
    title: String,
    email: String,
    onEmail: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    submitText: String,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    bottomLink: String,
    onBottomLink: () -> Unit,
    extraLabel: String? = null,
    extraValue: String = "",
    extraOnChange: (String) -> Unit = {},
    submitEnabled: Boolean
) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { onEmail(it.lowercase()) },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPassword,
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (extraLabel != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = extraValue,
                onValueChange = extraOnChange,
                label = { Text(extraLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick  = onSubmit,
                enabled  = submitEnabled,
                modifier = Modifier.weight(1f)
            ) { Text(submitText) }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onBottomLink) { Text(bottomLink) }
    }
}

/* ───────────────────────── Profile menu item ───────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                modifier = Modifier.size(40.dp)
                    .clip(CircleShape)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
