package xyz.larkzhh.lime.ui.components

object LoginGate {
    var onRequireLogin: (targetRoute: String?) -> Boolean = { false }
}
