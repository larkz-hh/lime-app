package xyz.larkzhh.lime.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import xyz.larkzhh.lime.data.local.SpeechEngine
import xyz.larkzhh.lime.data.local.SpeechModelStore
import xyz.larkzhh.lime.data.local.SpeechPackUiState
import xyz.larkzhh.lime.data.local.TranslateMode
import xyz.larkzhh.lime.data.local.TranslateSettings
import xyz.larkzhh.lime.data.local.TranslatorHolder
import xyz.larkzhh.lime.work.SpeechPackDownloadWorker
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
    private val settings: TranslateSettings,
    private val speechStore: SpeechModelStore,
    private val speechEngine: SpeechEngine,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackUiState())
    val uiState: StateFlow<PackUiState> = _uiState.asStateFlow()

    /// 语音包状态
    val speechState: StateFlow<SpeechPackUiState> = speechStore.state

    /// 翻译方式
    val mode: StateFlow<TranslateMode> = settings.mode

    fun setMode(mode: TranslateMode) = settings.setMode(mode)

    private var job: Job? = null
    private var downloadJob: Job? = null

    init { refresh() }

    /// 检查语言包是否已在本机
    fun refresh() {
        if (downloadJob?.isActive == true) {
            viewModelScope.launch { speechStore.refresh() }
            return
        }
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(status = PackStatus.Checking) }
            val downloaded = queryDownloaded()
            _uiState.update {
                it.copy(status = if (downloaded) PackStatus.Downloaded else PackStatus.NotDownloaded)
            }
            speechStore.refresh()
        }
    }

    /// 静默检查
    fun refreshQuietly() {
        if (downloadJob?.isActive == true) return
        viewModelScope.launch {
            if (queryDownloaded() && _uiState.value.status != PackStatus.Downloaded) {
                _uiState.update { it.copy(status = PackStatus.Downloaded) }
            }
        }
    }

    private suspend fun queryDownloaded(): Boolean = try {
        RemoteModelManager.getInstance()
            .isModelDownloaded(chineseModel())
            .await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    /// 下载语音包
    fun downloadSpeech() {
        SpeechPackDownloadWorker.enqueue(context)
    }

    /// 删除语音包
    fun deleteSpeech() {
        viewModelScope.launch {
            speechEngine.release()
            speechStore.delete()
        }
    }

    /// 下载中英语言包
    fun download() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
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
