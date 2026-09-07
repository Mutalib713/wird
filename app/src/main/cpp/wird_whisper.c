#include <jni.h>
#include <android/log.h>
#include <string.h>
#include "whisper.h"

#define TAG "WirdNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

/*
 * Wird's own bridge to whisper.cpp. PLAN task 14.
 *
 * ⚠ WHY THIS EXISTS, WHEN UPSTREAM SHIPS ONE.
 *
 * The first build compiled whisper.cpp's own Android bridge unmodified, to avoid a fork. That
 * was the wrong call for one line:
 *
 *     params.language = "en";
 *
 * Their file is a DEMO, and it carries a demo's assumptions. Wird records Qur'anic Arabic and
 * runs a model trained on Qur'anic Arabic; telling it the audio is English is not a tuning
 * detail, it is asking the wrong question. Mutalib's first real check ran at 287% CPU for over
 * a hundred seconds on the strength of it.
 *
 * This is not a fork of whisper.cpp. It is our own sixty lines calling their library, which is
 * the ordinary way to use a C library — the mistake was borrowing an example instead.
 */

/* Arabic. The whole reason this file exists. */
static const char *WIRD_LANGUAGE = "ar";

/* Logged as it advances, so progress is observable without a stopwatch. */
static void wird_progress(struct whisper_context *ctx, struct whisper_state *state,
                          int progress, void *user_data) {
    (void) ctx;
    (void) state;
    (void) user_data;
    LOGI("progress %d%%", progress);
}

/* Route internal whisper and ggml messages to logcat. */
static void wird_whisper_log(enum ggml_log_level level, const char *text, void *user_data) {
    (void) user_data;
    if (text == NULL || text[0] == '\0' || (text[0] == '\n' && text[1] == '\0')) return;
    int priority = ANDROID_LOG_INFO;
    switch (level) {
        case GGML_LOG_LEVEL_ERROR: priority = ANDROID_LOG_ERROR; break;
        case GGML_LOG_LEVEL_WARN:  priority = ANDROID_LOG_WARN; break;
        case GGML_LOG_LEVEL_DEBUG: priority = ANDROID_LOG_DEBUG; break;
        default:                   priority = ANDROID_LOG_INFO; break;
    }
    __android_log_print(priority, TAG, "%s", text);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) vm;
    (void) reserved;
    whisper_log_set(wird_whisper_log, NULL);
    ggml_log_set(wird_whisper_log, NULL);
    return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL
Java_com_mosman_wird_audio_WhisperNative_initContext(
        JNIEnv *env, jobject thiz, jstring model_path) {
    (void) thiz;
    whisper_log_set(wird_whisper_log, NULL);
    ggml_log_set(wird_whisper_log, NULL);
    const char *path = (*env)->GetStringUTFChars(env, model_path, NULL);

    struct whisper_context_params cparams = whisper_context_default_params();
    /* No GPU on Android through this path; asking for one costs a failed probe at load. */
    cparams.use_gpu = false;

    struct whisper_context *context = whisper_init_from_file_with_params(path, cparams);
    (*env)->ReleaseStringUTFChars(env, model_path, path);

    if (context == NULL) {
        LOGW("model would not load: %s", path);
        return 0;
    }
    return (jlong) context;
}

JNIEXPORT void JNICALL
Java_com_mosman_wird_audio_WhisperNative_freeContext(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    (void) env;
    (void) thiz;
    if (context_ptr != 0) {
        whisper_free((struct whisper_context *) context_ptr);
    }
}

JNIEXPORT jint JNICALL
Java_com_mosman_wird_audio_WhisperNative_fullTranscribe(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint num_threads, jfloatArray audio_data) {
    (void) thiz;
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    jfloat *samples = (*env)->GetFloatArrayElements(env, audio_data, NULL);
    const jsize count = (*env)->GetArrayLength(env, audio_data);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    /* ⚠ The line this file was written for. */
    params.language = WIRD_LANGUAGE;
    params.detect_language = false;

    /* Transcribe, never translate. Sacred Rule 2 territory: the app must not turn someone's
     * recitation into English and present it as what they said. */
    params.translate = false;

    params.n_threads = num_threads;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;

    /* Upstream's demo prints every segment and every timing as it goes. On a phone that is
     * real work done purely so a sample app can look busy in logcat. */
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;

    /*
     * ⚠ THE TWO SETTINGS THAT DECIDE WHETHER THIS FINISHES AT ALL.
     *
     * A 42-second recitation ran for 290 seconds at 386% CPU before this. Not deadlocked -
     * genuinely working, and doing several times the necessary work. Both causes are classic
     * with a FINE-TUNED Whisper model, which is what tarteel-ai/whisper-base-ar-quran is:
     *
     * 1. temperature_inc drives whisper's fallback loop. When a decode fails its entropy or
     *    log-probability thresholds, whisper.cpp retries the same window at a higher
     *    temperature, up to six times. A fine-tune trained on one narrow domain trips those
     *    thresholds constantly, so nearly every window gets decoded six times over. Setting it
     *    to zero says: decode once, take the answer.
     *
     * 2. no_timestamps. Fine-tunes for plain transcription are commonly trained WITHOUT
     *    timestamp tokens. Asking such a model to emit them makes it predict tokens it was
     *    never taught, which is the standard recipe for a repetition loop that runs until the
     *    context is full. Wird wants the words and has never once wanted the timings.
     *
     * ⚠ What this costs: with no fallback, a genuinely bad window returns a poor result instead
     * of being retried. That is the right trade here - the honest answer to "I could not make
     * that out" is to say so, not to spend a minute of someone's battery guessing again.
     */
    params.temperature_inc = 0.0f;
    params.no_timestamps = true;

    /* Reports percentage as it goes. Without this, a slow run and a stuck run look identical
     * from outside, which is exactly the ambiguity that cost two rounds of guessing. */
    params.progress_callback = wird_progress;
    params.progress_callback_user_data = NULL;

    LOGI("whisper_full: %d samples, %d threads, lang=%s", count, num_threads, params.language);
    const int result = whisper_full(context, params, samples, count);
    if (result != 0) {
        LOGW("whisper_full failed: %d", result);
    }

    /* JNI_ABORT: the array was never modified, so there is nothing to copy back. Copying a
     * forty-second recitation back for no reason is a few megabytes of pointless work. */
    (*env)->ReleaseFloatArrayElements(env, audio_data, samples, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_mosman_wird_audio_WhisperNative_getTextSegmentCount(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    (void) env;
    (void) thiz;
    return whisper_full_n_segments((struct whisper_context *) context_ptr);
}

JNIEXPORT jstring JNICALL
Java_com_mosman_wird_audio_WhisperNative_getTextSegment(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    (void) thiz;
    const char *text = whisper_full_get_segment_text(
            (struct whisper_context *) context_ptr, index);
    return (*env)->NewStringUTF(env, text == NULL ? "" : text);
}

JNIEXPORT jstring JNICALL
Java_com_mosman_wird_audio_WhisperNative_getSystemInfo(
        JNIEnv *env, jobject thiz) {
    (void) thiz;
    return (*env)->NewStringUTF(env, whisper_print_system_info());
}
