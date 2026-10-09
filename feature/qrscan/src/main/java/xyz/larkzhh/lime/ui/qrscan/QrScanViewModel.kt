package xyz.larkzhh.lime.ui.qrscan

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    suspend fun resolveUserId(identifier: String): Long? {
        identifier.toLongOrNull()?.let { return it }
        return userRepository.getUserByUid(identifier).getOrNull()?.id
            ?: userRepository.getUserByHandle(identifier).getOrNull()?.id
    }
}
