package unq.pdes.backend.external.flights

class FlightsServiceUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
