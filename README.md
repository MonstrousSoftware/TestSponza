# TestSponza

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).


Performance test of GLTF Sponza model (model and batteries not included).

Get model from here  https://github.com/KhronosGroup/glTF-Sample-Models/tree/main/2.0/Sponza
and place under assets

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Performance Testing

NVIDIA Nsight Graphics: download from here: https://developer.nvidia.com/nsight-graphics

Create a runnable jar file with gradle: Task :lwjgl3:jar

In Nvidia Nsight:

Command line with args: "path\to\java" -jar TestSponza-1.0.0.jar
    e.g. "C:\Program Files\BellSoft\LibericaJDK-17\bin\java.exe" -jar TestSponza-1.0.0.jar

working directory: F:\Coding\IdeaProjects\TestSponza\lwjgl3\build\libs


