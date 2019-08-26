cmake_minimum_required(VERSION 3.4)

set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}")

find_program(CCACHE_FOUND ccache)
if (CCACHE_FOUND)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE ccache)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_LINK ccache)
endif (CCACHE_FOUND)

execute_process(
        COMMAND truncate -s 0 crypto/compat/getpagesize.c
        WORKING_DIRECTORY src/c/libressl)

execute_process(
        COMMAND touch VERSION
        WORKING_DIRECTORY src/c/libressl)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Qunused-arguments")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Qunused-arguments")

include_directories(src/c/bzip2)
include_directories(src/c/libressl/include)

add_subdirectory(src/c/bzip2)
add_subdirectory(src/c/libressl)
add_subdirectory(src/c/dmg2img)
