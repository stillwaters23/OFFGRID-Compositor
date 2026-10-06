package com.offgrid.compositor

import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceProcessor
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/**
 * The one camera effect for preview and video together.
 * A second effect is not allowed on the same preview.
 */
class CompositorEffect(
    executor: Executor,
    processor: SurfaceProcessor,
    onError: Consumer<Throwable>,
) : CameraEffect(
    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
    executor,
    processor,
    onError,
)
