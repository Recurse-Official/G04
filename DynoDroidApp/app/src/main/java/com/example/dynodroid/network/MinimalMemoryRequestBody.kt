package com.example.dynodroid.network


import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.math.min


class MinimalMemoryRequestBody(
    private val file: File,
    private val contentType: String,
    private val startByte: Long = 0,
    private val endByte: Long = file.length() - 1,
    private val bufferSize: Int = 4096 // 4KB buffer
) : RequestBody() {
    override fun contentType() = contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = endByte - startByte + 1

    override fun writeTo(sink: BufferedSink) {
        FileInputStream(file).channel.use { channel ->
            channel.position(startByte)
            val buffer = ByteArray(bufferSize)
            var bytesRemaining = contentLength()
            while (bytesRemaining > 0) {
                val bytesToRead = min(buffer.size.toLong(), bytesRemaining).toInt()
                val bytesRead = channel.read(ByteBuffer.wrap(buffer, 0, bytesToRead))
                if (bytesRead == -1) break
                sink.write(buffer, 0, bytesRead)
                sink.flush()
                bytesRemaining -= bytesRead
                // Force garbage collection after each write
                System.gc()
            }
        }
    }
}