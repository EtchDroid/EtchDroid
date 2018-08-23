cmake_minimum_required(VERSION 3.4)

set(ASSETS_PATH "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/termux/${ANDROID_ABI}")

add_subdirectory(src/c/termux-wrapper)

# Dummy target for Gradle to pick up termux_wrapper
add_executable(termux_packages
    ${CMAKE_CURRENT_SOURCE_DIR}/src/c/dummy.c)
add_dependencies(termux_packages termux_build)