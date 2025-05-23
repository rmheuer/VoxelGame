# VoxelGame

Clone of Minecraft Classic version c0.30_c

## Controls

- WASD to move
- Space to jump
- Move mouse to turn camera
- Left click to break block
- Right click to place block
- B or E to open block picker
- Esc to pause game

## Building

Requires Azalea and NBTLib, found at https://github.com/rmheuer/azalea and
https://github.com/rmheuer/NBTLib.

They are both not currently in any Maven repositories, so they must be installed
manually using `mvn install`.

Then VoxelGame can be built using `mvn package`. The resulting JAR will be
found at `target/VoxelGame-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Running

On Windows or Linux, you can simply run the JAR file with
`java -jar target/VoxelGame-1.0-SNAPSHOT-jar-with-dependencies.jar`.

On macOS, the JVM flag `-XstartOnFirstThread` is required:
`java -XstartOnFirstThread -jar target/VoxelGame-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## References

Lava and water animations: https://github.com/ClassiCube/ClassiCube/wiki/Minecraft-Classic-lava-animation-algorithm

Voxel raycast algorithm: http://www.cse.yorku.ca/~amana/research/grid.pdf

Player physics: https://www.mcpk.wiki/wiki/Main_Page

Occlusion culling: https://tomcc.github.io/2014/08/31/visibility-1.html

Multiplayer protocol: https://web.archive.org/web/20240516115242/https://wiki.vg/Classic_Protocol

Protocol extensions: https://web.archive.org/web/20240516115200/https://wiki.vg/Classic_Protocol_Extension

Viewing OpenGL calls by the original game using apitrace: https://apitrace.github.io/
