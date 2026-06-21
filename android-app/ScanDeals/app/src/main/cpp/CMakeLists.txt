cmake_minimum_required(VERSION 3.22.1)

project(scandeals)

# 指向 MNN 编译产物 .so
set(MNN_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library(MNN SHARED IMPORTED)
set_target_properties(MNN PROPERTIES IMPORTED_LOCATION
    "${MNN_LIB_DIR}/libMNN.so")

add_library(scandeals SHARED qwen_bridge.cpp)

target_compile_options(scandeals PRIVATE
    -std=c++17
    -fno-rtti
    -O2)

target_include_directories(scandeals PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llm)

target_link_libraries(scandeals
    MNN
    android
    log
    jnigraphics)

# 链接 .so 到 APK
add_custom_command(TARGET scandeals POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:scandeals>
            ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libscandeals.so
    COMMENT "Copying libscandeals.so to jniLibs")