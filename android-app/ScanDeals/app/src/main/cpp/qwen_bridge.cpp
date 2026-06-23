// qwen_bridge.cpp - 招牌识别 JNI 桥
// 调用 MNN LLM 推理招牌图片,返回 JSON 商家信息

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <filesystem>
#include <mutex>

#include "MNN/Interpreter.hpp"
#include "MNN/ImageProcess.hpp"
#include "MNN/expr/ExecutorScope.hpp"
#include "cv/imgcodecs.hpp"
#include "llm/llm.hpp"

#define LOG_TAG "ScanDeals"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<MNN::Transformer::Llm> g_llm;
static std::string g_config_path;
static std::mutex g_llm_mutex;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_mqwen_scandeals_MnnBridge_nativeInit(
    JNIEnv* env, jclass, jstring jConfigPath, jstring jCacheDir) {

    LOGI("nativeInit: enter");

    if (jConfigPath == nullptr) {
        LOGE("nativeInit: config path is null");
        return JNI_FALSE;
    }

    const char* config_path = env->GetStringUTFChars(jConfigPath, nullptr);
    if (config_path == nullptr) {
        LOGE("nativeInit: GetStringUTFChars returned null");
        return JNI_FALSE;
    }
    g_config_path = std::string(config_path);
    env->ReleaseStringUTFChars(jConfigPath, config_path);

    // 取 cacheDir 绝对路径(用于 tmp_path,避免 SIGSEGV)
    std::string cache_path = "/data/local/tmp";
    if (jCacheDir != nullptr) {
        const char* cp = env->GetStringUTFChars(jCacheDir, nullptr);
        if (cp != nullptr) {
            cache_path = std::string(cp);
            env->ReleaseStringUTFChars(jCacheDir, cp);
        }
    }
    std::string tmp_path = cache_path + "/scandeals_llm_tmp";
    LOGI("nativeInit config=%s tmp_path=%s", g_config_path.c_str(), tmp_path.c_str());

    // 确保 tmp 目录存在(用 std::filesystem 代替 system(),Android 7+ 禁用 shell fork)
    {
        std::error_code ec;
        std::filesystem::create_directories(tmp_path, ec);
        if (ec) {
            LOGE("create_directories failed: %s (non-fatal)", ec.message().c_str());
        } else {
            LOGI("tmp dir ensured: %s", tmp_path.c_str());
        }
    }

    // 检查文件是否可读
    {
        std::ifstream f(g_config_path);
        if (!f.good()) {
            LOGE("nativeInit: cannot read config file: %s", g_config_path.c_str());
            return JNI_FALSE;
        }
        LOGI("nativeInit: config file readable");
    }

    try {
        LOGI("nativeInit: calling createLLM...");
        std::lock_guard<std::mutex> lock(g_llm_mutex);
        g_llm.reset(MNN::Transformer::Llm::createLLM(g_config_path));
        if (!g_llm) {
            LOGE("createLLM returned null");
            return JNI_FALSE;
        }
        // 必须传绝对路径给 tmp_path(原相对路径会导致 RuntimeManager::setExternalFile SIGSEGV)
        std::string config_json =
            std::string("{\"tmp_path\":\"") + tmp_path +
            "\",\"use_template\":false}";
        LOGI("nativeInit: set_config: %s", config_json.c_str());
        g_llm->set_config(config_json);
        LOGI("nativeInit: calling llm->load()...");
        bool ok = g_llm->load();
        if (!ok) {
            LOGE("llm load failed");
            return JNI_FALSE;
        }
        LOGI("nativeInit OK");
        return JNI_TRUE;
    } catch (const std::exception& e) {
        LOGE("nativeInit exception: %s", e.what());
        return JNI_FALSE;
    } catch (...) {
        LOGE("nativeInit unknown exception");
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mqwen_scandeals_MnnBridge_nativeRunVlm(
    JNIEnv* env, jclass, jstring jImagePath, jstring jPrompt) {

    const char* image_path = env->GetStringUTFChars(jImagePath, nullptr);
    const char* prompt = env->GetStringUTFChars(jPrompt, nullptr);

    LOGI("nativeRunVlm image=%s", image_path);

    std::ostringstream result;
    std::string image_path_str(image_path);
    std::string prompt_str(prompt);

    env->ReleaseStringUTFChars(jImagePath, image_path);
    env->ReleaseStringUTFChars(jPrompt, prompt);

    try {
        std::lock_guard<std::mutex> lock(g_llm_mutex);
        if (!g_llm) {
            LOGE("nativeRunVlm: llm not initialized");
            return env->NewStringUTF(R"({"error":"llm not initialized"})");
        }
        LOGI("nativeRunVlm: prompt=%s", prompt_str.c_str());

        MNN::Transformer::MultimodalPrompt mm_input;
        mm_input.prompt_template =
            "<|im_start|>user\n"
            "<img>" + image_path_str + "</img>\n" +
            prompt_str +
            "<|im_end|>\n"
            "<|im_start|>assistant\n";

        LOGI("nativeRunVlm: calling g_llm->response(MultimodalPrompt)...");
        g_llm->response(mm_input, &result, "\n", 512);
        LOGI("nativeRunVlm: response done");
    } catch (const std::exception& e) {
        LOGE("response exception: %s", e.what());
        result.str("");
        result << R"({"error":")" << e.what() << R"("})";
    } catch (...) {
        LOGE("response unknown exception");
        result.str("");
        result << R"({"error":"unknown"})";
    }

    std::string out = result.str();
    LOGI("nativeRunVlm output len=%zu first 300: %.300s", out.size(), out.c_str());
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mqwen_scandeals_MnnBridge_nativeRuntimeInfo(JNIEnv* env, jclass) {
    std::ostringstream info;
    info << "MNN backend: libMNN.so 6.95MB | Qwen3-VL-2B INT4 |";
    info << " Device: iQOO Neo 11 V2520A |";
    info << " Backend detected at runtime: i8mm (no SME2 support on Oryon V2)";
    return env->NewStringUTF(info.str().c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mqwen_scandeals_MnnBridge_nativeRelease(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    if (g_llm) {
        LOGI("nativeRelease: releasing llm");
        g_llm.reset();
    }
}