cmake_minimum_required(VERSION 3.4.1)

find_library(log-lib
             log )

add_library(network-lib
            SHARED
            network-lib.cpp )

include_directories(include)
