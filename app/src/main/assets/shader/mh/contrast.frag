precision mediump float;
varying vec2 vTextureCo;
uniform sampler2D uTexture;
uniform lowp float stepcv;
void main() {
    lowp vec4 textureColor = texture2D(uTexture, vTextureCo);
    gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * stepcv + vec3(0.5)), textureColor.w);
}