package xyz.larkzhh.lime.ui.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    val user: StateFlow<UserData?> = userRepository.userFlow

    init {
        if (userRepository.userFlow.value == null) {
            viewModelScope.launch {
                userRepository.refreshUser()
            }
        }
    }
}
