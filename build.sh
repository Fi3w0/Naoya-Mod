#!/bin/bash

# Naoya Mod Build Script
# This script builds the Naoya Mod for Minecraft 1.20.1

echo "Building Naoya Mod v1.2.1..."

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build the mod
echo "Building mod..."
./gradlew build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "JAR file location: build/libs/naoya-mod-1.2.1.jar"
    
    # Display file size
    if [ -f "build/libs/naoya-mod-1.2.1.jar" ]; then
        filesize=$(du -h "build/libs/naoya-mod-1.2.1.jar" | cut -f1)
        echo "File size: $filesize"
    fi
else
    echo "Build failed!"
    exit 1
fi