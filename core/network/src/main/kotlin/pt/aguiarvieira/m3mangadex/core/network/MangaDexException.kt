package pt.aguiarvieira.m3mangadex.core.network

import java.io.IOException

/** A non-2xx answer from MangaDex, with the first error's detail when it sent one. */
class MangaDexException(
    val status: Int,
    message: String,
) : IOException(message)
