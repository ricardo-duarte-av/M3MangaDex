package pt.aguiarvieira.m3mangadex.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loggedIn by rememberUpdatedState(onLoggedIn)
    LaunchedEffect(state.done) { if (state.done) loggedIn() }
    LoginScreen(state = state, onBack = onBack, onLogin = viewModel::login, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onLogin: (clientId: String, clientSecret: String, username: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clientId = rememberTextFieldState(state.saved?.clientId.orEmpty())
    val clientSecret = rememberTextFieldState(state.saved?.clientSecret.orEmpty())
    val username = rememberTextFieldState(state.saved?.username.orEmpty())
    val password = rememberTextFieldState()
    val uriHandler = LocalUriHandler.current
    val submit = {
        onLogin(
            clientId.text.toString(),
            clientSecret.text.toString(),
            username.text.toString(),
            password.text.toString()
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(DsR.drawable.ic_arrow_back), stringResource(DsR.string.ds_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.expired) {
                    Text(
                        stringResource(R.string.login_expired),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ClientHelp(onOpenSettings = { uriHandler.openUri(MANGADEX_SETTINGS) })
                }
                Field(clientId, R.string.login_client_id, KeyboardType.Ascii)
                OutlinedSecureTextField(
                    state = clientSecret,
                    label = { Text(stringResource(R.string.login_client_secret)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Field(username, R.string.login_username, KeyboardType.Email)
                OutlinedSecureTextField(
                    state = password,
                    label = { Text(stringResource(R.string.login_password)) },
                    onKeyboardAction = { submit() },
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let {
                    Text(
                        stringResource(it.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = submit,
                    enabled =
                        !state.busy && clientId.text.isNotBlank() && clientSecret.text.isNotBlank() &&
                            username.text.isNotBlank() && password.text.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.busy) LoadingIndicator(Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.login_submit))
                }
                Text(
                    stringResource(R.string.login_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ClientHelp(onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.login_why), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.login_steps), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.login_open_settings)) }
        }
    }
}

@Composable
private fun Field(
    state: TextFieldState,
    label: Int,
    keyboard: KeyboardType,
) {
    OutlinedTextField(
        state = state,
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, autoCorrectEnabled = false),
        lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val LoginError.message: Int
    get() =
        when (this) {
            LoginError.Credentials -> R.string.login_error_credentials
            LoginError.Client -> R.string.login_error_client
            LoginError.Attempts -> R.string.login_error_attempts
            LoginError.Offline -> R.string.login_error_offline
        }

private const val MANGADEX_SETTINGS = "https://mangadex.org/settings"
