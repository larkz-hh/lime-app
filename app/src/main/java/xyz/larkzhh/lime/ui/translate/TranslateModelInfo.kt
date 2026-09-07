package xyz.larkzhh.lime.ui.translate

/// 离线语言包信息工具类
object TranslateModelInfo {

    private const val EN_ZH_PACK_BYTES = 35_347_571L

    /// 获取下载体积
    fun downloadSizeBytes(): Long = EN_ZH_PACK_BYTES

    /// MB 格式
    fun downloadSizeLabel(): String =
        "%.1fMB".format(downloadSizeBytes() / 1_000_000.0)
}
