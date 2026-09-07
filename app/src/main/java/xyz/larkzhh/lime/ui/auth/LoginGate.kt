package xyz.larkzhh.lime.ui.auth


object LoginGate {
    var onRequireLogin: (targetRoute: String?) -> Boolean = { false }
}
