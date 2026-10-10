package com.offgrid.compositor

/**
 * The GPU programs for the camera stage (final build, M4). All are OpenGL ES 2.0.
 *
 * The same text is also checked off the phone by a software renderer before every push,
 * so a typo here can't reach the phone as a black screen.
 */
internal object StageShaders {
    /** The camera, from its own texture. [uFlipX] 1 mirrors left and right (mirror plus scan). */
    const val CAMERA_VERTEX = """
        attribute vec2 aPosition;
        attribute vec2 aTexCoord;
        uniform mat4 uTexMatrix;
        uniform float uFlipX;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = vec4(aPosition, 0.0, 1.0);
            vec2 tc = vec2(mix(aTexCoord.x, 1.0 - aTexCoord.x, uFlipX), aTexCoord.y);
            vTexCoord = (uTexMatrix * vec4(tc, 0.0, 1.0)).xy;
        }
    """

    const val CAMERA_FRAGMENT = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTexCoord;
        uniform samplerExternalOES sTexture;
        uniform float uAlpha;
        void main() {
            vec4 c = texture2D(sTexture, vTexCoord);
            gl_FragColor = vec4(c.rgb * uAlpha, uAlpha);
        }
    """

    /**
     * The drawn picture of effects, from its own texture. It is already see-through where the
     * camera should show (premultiplied), so it is only scaled by [uAlpha].
     */
    const val OVERLAY_FRAGMENT = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTexCoord;
        uniform samplerExternalOES sTexture;
        uniform float uAlpha;
        void main() {
            gl_FragColor = texture2D(sTexture, vTexCoord) * uAlpha;
        }
    """

    /**
     * A picture the stage kept (the live picture, a held frame, an echo copy).
     * [uTexRect] picks the part of the picture to show: x, y offset then width, height.
     */
    const val PICTURE_VERTEX = """
        attribute vec2 aPosition;
        attribute vec2 aTexCoord;
        uniform vec4 uTexRect;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = vec4(aPosition, 0.0, 1.0);
            vTexCoord = aTexCoord * uTexRect.zw + uTexRect.xy;
        }
    """

