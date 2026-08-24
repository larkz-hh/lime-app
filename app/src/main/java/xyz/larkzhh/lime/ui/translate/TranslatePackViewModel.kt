package xyz.larkzhh.lime.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import xyz.larkzhh.lime.data.local.TranslatorHolder
import javax.inject.Inject

/// 语言包状态
enum class PackStatus { Checking, NotDownloaded, Downloading, Downloaded, Failed }

data class PackUiState(val status: PackStatus = PackStatus.Checking)

/**
 * 语言包管理 ViewModel
 * 检查语言包、下载语言包、删除语言包
 */
@HiltViewModel
class TranslatePackViewModel @Inject constructor(
    private val translatorHolder: TranslatorHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackUiState())
    val uiState: StateFlow<PackUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    init { refresh() }

    /// 检查语言包是否已在本机
    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(status = PackStatus.Checking) }
            val downloaded = try {
                RemoteModelManager.getInstance()
                    .isModelDownloaded(chineseModel())
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _uiState.update {
                it.copy(status = if (downloaded) PackStatus.Downloaded else PackStatus.NotDownloaded)
            }
        }
    }

    /// 下载中英语言包
    fun download() {
        if (_uiState.value.status == PackStatus.Downloading) return
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(status = PackStatus.Downloading) }
            try {
                translatorHolder.ensureModel("zh", "en")
                _uiState.update { it.copy(status = PackStatus.Downloaded) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(status = PackStatus.Failed) }
            }
        }
    }

    /// 删除已下载的语言包
    fun delete() {
        if (_uiState.value.status != PackStatus.Downloaded) return
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(status = PackStatus.Checking) }
            try {
                RemoteModelManager.getInstance()
                    .deleteDownloadedModel(chineseModel())
                    .await()
                _uiState.update { it.copy(status = PackStatus.NotDownloaded) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 删除失败保持已下载状态
                _uiState.update { it.copy(status = PackStatus.Downloaded) }
            }
        }
    }

    private fun chineseModel() = TranslateRemoteModel.Builder("zh").build()
}
