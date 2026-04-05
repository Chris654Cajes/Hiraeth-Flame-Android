package com.hiraeth.flame.data.local

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Trims the **first video track** found in the container to [startUs]–[endUs] (microseconds).
 * Audio is not copied in this sample path — for full A/V pipelines use Media3 Transformer.
 */
object VideoTrimmer {

    suspend fun trimVideoTrackToFile(
        input: File,
        output: File,
        startUs: Long,
        endUs: Long,
    ): Unit = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        extractor.setDataSource(input.absolutePath)

        var videoTrack = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) {
                videoTrack = i
                format = f
                break
            }
        }
        if (videoTrack < 0 || format == null) {
            extractor.release()
            error("No video track in ${input.name}")
        }

        extractor.selectTrack(videoTrack)
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val outTrack = muxer.addTrack(format)
        muxer.start()

        val bufferSize = 256 * 1024
        val buffer = ByteBuffer.allocate(bufferSize)
        val info = MediaCodec.BufferInfo()

        try {
            while (true) {
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                val time = extractor.sampleTime
                if (time > endUs) break
                info.presentationTimeUs = time - startUs
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(outTrack, buffer, info)
                if (!extractor.advance()) break
            }
        } finally {
            try {
                muxer.stop()
            } catch (_: Exception) {
            }
            muxer.release()
            extractor.release()
        }
    }
}
