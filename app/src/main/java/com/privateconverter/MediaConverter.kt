package com.privateconverter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

object MediaConverter {
    fun convert(context: Context, source: Uri, outputTreeUri: Uri, originalName: String, jpegQuality: Int = 95): String {
        val tree = DocumentFile.fromTreeUri(context, outputTreeUri)
            ?: throw IOException("Output folder is unavailable")
        val lower = originalName.lowercase()
        return when {
            lower.endsWith(".heic") || lower.endsWith(".heif") -> convertImage(context, source, tree, originalName, jpegQuality.coerceIn(50, 100))
            lower.endsWith(".mov") -> convertVideo(context, source, tree, originalName)
            else -> throw IOException("Unsupported extension. Choose a .heic, .heif, or .mov file.")
        }
    }

    private fun convertImage(context: Context, source: Uri, tree: DocumentFile, name: String, quality: Int): String {
        val outputName = uniqueName(tree, name.substringBeforeLast('.', name) + ".jpg")
        val output = tree.createFile("image/jpeg", outputName) ?: throw IOException("Could not create JPG output")
        try {
            val imageSource = ImageDecoder.createSource(context.contentResolver, source)
            val bitmap = ImageDecoder.decodeBitmap(imageSource) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
            context.contentResolver.openOutputStream(output.uri, "w").use { stream ->
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                    throw IOException("JPEG encoding failed")
                }
                stream.flush()
            }
            bitmap.recycle()
            return output.name ?: outputName
        } catch (e: Exception) {
            output.delete()
            throw IOException("Image decode/encode failed; this device may not support this HEIC/HEIF variant. ${e.message}", e)
        }
    }

    private fun convertVideo(context: Context, source: Uri, tree: DocumentFile, name: String): String {
        val outputName = uniqueName(tree, name.substringBeforeLast('.', name) + ".mp4")
        val output = tree.createFile("video/mp4", outputName) ?: throw IOException("Could not create MP4 output")
        var pfd: ParcelFileDescriptor? = null
        var muxer: MediaMuxer? = null
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, source, null)
            if (extractor.trackCount == 0) throw IOException("The MOV file has no readable tracks")
            pfd = context.contentResolver.openFileDescriptor(output.uri, "rw")
                ?: throw IOException("Could not open MP4 output")
            muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    // This path remuxes compatible encoded tracks; it does not transcode codecs.
                    trackMap[i] = muxer.addTrack(format)
                    extractor.selectTrack(i)
                }
            }
            if (trackMap.isEmpty()) throw IOException("No audio/video tracks were found")
            muxer.start()
            val buffer = java.nio.ByteBuffer.allocate(2 * 1024 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                if (info.size > buffer.capacity()) throw IOException("A video sample exceeds the supported buffer size")
                info.presentationTimeUs = extractor.sampleTime
                info.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else 0
                val destinationTrack = trackMap[extractor.sampleTrackIndex]
                if (destinationTrack != null) muxer.writeSampleData(destinationTrack, buffer, info)
                if (!extractor.advance()) break
            }
            muxer.stop()
            muxer.release()
            muxer = null
            pfd.close()
            pfd = null
            return output.name ?: outputName
        } catch (e: Exception) {
            try { muxer?.stop() } catch (_: Exception) { }
            try { muxer?.release() } catch (_: Exception) { }
            try { pfd?.close() } catch (_: Exception) { }
            output.delete()
            throw IOException("MOV could not be packaged as MP4 with its existing codecs. This initial version remuxes compatible streams; it does not transcode unsupported codecs. ${e.message}", e)
        } finally {
            extractor.release()
        }
    }

    private fun uniqueName(tree: DocumentFile, desired: String): String {
        if (tree.findFile(desired) == null) return desired
        val base = desired.substringBeforeLast('.', desired)
        val ext = desired.substringAfterLast('.', "")
        var i = 1
        while (tree.findFile("$base ($i).$ext") != null) i++
        return "$base ($i).$ext"
    }
}
