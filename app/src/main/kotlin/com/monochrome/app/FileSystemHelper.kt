package com.monochrome.app

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.monochrome.app.Constants.MIME_MPEG
import org.json.JSONArray
import org.json.JSONObject

object FileSystemHelper {

    fun enumerateAudioFiles(contentResolver: ContentResolver, treeUri: Uri): List<Triple<Uri, String, String>> {
        val results = mutableListOf<Triple<Uri, String, String>>()
        val audioExts = mapOf(
            "flac" to "audio/flac",
            "wav" to "audio/wav",
            "mp3" to "audio/mpeg",
            "aac" to "audio/aac",
            "ogg" to "audio/ogg",
            "opus" to "audio/opus",
            "m4a" to "audio/mp4",
            "alac" to "audio/alac",
            "aiff" to "audio/x-aiff"
        )

        fun scanDir(docId: String) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val iName = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val iMime = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (c.moveToNext()) {
                    val childId = c.getString(iId) ?: continue
                    val name = c.getString(iName) ?: continue
                    var mime = c.getString(iMime) ?: ""
                    
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanDir(childId)
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val isAudioMime = mime.startsWith("audio/")
                        val knownMime = audioExts[ext]
                        
                        if (isAudioMime || knownMime != null) {
                            // If mime is generic, try to use the more specific one from extension
                            if ((mime == "application/octet-stream" || mime == "audio/mpeg") && knownMime != null) {
                                mime = knownMime
                            }
                            results.add(Triple(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId), name, mime))
                        }
                    }
                }
            }
        }

        try {
            scanDir(DocumentsContract.getTreeDocumentId(treeUri))
        } catch (_: Exception) { }
        return results
    }

    fun buildJson(files: List<Triple<Uri, String, String>>): String {
        val array = JSONArray()
        files.forEach { (uri, name, mime) ->
            val obj = JSONObject()
            obj.put("name", name)
            obj.put("uri", uri.toString())
            obj.put("mimeType", mime.ifBlank { MIME_MPEG })
            array.put(obj)
        }
        return array.toString()
    }
}
