
#关闭SSL，不支持rtmps
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

#将所有源文件放入rtmp_source变量中
file(GLOB rtmp_source *.c)

#编译静态库
add_library( # Sets the name of the library.
             rtmp

             # Sets the library as a shared library.
             STATIC

             # Provides a relative path to your source file(s).
             ${rtmp_source})