# Flatbuffers Dockerfile
# https://github.com/neomantra/docker-flatbuffers

###############################################################################
# FlatBuffer Build
###############################################################################

ARG FLATBUFFERS_IMAGE_BASE="debian"
ARG FLATBUFFERS_IMAGE_TAG="stretch-slim"

FROM ${FLATBUFFERS_IMAGE_BASE}:${FLATBUFFERS_IMAGE_TAG} as flatbuffer_build

ARG FLATBUFFERS_ARCHIVE_BASE_URL="https://github.com/google/flatbuffers/archive"
ARG FLATBUFFERS_ARCHIVE_TAG="v1.12.0"
ARG FLATBUFFERS_BUILD_TYPE="Release"

# Set to exactly "true" to use clang
ARG FLATBUFFERS_USE_CLANG="false"


RUN DEBIAN_FRONTEND=noninteractive apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates \
        cmake \
        curl \
        make \
        $( if [ "${FLATBUFFERS_USE_CLANG}" = "true" ] ; then echo "clang" ; else echo "g++" ; fi)


RUN curl -fSL "${FLATBUFFERS_ARCHIVE_BASE_URL}/${FLATBUFFERS_ARCHIVE_TAG}.tar.gz" -o flatbuffers.tar.gz \
    && tar xzf flatbuffers.tar.gz \
    && cd flatbuffers-* \
    && env $( if [ "${FLATBUFFERS_USE_CLANG}" = "true" ] ; then echo "CC=/usr/bin/clang CXX=/usr/bin/clang++ " ; fi) \
           cmake -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=${FLATBUFFERS_BUILD_TYPE} \
    && make \
    && make test \
    && make install \
    && cp src/idl_parser.cpp src/idl_gen_text.cpp /usr/local/include/flatbuffers


# Build artifacts:
# -- Install configuration: "Release"
# -- Installing: /usr/local/include/flatbuffers
# -- Installing: /usr/local/include/flatbuffers/idl.h
# -- Installing: /usr/local/include/flatbuffers/registry.h
# -- Installing: /usr/local/include/flatbuffers/reflection.h
# -- Installing: /usr/local/include/flatbuffers/flexbuffers.h
# -- Installing: /usr/local/include/flatbuffers/flatc.h
# -- Installing: /usr/local/include/flatbuffers/minireflect.h
# -- Installing: /usr/local/include/flatbuffers/base.h
# -- Installing: /usr/local/include/flatbuffers/grpc.h
# -- Installing: /usr/local/include/flatbuffers/flatbuffers.h
# -- Installing: /usr/local/include/flatbuffers/reflection_generated.h
# -- Installing: /usr/local/include/flatbuffers/hash.h
# -- Installing: /usr/local/include/flatbuffers/stl_emulation.h
# -- Installing: /usr/local/include/flatbuffers/util.h
# -- Installing: /usr/local/include/flatbuffers/code_generators.h
# -- Installing: /usr/local/lib/cmake/flatbuffers/FlatbuffersConfig.cmake
# -- Installing: /usr/local/lib/cmake/flatbuffers/FlatbuffersConfigVersion.cmake
# -- Installing: /usr/local/lib/libflatbuffers.a
# -- Installing: /usr/local/lib/cmake/flatbuffers/FlatbuffersTargets.cmake
# -- Installing: /usr/local/lib/cmake/flatbuffers/FlatbuffersTargets-release.cmake
# -- Installing: /usr/local/bin/flatc
# -- Installing: /usr/local/lib/cmake/flatbuffers/FlatcTargets.cmake
# -- Installing: /usr/local/lib/cmake/flatbuffers/FlatcTargets-release.cmake
#
# Also want:
# src/idl_parser.cpp
# src/idl_gen_text.cpp
#


###############################################################################
# flatcc Build
###############################################################################

# Same build environment as FlatBuffer

ARG FLATCC_ARCHIVE_BASE_URL="https://github.com/dvidelabs/flatcc/archive/"
ARG FLATCC_ARCHIVE_TAG="master"

RUN curl -fSL "${FLATCC_ARCHIVE_BASE_URL}/${FLATCC_ARCHIVE_TAG}.tar.gz" -o flatcc.tar.gz \
    && tar xzf flatcc.tar.gz \
    && mv flatcc-* flatcc \
    && cd flatcc \
    && env $( if [ "${FLATBUFFERS_USE_CLANG}" = "true" ] ; then echo "CC=/usr/bin/clang CXX=/usr/bin/clang++ " ; fi) \
        ./scripts/initbuild.sh make \
    && env $( if [ "${FLATBUFFERS_USE_CLANG}" = "true" ] ; then echo "CC=/usr/bin/clang CXX=/usr/bin/clang++ " ; fi) \
       ./scripts/build.sh


# Build artifacts:
# Compiler:
#   bin/flatcc                 (command line interface to schema compiler)
#   lib/libflatcc.a            (optional, for linking with schema compiler)
#   include/flatcc/flatcc.h    (optional, header and doc for libflatcc.a)
# Runtime:
#   include/flatcc/**          (runtime header files)
#   include/flatcc/reflection  (optional)
#   include/flatcc/support     (optional, only used for test and samples)
#   lib/libflatccrt.a          (runtime library)


###############################################################################
# Final Image Composition
###############################################################################

FROM ${FLATBUFFERS_IMAGE_BASE}:${FLATBUFFERS_IMAGE_TAG}

COPY --from=flatbuffer_build /usr/local/bin/flatc /usr/local/bin/flatc
COPY --from=flatbuffer_build /usr/local/include/flatbuffers /usr/local/include/flatbuffers
COPY --from=flatbuffer_build /usr/local/lib/libflatbuffers.a /usr/local/lib/libflatbuffers.a
COPY --from=flatbuffer_build /usr/local/lib/cmake/flatbuffers /usr/local/lib/cmake/flatbuffers

COPY --from=flatbuffer_build /flatcc/bin/flatcc /usr/local/bin/flatcc
COPY --from=flatbuffer_build /flatcc/include/flatcc /usr/local/include/flatcc
COPY --from=flatbuffer_build /flatcc/lib/*.a /usr/local/lib/

ARG FLATBUFFERS_USE_CLANG="false"
ARG FLATBUFFERS_ARCHIVE_BASE_URL="https://github.com/google/flatbuffers/archive"
ARG FLATBUFFERS_ARCHIVE_TAG="master"
ARG FLATBUFFERS_BUILD_TYPE="Release"
ARG FLATCC_ARCHIVE_BASE_URL="https://github.com/dvidelabs/flatcc/archive/"
ARG FLATCC_ARCHIVE_TAG="master"

LABEL maintainer="Evan Wies <evan@neomantra.net>"
LABEL FLATBUFFERS_IMAGE_BASE="${FLATBUFFERS_IMAGE_BASE}"
LABEL FLATBUFFERS_IMAGE_TAG="${FLATBUFFERS_IMAGE_TAG}"
LABEL FLATBUFFERS_USE_CLANG="${FLATBUFFERS_USE_CLANG}"
LABEL FLATBUFFERS_ARCHIVE_BASE_URL="${FLATBUFFERS_ARCHIVE_BASE_URL}"
LABEL FLATBUFFERS_ARCHIVE_TAG="${FLATBUFFERS_ARCHIVE_TAG}"
LABEL FLATBUFFERS_BUILD_TYPE="${FLATBUFFERS_BUILD_TYPE}"
LABEL FLATCC_ARCHIVE_BASE_URL="${FLATCC_ARCHIVE_BASE_URL}"
LABEL FLATCC_ARCHIVE_TAG="${FLATCC_ARCHIVE_TAG}"
