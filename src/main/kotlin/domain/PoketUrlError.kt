package domain

sealed interface PoketUrlError {
    data class InvalidScheme(val scheme: String) : PoketUrlError

    data object EmptyShortCode : PoketUrlError

    data object OriginalUrlExceedLength : PoketUrlError
}
