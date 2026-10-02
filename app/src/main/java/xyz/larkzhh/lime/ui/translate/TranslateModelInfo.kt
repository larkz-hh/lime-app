package xyz.larkzhh.lime.ui.translate

/// 离线语言包信息工具类
object TranslateModelInfo {

    private const val EN_ZH_PACK_BYTES = 35_347_571L
    private const val SPEECH_PACK_BYTES = 43_898_754L

    /// 翻译语言包
    fun downloadSizeLabel(): String =
        "%.1fMB".format(EN_ZH_PACK_BYTES / 1_000_000.0)

    /// 语音包
    fun speechPackSizeLabel(): String =
        "%.1fMB".format(SPEECH_PACK_BYTES / 1_000_000.0)
}
