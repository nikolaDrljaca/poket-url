package domain

sealed interface OriginalUrlError {
    data object ExceedLength : OriginalUrlError
    data class InvalidScheme(val scheme: String) : OriginalUrlError
}
