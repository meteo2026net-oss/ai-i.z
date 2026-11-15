package com.example.core.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/**
 * Produces a support-safe error summary.
 *
 * Raw exception messages, user identifiers, email addresses, provider metadata,
 * document identifiers, and authentication details must never be written to logs.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val firestoreError = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    val rootError = causes.firstOrNull { it !is CancellationException } ?: exception

    val safePath = path
        ?.split("/")
        ?.mapIndexed { index, segment ->
            if (index % 2 == 1) "{id}" else segment
        }
        ?.joinToString("/")

    val errorInfoJson = JSONObject().apply {
        put("errorType", rootError::class.java.simpleName.take(80))
        put("firestoreCode", firestoreError?.code?.name ?: JSONObject.NULL)
        put("operationType", operationType.value)
        put("resourcePathTemplate", safePath ?: JSONObject.NULL)
    }

    val safeSummary = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore operation failed: $safeSummary")
    return safeSummary
}
