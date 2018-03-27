# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# this is the directory, from which cmake was started, i.e. the top level source directory
MESSAGE( STATUS "CMAKE_SOURCE_DIR:         " ${CMAKE_SOURCE_DIR} )

# this is the directory where the currently processed CMakeLists.txt is located in
MESSAGE( STATUS "CMAKE_CURRENT_SOURCE_DIR: " ${CMAKE_CURRENT_SOURCE_DIR} )

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
             calibration_upload_native

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/calib_camera.cpp src/main/cpp/fileUploader.cpp)

#Specify the header location
include_directories(src/main/cpp/ ${CMAKE_CURRENT_SOURCE_DIR}/../../artoolkitX-dependency/include/ ${CMAKE_CURRENT_SOURCE_DIR}/../../curl-dependency/include/ )

##Find Android NDK APIs
#TBB
find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              tbb-lib

              # Specifies the name of the NDK library that
              # CMake needs to locate.
              tbb )
#Z
find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              z-lib

              # Specifies the name of the NDK library that
              # CMake needs to locate.
              z )

#GLES
find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              GLESv1_CM-lib

              # Specifies the name of the NDK library that
              # CMake needs to locate.
              GLESv1_CM )

#LOG
find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              log-lib

              # Specifies the name of the NDK library that
              # CMake needs to locate.
              log )

add_library( ARX
             SHARED
             IMPORTED )

add_library( curl
          STATIC
          IMPORTED )


set_target_properties( # Specifies the target library.
                     curl

                     # Specifies the parameter you want to define.
                     PROPERTIES IMPORTED_LOCATION

                     # Provides the path to the library you want to import.
                     ${CMAKE_CURRENT_SOURCE_DIR}/../../curl-dependency/${ANDROID_ABI}/libcurl.a )

set_target_properties( # Specifies the target library.
                  ARX

                  # Specifies the parameter you want to define.
                  PROPERTIES IMPORTED_LOCATION

                  # Provides the path to the library you want to import.
                  ${CMAKE_CURRENT_SOURCE_DIR}/../../artoolkitX-dependency/${ANDROID_ABI}/libARX.so )

# Links your native library against one or more other native libraries.
target_link_libraries( # Specifies the target library.
                     calibration_upload_native

                     # Links the libraries to the target library.
                     curl ${log-lib} ${GLESv1-CM} z ${tbb})

target_link_libraries( # Specifies the target library.
                  calibration_upload_native

                  # Links the libraries to the target library.
                  ARX )
