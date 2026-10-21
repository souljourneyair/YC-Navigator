package com.yc.navigator.data

class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DataCorruptedException(message: String, cause: Throwable? = null) : Exception(message, cause)