    /**
     * Like [PICTURE_VERTEX], but each corner has its own depth (the 4th number), so a tilted
     * picture looks right in 3D (depth shift).
     */
    const val TILT_VERTEX = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        uniform vec4 uTexRect;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord * uTexRect.zw + uTexRect.xy;
        }
    """

    /** One glass piece of Shatter: the picture, darkened by [aShade] as the piece turns away. */
    const val SHARD_VERTEX = """
        attribute vec2 aPosition;
        attribute vec2 aTexCoord;
        attribute float aShade;
        varying vec2 vTexCoord;
        varying float vShade;
        void main() {
            gl_Position = vec4(aPosition, 0.0, 1.0);
            vTexCoord = aTexCoord;
            vShade = aShade;
        }
    """

    const val SHARD_FRAGMENT = """
        precision mediump float;
        varying vec2 vTexCoord;
        varying float vShade;
        uniform sampler2D sPicture;
        void main() {
            vec3 c = texture2D(sPicture, vTexCoord).rgb;
            gl_FragColor = vec4(c * vShade, 1.0);
        }
    """

    /**
     * [uBrightness] dims the picture (quiet mode). [uGlitch] splits the colors and slides a few
     * bands sideways (time freeze), for one short flicker on the way in and out.
     * The result is premultiplied: color times [uAlpha].
     */
    const val PICTURE_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D sPicture;
        uniform float uAlpha;
        uniform float uBrightness;
        uniform float uGlitch;
        uniform float uSeed;
        void main() {
            vec2 uv = vTexCoord;
            if (uGlitch > 0.0) {
                float band = floor(uv.y * 36.0);
                float pick = fract(sin(band * 12.9898 + uSeed * 78.233) * 43758.5453);
                uv.x += step(0.82, pick) * (pick - 0.82) * 1.2 * uGlitch;
            }
            vec3 c;
            if (uGlitch > 0.0) {
                float split = 0.012 * uGlitch;
                c.r = texture2D(sPicture, uv + vec2(split, 0.0)).r;
                c.g = texture2D(sPicture, uv).g;
                c.b = texture2D(sPicture, uv - vec2(split, 0.0)).b;
            } else {
                c = texture2D(sPicture, uv).rgb;
            }
            gl_FragColor = vec4(c * uBrightness * uAlpha, uAlpha);
        }
    """

    /**
     * The cloak void (feature 10): the box looks like empty space, a dark wormhole with faint
     * drifting light. [uMix] 0 is the camera, 1 is fully cloaked. While it changes, a ripple
     * bends the camera picture and a bright edge runs across, like a cloak turning on.
     * [uStrength] is how dark and empty the void is.
     */
    const val VOID_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D sPicture;
        uniform float uTime;
        uniform float uMix;
        uniform float uShimmer;
        uniform float uStrength;
        uniform float uAspect;
        uniform float uAlpha;

        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            float a = fract(sin(dot(i, vec2(127.1, 311.7))) * 43758.5453);
            float b = fract(sin(dot(i + vec2(1.0, 0.0), vec2(127.1, 311.7))) * 43758.5453);
            float c = fract(sin(dot(i + vec2(0.0, 1.0), vec2(127.1, 311.7))) * 43758.5453);
            float d = fract(sin(dot(i + vec2(1.0, 1.0), vec2(127.1, 311.7))) * 43758.5453);
            vec2 u = f * f * (3.0 - 2.0 * f);
            return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
        }

        vec3 voidColor(vec2 uv) {
            // Round in real pixels: x is scaled by the picture's width share.
            vec2 p = (uv - 0.5) * vec2(uAspect, 1.0);
            float r = length(p);
            // The swirl twists more toward the edges and turns slowly, like a tunnel into nothing.
            float a = r * 6.0 - uTime * 0.25;
            vec2 q = mat2(cos(a), -sin(a), sin(a), cos(a)) * p;
            float swirl = noise(q * 7.0 + vec2(uTime * 0.05, 0.0));
            float mist = noise(uv * 4.0 + vec2(uTime * 0.03, -uTime * 0.02));
            float walls = smoothstep(0.08, 0.55, r);
            vec3 deep = vec3(0.003, 0.004, 0.010);
            vec3 glow = vec3(0.05, 0.11, 0.19) * swirl * walls + vec3(0.03, 0.018, 0.06) * mist * walls;
            float star = step(0.985, noise(uv * 90.0 + vec2(0.0, uTime * 0.6))) * 0.35 * (0.4 + 0.6 * walls);
            return deep + glow * (1.3 - uStrength) + vec3(star);
        }

        void main() {
            vec2 uv = vTexCoord;
            float edge = 1.0 - abs(uMix * 2.0 - 1.0);
            float ripple = sin(uv.y * 38.0 - uTime * 9.0) * 0.012 + sin(uv.x * 24.0 + uTime * 6.0) * 0.008;
            vec2 bent = uv + vec2(ripple, ripple * 0.6) * uShimmer * edge;
            vec3 cam = texture2D(sPicture, clamp(bent, 0.0, 1.0)).rgb;
            // The void sweeps down from the top. At 0 it is all camera, at 1 all void.
            float front = uMix * 1.4 - 0.2;
            float wipe = 1.0 - smoothstep(front - 0.12, front + 0.12, 1.0 - uv.y + ripple * 2.0);
            float amount = clamp(mix(uMix, wipe, 0.5 * uShimmer), 0.0, 1.0);
            amount = max(amount, step(0.999, uMix)) * step(0.001, uMix);
            vec3 c = voidColor(uv);
            if (amount < 0.999) {
                c = mix(cam, c, amount);
            }
            float rim = (1.0 - smoothstep(0.0, 0.05, abs(1.0 - uv.y + ripple * 2.0 - front))) * edge * uShimmer;
            c += vec3(0.55, 0.85, 1.0) * rim * 0.7;
            gl_FragColor = vec4(c * uAlpha, uAlpha);
        }
    """

    /** A simple beach behind the shatter pieces: sky, sea with moving waves, and sand. */
    const val BEACH_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform float uTime;
        void main() {
            float y = vTexCoord.y;
            vec3 sky = mix(vec3(0.98, 0.83, 0.62), vec3(0.32, 0.62, 0.92), smoothstep(0.55, 1.0, y));
            float wave = sin(vTexCoord.x * 18.0 + uTime * 1.6) * 0.006 + sin(vTexCoord.x * 7.0 - uTime) * 0.008;
            float seaTop = 0.56 + wave;
            float shore = 0.30 + sin(vTexCoord.x * 5.0 + uTime * 0.7) * 0.015;
            vec3 sea = mix(vec3(0.05, 0.42, 0.55), vec3(0.10, 0.60, 0.70), smoothstep(shore, seaTop, y));
            vec3 sand = mix(vec3(0.86, 0.74, 0.55), vec3(0.94, 0.86, 0.68), y / 0.3);
            vec3 c = y > seaTop ? sky : (y > shore ? sea : sand);
            float foam = 1.0 - smoothstep(0.0, 0.012, abs(y - shore));
            c = mix(c, vec3(1.0), foam * 0.8);
            gl_FragColor = vec4(c, 1.0);
        }
    """

    /**
     * The person mask inside the video box (Lane E). [sMask] is the person, 0 to 1 in alpha.
     * [uKind] 1 cuts the body out, 2 replaces it, 3 cloaks the silhouette, 4 draws a hologram.
     * The camera colors of the person are never kept. The room around the person stays.
     * Motion stays under 3 flashes a second.
     */
    const val BODY_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D sPicture;
        uniform sampler2D sMask;
        uniform float uKind;
        uniform float uPlate;
        uniform float uCut;
        uniform float uTime;
        uniform float uAspect;

        void main() {
            vec3 cam = texture2D(sPicture, vTexCoord).rgb;
            float person = texture2D(sMask, vTexCoord).a;
            float body = smoothstep(uCut - 0.08, uCut + 0.02, person);
            vec3 outc = cam;
            if (uKind < 1.5) {
                outc = mix(cam, vec3(0.0), body);
            } else if (uKind < 2.5) {
                vec3 plate = vec3(0.0);
                if (uPlate > 0.5) {
                    vec2 p = (vTexCoord - 0.5) * vec2(uAspect, 1.0);
                    float r = length(p);
                    float swirl = fract(sin(dot(vec2(r, uTime * 0.2), vec2(127.1, 311.7))) * 43758.5453);
                    plate = vec3(0.012, 0.02, 0.045) + vec3(0.03, 0.07, 0.12) * swirl;
                }
                outc = mix(cam, plate, body);
            } else if (uKind < 3.5) {
                vec2 p = (vTexCoord - 0.5) * vec2(uAspect, 1.0);
                float r = length(p);
                float swirl = fract(sin(dot(vec2(r * 6.0, uTime * 0.25), vec2(127.1, 311.7))) * 43758.5453);
                vec3 voidc = vec3(0.008, 0.016, 0.04) + vec3(0.04, 0.09, 0.15) * swirl;
                float edge = smoothstep(0.18, 0.0, abs(person - uCut));
                voidc += vec3(0.35, 0.62, 0.9) * edge * (0.25 + 0.15 * sin(uTime * 6.0));
                outc = mix(cam, voidc, body);
            } else {
                float scan = 0.62 + 0.38 * sin(vTexCoord.y * 80.0 - uTime * 2.2);
                float glow = 0.84 + 0.16 * sin(uTime * 3.0);
                vec3 holo = vec3(0.12, 0.72, 0.95) * scan * glow;
                float edge = smoothstep(0.2, 0.0, abs(person - uCut));
                holo += vec3(0.45, 0.9, 1.0) * edge * 0.45;
                outc = mix(cam, holo, body);
            }
            gl_FragColor = vec4(outc, 1.0);
        }
    """

    /** One flat color. Used for crack lines, frame lines, and shadows. Premultiplied. */
    const val SHAPE_VERTEX = """
        attribute vec2 aPosition;
        void main() {
            gl_Position = vec4(aPosition, 0.0, 1.0);
        }
    """

    const val SHAPE_FRAGMENT = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = vec4(uColor.rgb * uColor.a, uColor.a);
        }
    """
}
