#!/bin/sh

if [ $# -ge 1 ]; then
    SUFFIX="--test-only *$1"
else
    SUFFIX=''
fi
if scala test . $SUFFIX --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning --suppress-outdated-dependency-warning; then
    echo "[32mDone.[0m"
else
    echo "[41m[37mTests failed, check the log for the details.[0m"
fi
