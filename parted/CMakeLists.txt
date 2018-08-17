cmake_minimum_required(VERSION 3.4)

find_program(CCACHE_FOUND ccache)
if(CCACHE_FOUND)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE ccache)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_LINK ccache)
endif(CCACHE_FOUND)

include(ExternalProject)

ExternalProject_Add(parted_build
	SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src/c/parted"
	CONFIGURE_COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/src/c/parted/configure --prefix=<INSTALL_DIR> --sbindir=${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/ --bindir=${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/ --libdir=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
	BUILD_COMMAND ${MAKE})

ExternalProject_Add_Step(parted_build bootstrap
    DEPENDERS configure
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/src/c/parted
    COMMAND ./bootstrap)

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

#ExternalProject_Add_Step(parted mkbindir
#    DEPENDEES build
#    COMMAND mkdir -p "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/")

#ExternalProject_Add_Step(parted copybin
#    DEPENDEES mkbindir
#    COMMAND cp <INSTALL_DIR>/sbin/parted <INSTALL_DIR>/sbin/partprobe "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/bin/${ANDROID_ABI}/")
