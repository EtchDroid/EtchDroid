cmake_minimum_required(VERSION 3.4)

# Use ccache if available
find_program(CCACHE_FOUND ccache)
if(CCACHE_FOUND)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE ccache)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_LINK ccache)
endif(CCACHE_FOUND)


project(parted_build)
include(ExternalProject)

set(AUTOMAKE_DUCTTAPE_SRC ${PROJECT_BINARY_DIR}/parted_src)

include(../cmake_utils/get_args.cmake)

list(APPEND CMAKE_ARGS "-DCMAKE_HOST_SYSTEM_NAME=${CMAKE_HOST_SYSTEM_NAME}")

set(BOOTSTRAP_ARGS ${CMAKE_ARGS})

list(APPEND CMAKE_ARGS "-DCWD=${AUTOMAKE_DUCTTAPE_SRC}")

set(CONFIG_ARGS ${CMAKE_ARGS})
set(MAKE_ARGS ${CMAKE_ARGS})
set(INSTALL_ARGS ${CMAKE_ARGS})

list(APPEND BOOTSTRAP_ARGS "-DCWD=${CMAKE_CURRENT_SOURCE_DIR}/src/c/parted")
list(APPEND BOOTSTRAP_ARGS "-DCOMMAND=${AUTOMAKE_DUCTTAPE_SRC}/bootstrap")
list(APPEND BOOTSTRAP_ARGS "-P" "${CMAKE_CURRENT_SOURCE_DIR}/../cmake_utils/ndk_exec.cmake")

list(APPEND CONFIG_ARGS "-DCOMMAND:STRING=${AUTOMAKE_DUCTTAPE_SRC}/configure --disable-dependency-tracking --host=<NDK_HOST> --disable-rpath --disable-static --disable-nls --enable-shared --prefix=<INSTALL_DIR> --sbindir=${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/ --bindir=${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/ --libdir=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
list(APPEND CONFIG_ARGS "-P" "${CMAKE_CURRENT_SOURCE_DIR}/../cmake_utils/ndk_exec.cmake")

list(APPEND MAKE_ARGS "-DCOMMAND=make")
list(APPEND MAKE_ARGS "-P" "${CMAKE_CURRENT_SOURCE_DIR}/../cmake_utils/ndk_exec.cmake")

list(APPEND INSTALL_ARGS "-DCOMMAND:STRING=make install")
list(APPEND INSTALL_ARGS "-P" "${CMAKE_CURRENT_SOURCE_DIR}/../cmake_utils/ndk_exec.cmake")


get_cmake_property(_variableNames VARIABLES)
list (SORT _variableNames)
foreach (_variableName ${_variableNames})
    message(STATUS "${_variableName}=${${_variableName}}")
endforeach()

ExternalProject_Add(parted_build
	SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src/c/parted"
	CONFIGURE_COMMAND ${CMAKE_COMMAND} ${CONFIG_ARGS}
	BUILD_COMMAND ${CMAKE_COMMAND} ${MAKE_ARGS}
	BUILD_ALWAYS True
	INSTALL_COMMAND ${CMAKE_COMMAND} ${INSTALL_ARGS})

ExternalProject_Add_Step(parted_build bootstrap
    COMMAND true)
    #${CMAKE_COMMAND} ${BOOTSTRAP_ARGS})

ExternalProject_Add_Step(parted_build rmoldsrc
    DEPENDEES bootstrap
    COMMAND  rm -Rf ${AUTOMAKE_DUCTTAPE_SRC})

ExternalProject_Add_Step(parted_build copysrc
    DEPENDERS configure
    DEPENDEES rmoldsrc
    COMMAND  cp -R ${CMAKE_CURRENT_SOURCE_DIR}/src/c/parted ${AUTOMAKE_DUCTTAPE_SRC})


add_library(libparted-fs-resize SHARED IMPORTED)
add_dependencies(libparted-fs-resize parted_build)
set_target_properties(libparted-fs-resize PROPERTIES IMPORTED_LOCATION ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libparted-fs-resize.so.0.0.1)


add_library(libparted SHARED IMPORTED)
add_dependencies(libparted parted_build)
set_target_properties(libparted PROPERTIES IMPORTED_LOCATION ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libparted.so.2.0.1)

add_executable(parted IMPORTED)
add_dependencies(parted parted_build)
set_target_properties(parted PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/parted)

add_executable(partprobe IMPORTED)
add_dependencies(partprobe parted_build)
set_target_properties(partprobe PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/partprobe)

add_executable(dummy
    ${CMAKE_CURRENT_SOURCE_DIR}/src/c/dummy.c)
add_dependencies(dummy parted_build)