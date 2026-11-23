#version 150

in vec3 WorldPosition;
in vec4 VertexColor;

uniform vec3 CameraPosition;
uniform float GameTime;
uniform float CloudCoverage;
uniform float StormFactor;
uniform float CloudBase;
uniform float CloudTop;
uniform float RaySteps;
uniform float LightingQuality;

out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash31(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);
    return mix(mix(x00, x10, f.y), mix(x01, x11, f.y), f.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; ++i) {
        value += noise3(p) * amplitude;
        p *= 2.03;
        amplitude *= 0.5;
    }
    return value;
}

float cloudDensity(vec3 p) {
    float h = clamp((p.y - CloudBase) / max(CloudTop - CloudBase, 1.0), 0.0, 1.0);
    float topHeight = mix(0.66, 0.94, StormFactor);
    float verticalShape = smoothstep(0.015, 0.16, h)
        * (1.0 - smoothstep(topHeight, 1.0, h));

    vec3 broadCoord = vec3(
        p.x * 0.00165 + GameTime * 0.018,
        p.y * 0.0042,
        p.z * 0.00165 - GameTime * 0.011
    );
    float broad = fbm(broadCoord);
    float erosion = fbm(p * vec3(0.010, 0.014, 0.010) + GameTime * 0.025);
    float field = broad + (erosion - 0.5) * mix(0.12, 0.24, LightingQuality);
    float threshold = mix(0.72, 0.43, CloudCoverage);
    float body = smoothstep(threshold, threshold + 0.19, field);
    return body * verticalShape;
}

void main() {
    vec3 rayDirection = normalize(WorldPosition - CameraPosition);
    if (rayDirection.y <= 0.015) {
        discard;
    }

    float steps = clamp(RaySteps, 6.0, 56.0);
    float stepLength = (CloudTop - CloudBase) / (steps * max(rayDirection.y, 0.08));
    vec3 accumulatedLight = vec3(0.0);
    float transmittance = 1.0;
    vec3 sunDirection = normalize(vec3(-0.35, 0.82, 0.28));

    for (int i = 0; i < 56; ++i) {
        if (float(i) >= steps || transmittance < 0.025) {
            break;
        }

        vec3 samplePosition = WorldPosition
            + rayDirection * ((float(i) + 0.5) * stepLength);
        float density = cloudDensity(samplePosition);
        if (density > 0.002) {
            float height = clamp(
                (samplePosition.y - CloudBase) / max(CloudTop - CloudBase, 1.0),
                0.0, 1.0
            );
            float silverLining = pow(max(dot(-rayDirection, sunDirection), 0.0), 3.0);
            float lighting = 0.60 + height * 0.18 + silverLining * 0.22;
            float opacity = 1.0 - exp(-density * stepLength * 0.012);
            vec3 fairColor = vec3(0.88, 0.91, 0.96);
            vec3 stormColor = vec3(0.39, 0.43, 0.50);
            vec3 cloudColor = mix(fairColor, stormColor, StormFactor) * lighting;
            accumulatedLight += transmittance * opacity * cloudColor;
            transmittance *= (1.0 - opacity);
        }
    }

    float alpha = 1.0 - transmittance;
    if (alpha < 0.008) {
        discard;
    }

    vec3 finalColor = accumulatedLight / max(alpha, 0.001);
    fragColor = vec4(finalColor, alpha) * VertexColor;
}
