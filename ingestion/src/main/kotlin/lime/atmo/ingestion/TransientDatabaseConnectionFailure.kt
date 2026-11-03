package lime.atmo.ingestion

import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.sql.SQLException

internal object TransientDatabaseConnectionFailure {
    fun matches(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is SQLException && current.sqlState?.startsWith("08") == true) {
                return true
            }
            if (current is EOFException || current is ConnectException ||
                current is SocketException || current is SocketTimeoutException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
