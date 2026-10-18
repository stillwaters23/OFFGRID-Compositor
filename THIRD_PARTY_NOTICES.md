# Third-party notices

Copyright © 2026 stillwaters23. OFFGRID Compositor is proprietary. See `LICENSE`.

This file lists every third-party dependency declared in the Gradle files, with the license named in that dependency's published metadata. The version is the version written in the Gradle file. Transitive dependencies are not expanded.

`project(":live")` in `04 Build/app/build.gradle.kts` is part of OFFGRID. It is a first-party module.

## Gradle plugins

Declared in `04 Build/build.gradle.kts`.

| Plugin id | Version | Published coordinate | License |
| --- | --- | --- | --- |
| `com.android.application` | 8.13.2 | `com.android.tools.build:gradle:8.13.2` | Apache-2.0 |
| `com.android.library` | 8.13.2 | `com.android.tools.build:gradle:8.13.2` | Apache-2.0 |
| `org.jetbrains.kotlin.android` | 2.2.21 | `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21` | Apache-2.0 |

The Android Gradle Plugin POM names the license "The Apache Software License, Version 2.0". The Kotlin Gradle plugin POM names it "Apache-2.0". Both point at <http://www.apache.org/licenses/LICENSE-2.0.txt>.

## Libraries

| Declared in | Configuration | Coordinate | License |
| --- | --- | --- | --- |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.core:core-ktx:1.16.0` | Apache-2.0 |
| `04 Build/live/build.gradle.kts` | implementation | `androidx.core:core-ktx:1.16.0` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.appcompat:appcompat:1.7.1` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.activity:activity-ktx:1.10.1` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.camera:camera-core:1.6.2` | Apache-2.0 and BSD-3-Clause |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.camera:camera-camera2:1.6.2` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.camera:camera-lifecycle:1.6.2` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.camera:camera-video:1.6.2` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.camera:camera-view:1.6.2` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.media3:media3-exoplayer:1.11.1` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.media3:media3-common:1.11.1` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `androidx.media3:media3-datasource:1.11.1` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | implementation | `com.google.mediapipe:tasks-vision:0.10.35` | Apache-2.0 |
| `04 Build/app/build.gradle.kts` | testImplementation | `junit:junit:4.13.2` | EPL-1.0 |
| `04 Build/live/build.gradle.kts` | testImplementation | `junit:junit:4.13.2` | EPL-1.0 |
| `04 Build/app/build.gradle.kts` | testImplementation | `org.robolectric:robolectric:4.17` | MIT |
| `04 Build/live/build.gradle.kts` | testImplementation | `org.robolectric:robolectric:4.17` | MIT |

License names below are the names in each artifact's POM.

- Apache-2.0, "The Apache Software License, Version 2.0": <http://www.apache.org/licenses/LICENSE-2.0.txt>
- BSD-3-Clause: <https://opensource.org/licenses/BSD-3-Clause>
- EPL-1.0, "Eclipse Public License 1.0": <http://www.eclipse.org/legal/epl-v10.html>
- MIT, "The MIT License": <https://opensource.org/licenses/MIT>

`androidx.camera:camera-core:1.6.2` publishes Apache-2.0 and BSD-3-Clause. Its POM says the library includes libyuv, which is under the BSD-3-Clause license. The other CameraX artifacts at 1.6.2 publish Apache-2.0 only.

JUnit 4.13.2 is Eclipse Public License 1.0. Robolectric 4.17 is the MIT License.

## Gradle distribution

`04 Build/gradle/wrapper/gradle-wrapper.properties` pins Gradle 8.14.5 (`https://services.gradle.org/distributions/gradle-8.14.5-bin.zip`). The Gradle Build Tool is Apache-2.0: <https://github.com/gradle/gradle/blob/v8.14.5/LICENSE>.

## Bundled third-party assets

These files are already in the tree. They are not Gradle coordinates.

### JetBrains Mono

- Copyright 2020 The JetBrains Mono Project Authors (<https://github.com/JetBrains/JetBrainsMono>)
- SIL Open Font License, Version 1.1
- `04 Build/app/src/main/res/font/jetbrains_mono_regular.ttf`
- `04 Build/app/src/main/res/font/jetbrains_mono_bold.ttf`
- `04 Build/app/src/main/res/font/jetbrains_mono.xml`
- Full license text already in the app: `04 Build/app/src/main/assets/JetBrains Mono License.txt`
- Also at <https://openfontlicense.org>

The font is bundled under its reserved name, JetBrains Mono.

### MediaPipe selfie segmenter model

- `04 Build/app/src/main/assets/selfie_segmenter.tflite` (249,537 bytes)
- The file identifies itself as the MediaPipe selfie-segmentation general model `selfiesegmentation_mlkit-256x256-2021_01_19-v1215`
- Used with `com.google.mediapipe:tasks-vision`
- Apache-2.0, as stated on the model card "Model Card MediaPipe Selfie Segmentation" (6 May 2021): <https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Selfie%20Segmentation.pdf>

## Named in the voice review, absent from the Gradle files

The voice review's inventory names MicUp (MIT), Oboe (Apache-2.0), sherpa-onnx (Apache-2.0), and the Shizuku API (MIT). None of those coordinates appear in a Gradle file. This change does not add them. A later phase that declares one adds its notice in the same change.
