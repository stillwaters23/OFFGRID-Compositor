package com.offgrid.compositor.terminal

internal data class EffectLink(val title: String, val note: String, val url: String)
internal data class EffectLinkCategory(val title: String, val links: List<EffectLink>)

internal object EffectLinks {
    val categories = listOf(
        EffectLinkCategory("Segmentation models", listOf(
            EffectLink("MediaPipe Image Segmenter", "Guide", "https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter"),
            EffectLink("MediaPipe GitHub", "Source code", "https://github.com/google-ai-edge/mediapipe"),
        )),
        EffectLinkCategory("Shaders and overlays", listOf(
            EffectLink("Shadertoy", "Shader gallery", "https://www.shadertoy.com"),
            EffectLink("The Book of Shaders", "Shader lessons", "https://thebookofshaders.com"),
            EffectLink("GPUImage for Android", "Android GPU effects", "https://github.com/cats-oss/android-gpuimage"),
        )),
        EffectLinkCategory("Sound packs", listOf(
            EffectLink("Freesound", "Sound library", "https://freesound.org"),
            EffectLink("Pixabay sound effects", "Sound library", "https://pixabay.com/sound-effects/"),
        )),
        EffectLinkCategory("Animations and emojis", listOf(
            EffectLink("LottieFiles", "Animation library", "https://lottiefiles.com"),
            EffectLink("Google Noto Emoji animations", "Emoji animations", "https://googlefonts.github.io/noto-emoji-animation/"),
        )),
        EffectLinkCategory("Community repos", listOf(
            EffectLink("Android camera effects", "GitHub topic", "https://github.com/topics/camera-effects"),
            EffectLink("OpenGL ES", "GitHub topic", "https://github.com/topics/opengl-es"),
        )),
        EffectLinkCategory("PC streaming", listOf(
            EffectLink("OBS Studio", "Streaming software", "https://obsproject.com"),
        )),
    )

    const val LICENSE_NOTE = "Check each asset's license before baking it in."

    fun find(url: String): EffectLink? = categories.asSequence().flatMap { it.links.asSequence() }.firstOrNull {
        it.url == url
    }
}
