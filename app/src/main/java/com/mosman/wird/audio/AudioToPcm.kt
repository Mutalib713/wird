package com.mosman.wird.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Turning a recording into the numbers whisper.cpp wants. **PLAN task 14.**
 *
 * The recorder writes **mono AAC at 44,100 Hz inside an MP4**, because that is a sensible thing
 * to keep a recitation in. whisper wants **16,000 Hz mono floats between -1 and 1**, because
 * that is what every Whisper model was trained on. Neither side is wrong; this is the bridge.
 *
 * ⚠ **Feeding whisper audio at the wrong rate does not fail — it transcribes nonsense.** There
 * is no error and no exception; the model simply hears a voice pitched and paced wrongly and
 * returns confident rubbish. That is the single most important reason this file exists as its
 * own testable thing rather than as a few lines inside the recogniser.
 *
 * **Nothing here is streamed.** CLAUDE.md, measured earlier in this project: *whisper.cpp on
 * Android — batch is fast, streaming is ~5x slower than real time. Always batch.* A recitation
 * is already finished by the time anyone asks about it, so there is nothing to stream anyway.
 */
object AudioToPcm {

    /** What every Whisper model was trained on. Not a preference. */
    const val WHISPER_HZ = 16_000

    /**
     * Decode [file] to mono 16 kHz floats, or null if it cannot be read.
     *
     * Null rather than an exception: a recording that will not decode is a real state — a
     * truncated file, a codec the phone lacks — and the caller's honest answer is "I could not
     * listen to that", not a crash.
     */
    suspend fun read(file: File): FloatArray? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)

            val track = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return@withContext null

            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            val sourceHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val pcm = drain(extractor, codec, channels)
            Log.i(TAG, "decoded ${file.name}: ${pcm.size} samples at $sourceHz Hz, $channels ch")

            if (pcm.isEmpty()) null else resample(pcm, sourceHz, WHISPER_HZ)
        } catch (e: Exception) {
            // Anything from a missing codec to a half-written file. Every one of them means the
            // same thing to the caller, and none of them should take the app down.
            Log.w(TAG, "could not decode ${file.name}", e)
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /**
     * Pull every decoded frame out of the codec, as floats in -1..1.
     *
     * Synchronous `dequeue` rather than callbacks: this runs on a background thread with nothing
     * else to do, and the callback form buys concurrency that would only add ordering bugs to a
     * job whose whole nature is sequential.
     */
    private fun drain(
        extractor: MediaExtractor,
        codec: MediaCodec,
        channels: Int,
    ): FloatArray {
        val out = ArrayList<Float>(INITIAL_CAPACITY)
        val info = MediaCodec.BufferInfo()
        var fed = false

        while (true) {
            if (!fed) {
                val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)!!
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(
                            inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        fed = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (outIndex >= 0) {
                val buffer = codec.getOutputBuffer(outIndex)!!
                    .order(ByteOrder.nativeOrder())
                appendMono(buffer, info.offset, info.size, channels, out)
                codec.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER && fed) {
                // Fed everything and the codec has stopped producing. Some devices never set
                // the end-of-stream flag, so without this the loop would spin forever on a
                // file that has already been fully decoded.
                break
            }
        }
        return out.toFloatArray()
    }

    /**
     * 16-bit samples to floats, downmixing to one channel on the way.
     *
     * The recorder already records mono, so the averaging below is for recordings that came from
     * somewhere else — an imported file, or a future recorder that changes its mind. **Averaging
     * rather than taking the left channel**, because dropping a channel throws away half of a
     * voice that happened to be panned.
     */
    private fun appendMono(
        buffer: ByteBuffer,
        offset: Int,
        size: Int,
        channels: Int,
        out: ArrayList<Float>,
    ) {
        // ⚠ Chaining these would not compile: on Android `ByteBuffer.position(int)` is declared
        // to return the base `Buffer`, which has no `asShortBuffer`. Separate statements keep
        // the type.
        buffer.position(offset)
        buffer.limit(offset + size)
        val shorts = buffer.asShortBuffer()
        val frames = shorts.remaining() / channels.coerceAtLeast(1)
        repeat(frames) {
            var sum = 0f
            repeat(channels) { sum += shorts.get() / Short.MAX_VALUE.toFloat() }
            out.add(sum / channels)
        }
    }

    /**
     * 44,100 Hz down to 16,000, by averaging each output sample's window of input.
     *
     * ⚠ **The averaging is not laziness — it is the anti-aliasing.** Simply picking the nearest
     * input sample (the obvious way to resample) folds every frequency above 8 kHz back down
     * into the speech range as a shrill ghost, and the model hears a voice with noise mixed
     * through it. Averaging the whole window is a crude low-pass filter, which is exactly the
     * thing that must happen before throwing samples away.
     *
     * The ratio here is 2.756 and not a whole number, so windows do not line up with sample
     * boundaries; the window is computed per output sample rather than fixed.
     */
    internal fun resample(input: FloatArray, fromHz: Int, toHz: Int): FloatArray {
        if (fromHz == toHz || input.isEmpty()) return input

        val ratio = fromHz.toDouble() / toHz
        val outSize = (input.size / ratio).toInt().coerceAtLeast(1)
        val out = FloatArray(outSize)

        for (i in 0 until outSize) {
            val start = (i * ratio).toInt()
            val end = (((i + 1) * ratio).toInt()).coerceAtMost(input.size)
            if (start >= input.size) break
            var sum = 0f
            var n = 0
            for (j in start until end.coerceAtLeast(start + 1)) {
                if (j >= input.size) break
                sum += input[j]
                n++
            }
            out[i] = if (n > 0) sum / n else input[start]
        }
        return out
    }

    private const val TIMEOUT_US = 10_000L

    /** About ten seconds of 44.1 kHz audio, so a short recitation never reallocates. */
    private const val INITIAL_CAPACITY = 441_000

    private const val TAG = "WirdPcm"
}
