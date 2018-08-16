cmake_minimum_required(VERSION 3.4)

set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}")

execute_process(
    COMMAND truncate -s 0 crypto/compat/getpagesize.c
    WORKING_DIRECTORY src/c/libressl)

execute_process(
    COMMAND ./autogen.sh
    WORKING_DIRECTORY src/c/libressl)

include_directories(src/c/bzip2)
include_directories(src/c/libressl/include)

add_subdirectory(src/c/bzip2)
add_subdirectory(src/c/libressl)
add_subdirectory(src/c/dmg2img)