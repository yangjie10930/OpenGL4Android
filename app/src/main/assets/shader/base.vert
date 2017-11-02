attribute vec4 aVertexCo;
attribute vec2 aTextureCo;

uniform mat4 uVertexMatrix;
uniform mat4 uTextureMatrix;

varying vec2 vTextureCo;

void main(){
    gl_Position = uVertexMatrix*aVertexCo;
    vTextureCo = aTextureCo.xy;
}