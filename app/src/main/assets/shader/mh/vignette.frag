precision mediump float;
varying vec2 vTextureCo;
uniform sampler2D uTexture;

uniform lowp vec2 vignetteCenter;
uniform lowp vec3 vignetteColor;
uniform highp float vignetteStart;
uniform highp float vignetteEnd;

void main() {
    lowp vec3 rgb = texture2D(uTexture, vTextureCo).rgb;
    lowp float d = distance(vTextureCo, vec2(0.5,0.5));
    rgb *= (1.0 - smoothstep(vignetteStart, vignetteEnd, d));
    gl_FragColor = vec4(vec3(rgb),1.0);
}