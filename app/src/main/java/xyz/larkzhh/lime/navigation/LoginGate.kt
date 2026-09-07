package xyz.larkzhh.lime.navigation


object LoginGate {
    var onRequireLogin: (targetRoute: String?) -> Boolean = { false }
}
