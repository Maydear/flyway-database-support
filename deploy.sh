#!/usr/bin/env bash
# 上传SNAPSHOT版本到maven仓库
echo "Execute: mvn clean deploy -DskipTests"
mvn clean deploy -DskipTests
echo "Done."