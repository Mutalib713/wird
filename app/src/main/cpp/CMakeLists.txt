# Wird's native build: on-device recitation checking. PLAN task 14.
#
# **Nothing here is vendored and nothing is forked.** whisper.cpp lives in the repo as a git
# submodule pinned to v1.9.2, and both files compiled below are read straight out of it:
#
#   * `jni.c` is compiled straight out of the submodule. Their bridge already exposes exactly
#     what a transcription needs - init, transcribe, read the segments back, free - so writing
#     our own would be copying working C to change nothing but the file it lives in.
#   * The Kotlin class that binds to it, `com.whispercpp.whisper.WhisperLib`, is named to match
#     THEIR symbols rather than our package convention. JNI links by mangled name, so the
#     alternative was editing every function name in their file and re-editing it on every
#     upgrade. See PROFILE.md § 5aw.
#
# Adapted from examples/whisper.android/lib/src/main/jni/whisper/CMakeLists.txt. Two deliberate
# differences from theirs, both noted where they occur below.

cmake_minimum_required(VERSION 3.22)

project(wird_whisper)

set(CMAKE_CXX_STANDARD 17)

# cpp -> main -> src -> app -> repo root
set(WHISPER_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/whisper.cpp)

if (NOT EXISTS ${WHISPER_LIB_DIR}/src/whisper.cpp)
    # A submodule that was never initialised produces a wall of missing-header errors that say
    # nothing about the cause. Fail here instead, with the command that fixes it.
    message(FATAL_ERROR
        "whisper.cpp is missing. The submodule has not been checked out.\n"
        "Run: git submodule update --init --recursive")
endif ()

file(READ "${WHISPER_LIB_DIR}/CMakeLists.txt" MAIN_CMAKE_CONTENT)
string(REGEX MATCH "project\\(\"whisper\\.cpp\" VERSION ([0-9]+\\.[0-9]+\\.[0-9]+)\\)"
        VERSION_MATCH "${MAIN_CMAKE_CONTENT}")
if (CMAKE_MATCH_1)
    set(WHISPER_VERSION ${CMAKE_MATCH_1})
else ()
    set(WHISPER_VERSION "unknown")
endif ()
message(STATUS "Wird is building whisper.cpp ${WHISPER_VERSION}")

set(SOURCE_FILES
        ${WHISPER_LIB_DIR}/src/whisper.cpp
        ${WHISPER_LIB_DIR}/examples/whisper.android/lib/src/main/jni/whisper/jni.c
)

find_library(LOG_LIB log)
include(FetchContent)

# ⚠ **One library, not three.** Upstream builds `whisper`, `whisper_v8fp16_va` and
# `whisper_vfpv4` so its demo can pick the best at runtime for the CPU it landed on. Wird ships
# arm64-v8a only, and every arm64 Android device supports the fp16 vector instructions, so the
# other two would be dead weight in an APK that § 10 measures byte by byte.
add_library(whisper SHARED ${SOURCE_FILES})

target_compile_definitions(whisper PUBLIC GGML_USE_CPU)
target_compile_definitions(whisper PRIVATE WHISPER_VERSION="${WHISPER_VERSION}")
# ⚠ **The fp16 flag is ARM-only and must be guarded.** Debug builds also produce x86_64 so the
# emulator can run this, and clang rejects `-march=armv8.2-a+fp16` there outright:
# `error: unknown target CPU`. Upstream never hits this because its own CMake only ever builds
# the two ARM ABIs.
if (ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(whisper PRIVATE -march=armv8.2-a+fp16)
    set(GGML_COMPILE_OPTIONS -march=armv8.2-a+fp16)
else ()
    set(GGML_COMPILE_OPTIONS "")
endif ()

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(whisper PRIVATE -O3)
    target_compile_options(whisper PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
    target_compile_options(whisper PRIVATE -ffunction-sections -fdata-sections)
    target_link_options(whisper PRIVATE -Wl,--gc-sections)
    target_link_options(whisper PRIVATE -Wl,--exclude-libs,ALL)
    target_link_options(whisper PRIVATE -flto)
endif ()

FetchContent_Declare(ggml SOURCE_DIR ${WHISPER_LIB_DIR}/ggml)
FetchContent_MakeAvailable(ggml)
target_compile_options(ggml PRIVATE ${GGML_COMPILE_OPTIONS})
target_link_libraries(whisper ${LOG_LIB} android ggml)

target_include_directories(whisper PRIVATE
        ${WHISPER_LIB_DIR}
        ${WHISPER_LIB_DIR}/src
        ${WHISPER_LIB_DIR}/include
        ${WHISPER_LIB_DIR}/ggml/include
        ${WHISPER_LIB_DIR}/ggml/src
        ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu
)
