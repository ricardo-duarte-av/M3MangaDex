package pt.aguiarvieira.m3mangadex.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.auth.AuthRepository
import pt.aguiarvieira.m3mangadex.core.auth.SavedClient
import pt.aguiarvieira.m3mangadex.core.auth.Session
import pt.aguiarvieira.m3mangadex.core.data.LibraryRepository
import pt.aguiarvieira.m3mangadex.core.network.AuthException
import java.io.IOException
import javax.inject.Inject

enum class LoginError { Credentials, Client, Attempts, Offline }

data class LoginUiState(
    val busy: Boolean = false,
    val error: LoginError? = null,
    val done: Boolean = false,
    /** The client and user of an earlier session, to pre-fill. */
    val saved: SavedClient? = null,
    val expired: Boolean = false,
)

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val auth: AuthRepository,
        private val library: LibraryRepository,
    ) : ViewModel() {
        private val _state =
            MutableStateFlow(LoginUiState(saved = auth.savedClient(), expired = auth.session.value is Session.Expired))
        val state: StateFlow<LoginUiState> = _state.asStateFlow()

        fun login(
            clientId: String,
            clientSecret: String,
            username: String,
            password: String,
        ) {
            if (_state.value.busy) return
            _state.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                val error =
                    try {
                        auth.login(clientId, clientSecret, username, password)
                        null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: AuthException.BadClient) {
                        LoginError.Client
                    } catch (_: AuthException.TooManyAttempts) {
                        LoginError.Attempts
                    } catch (_: AuthException) {
                        LoginError.Credentials
                    } catch (_: IOException) {
                        LoginError.Offline
                    }
                if (error == null) runCatching { library.refreshStatuses() }
                _state.update { it.copy(busy = false, error = error, done = error == null) }
            }
        }
    }
