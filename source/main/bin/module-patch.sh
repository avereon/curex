#!/bin/bash

WORK_FOLDER="/tmp/modpatch"
BASE_MODULE_PATH="target/dependency"
MODULE_PATH="$BASE_MODULE_PATH"

patch() {
	JAR_NAME="$(basename "$1")"
	JAR_PATH="$(dirname "$1")"

	MOD_NAME=$(echo "${JAR_NAME%.*}" | sed 's/-/./')

	echo "ModuleJar path: $JAR_PATH"
	echo "ModuleJar name: $JAR_NAME"
	echo "Module name: $MOD_NAME"

	# Need to move module to fix out of the module path
	mkdir -p "$WORK_FOLDER"
	mv "$1" "$WORK_FOLDER"

	$JAVA_HOME/bin/jdeps --module-path "$MODULE_PATH" --generate-module-info "$WORK_FOLDER" "$WORK_FOLDER/$JAR_NAME"
	$JAVA_HOME/bin/javac -p "$MODULE_PATH" --patch-module $MOD_NAME="$WORK_FOLDER/$JAR_NAME" "$WORK_FOLDER/$MOD_NAME/module-info.java"
	$JAVA_HOME/bin/jar uf "$WORK_FOLDER/$JAR_NAME" -C "$WORK_FOLDER/$MOD_NAME" module-info.class

	mv "$WORK_FOLDER/$JAR_NAME" "$JAR_PATH"
	rm -rf "$WORK_FOLDER"
}

#echo "OS name:  $OS"
#echo "Module info folder: $MODULE_INFO_FOLDER"

for JAR in "$@"
do
	patch "$JAR"
done

