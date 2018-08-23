cmake_minimum_required(VERSION 3.4)

# NDK is assumed to be placed inside SDK directory
# Should be implemented better
get_filename_component(ANDROID_SDK ${ANDROID_NDK} DIRECTORY)

if(${ANDROID_ABI} STREQUAL "x86")
    set(TERMUX_ARCH "i686")
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(TERMUX_ARCH "aarch64")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(TERMUX_ARCH "arm")
else()
    set(TERMUX_ARCH "${ANDROID_ABI}")
endif()

set(TERMUX_TOPDIR "${PROJECT_BINARY_DIR}/termux-topdir")
set(TERMUX_DEBDIR "${Project_BINARY_DIR}/termux-debs")
set(TERMUX_PREFIX "/data/data/${APP_PKGNAME}/files/termux/usr")
set(TERMUX_ANDROID_HOME "/data/data/${APP_PKGNAME}/files/termux/home")

add_custom_target(termux_build
    ${CMAKE_COMMAND} -E env
        ASSETS_PATH=${ASSETS_PATH}
        NDK=${ANDROID_NDK}
        ANDROID_HOME=${ANDROID_SDK}
        TERMUX_TOPDIR=${TERMUX_TOPDIR}
        TERMUX_DEBDIR=${TERMUX_DEBDIR}
        TERMUX_PREFIX=${TERMUX_PREFIX}
        TERMUX_ANDROID_HOME=${TERMUX_ANDROID_HOME}
        TERMUX_ARCH=${TERMUX_ARCH}
        ${CMAKE_CURRENT_LIST_DIR}/build-packages.sh ${TERMUX_PACKAGES}
    WORKING_DIRECTORY ${CMAKE_CURRENT_LIST_DIR})
