precision highp float;

varying vec2 vTextureCo;
varying vec2 leftTextureCoordinate;
varying vec2 rightTextureCoordinate;

varying vec2 topTextureCoordinate;
varying vec2 topLeftTextureCoordinate;
varying vec2 topRightTextureCoordinate;

varying vec2 bottomTextureCoordinate;
varying vec2 bottomLeftTextureCoordinate;
varying vec2 bottomRightTextureCoordinate;

uniform sampler2D uTexture;

uniform int vChangeType;
uniform highp float intensity;
uniform float uWidth;
uniform float uHeight;

const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);//灰度滤镜
const highp vec3 COOL = vec3(0.0, 0.0, 0.1);//冷色调
const highp vec3 WARM = vec3(0.1, 0.1, 0.0);//暖色调
const vec2 texSize = vec2(1920,1080);//浮雕参数
const lowp float intensityone = 1.0;
const lowp mat4 colorMatrix = mat4(0.3588, 0.7044, 0.1368, 0.0,0.2990, 0.5870, 0.1140, 0.0,0.2392, 0.4696, 0.0912, 0.0,0, 0, 0, 1.0);
const highp float threshold = 0.2;
const highp float quantizationLevels = 10.0;
const mediump mat3 convolutionMatrix = mat3(-1.0, 0.0, 1.0,-2.0, 0.0, 2.0,-1.0, 0.0, 1.0);

const float stepcv=1.;
const mat3 GX=mat3(-1.,0., +1., -2., 0., +2., -1., 0., +1.);
const mat3 GY=mat3(-1., -2., -1., 0., 0., 0., +1., +2., +1.);

float colorR(vec2 center,float shiftX,float shiftY){
    return texture2D(uTexture,vec2(vTextureCo.x+shiftX/uWidth,vTextureCo.y+shiftY/uHeight)).r;
}

void main()
{
    vec4 textureColor = texture2D(uTexture, vTextureCo);
    if(vChangeType == 0){
        gl_FragColor = textureColor;
    }
    else if(vChangeType == 1){
        vec4 nColor=texture2D(uTexture,vTextureCo);
        vec4 deltaColor=nColor+vec4(COOL,0.0);
        gl_FragColor=deltaColor;
    }
    else if(vChangeType == 2){
        vec4 nColor=texture2D(uTexture,vTextureCo);
        vec4 deltaColor=nColor+vec4(WARM,0.0);
        gl_FragColor=deltaColor;
    }
    else if(vChangeType == 3){
        gl_FragColor=vec4(vec3(dot(texture2D( uTexture, vTextureCo).rgb,W)),1.0);
    }
    else if(vChangeType == 4){
        vec2 tex = vTextureCo;
        vec2 upLeftUV = vec2(tex.x - 1.0/texSize.x, tex.y - 1.0/texSize.y);
        vec4 upLeftColor = texture2D(uTexture,upLeftUV);
        vec4 delColor = textureColor - upLeftColor;
        float h = 0.3*delColor.x + 0.59*delColor.y + 0.11*delColor.z;
        vec4 bkColor = vec4(0.5, 0.5, 0.5, 1.0);
        gl_FragColor = vec4(h,h,h,0.0) +bkColor;
    }
    else if(vChangeType == 5){
        gl_FragColor = vec4((1.0 - textureColor.rgb), textureColor.w);
    }
    else if(vChangeType == 6){
        lowp vec4 outputColor = textureColor * colorMatrix;
        gl_FragColor = (intensityone * outputColor) + ((1.0 - intensityone) * textureColor);
    }
    else if(vChangeType == 7){
        float bottomLeftIntensity = texture2D(uTexture, bottomLeftTextureCoordinate).r;
        float topRightIntensity = texture2D(uTexture, topRightTextureCoordinate).r;
        float topLeftIntensity = texture2D(uTexture, topLeftTextureCoordinate).r;
        float bottomRightIntensity = texture2D(uTexture, bottomRightTextureCoordinate).r;
        float leftIntensity = texture2D(uTexture, leftTextureCoordinate).r;
        float rightIntensity = texture2D(uTexture, rightTextureCoordinate).r;
        float bottomIntensity = texture2D(uTexture, bottomTextureCoordinate).r;
        float topIntensity = texture2D(uTexture, topTextureCoordinate).r;
        float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;
        float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;

        float mag = length(vec2(h, v));
        vec3 posterizedImageColor = floor((textureColor.rgb * quantizationLevels) + 0.5) / quantizationLevels;
        float thresholdTest = 1.0 - step(threshold, mag);
        gl_FragColor = vec4(posterizedImageColor * thresholdTest, textureColor.a);
    }
    else if(vChangeType == 8){
         mediump vec4 bottomColor = texture2D(uTexture, bottomTextureCoordinate);
         mediump vec4 bottomLeftColor = texture2D(uTexture, bottomLeftTextureCoordinate);
         mediump vec4 bottomRightColor = texture2D(uTexture, bottomRightTextureCoordinate);
         mediump vec4 leftColor = texture2D(uTexture, leftTextureCoordinate);
         mediump vec4 rightColor = texture2D(uTexture, rightTextureCoordinate);
         mediump vec4 topColor = texture2D(uTexture, topTextureCoordinate);
         mediump vec4 topRightColor = texture2D(uTexture, topRightTextureCoordinate);
         mediump vec4 topLeftColor = texture2D(uTexture, topLeftTextureCoordinate);

         mediump vec4 resultColor = topLeftColor * convolutionMatrix[0][0] + topColor * convolutionMatrix[0][1] + topRightColor * convolutionMatrix[0][2];
         resultColor += leftColor * convolutionMatrix[1][0] + textureColor * convolutionMatrix[1][1] + rightColor * convolutionMatrix[1][2];
         resultColor += bottomLeftColor * convolutionMatrix[2][0] + bottomColor * convolutionMatrix[2][1] + bottomRightColor * convolutionMatrix[2][2];
         gl_FragColor = resultColor;
    }
    else if(vChangeType == 9){
        vec2 center=vec2(vTextureCo.x*uWidth,vTextureCo.y*uHeight);
        float leftTop=colorR(center,-stepcv,-stepcv);
        float centerTop=colorR(center,0.,-stepcv);
        float rightTop=colorR(center,stepcv,-stepcv);
        float leftCenter=colorR(center,-stepcv,0.);
        float rightCenter=colorR(center,stepcv,0.);
        float leftBottom=colorR(center,-stepcv,stepcv);
        float centerBottom=colorR(center,0.,stepcv);
        float rightBottom=colorR(center,stepcv,stepcv);
        mat3 d=mat3(colorR(center,-stepcv,-stepcv),colorR(center,0.,-stepcv),colorR(center,stepcv,-stepcv),
                     colorR(center,-stepcv,0.),colorR(center,0.,0.),colorR(center,stepcv,0.),
                     colorR(center,-stepcv,stepcv),colorR(center,0.,stepcv),colorR(center,stepcv,stepcv));
        float x = d[0][0]*GX[0][0]+d[1][0]*GX[1][0]+d[2][0]*GX[2][0]+
                   d[0][1]*GX[0][1]+d[1][1]*GX[1][1]+d[2][1]*GX[2][1]+
                   d[0][2]*GX[0][2]+d[1][2]*GX[1][2]+d[2][2]*GX[2][2];
        float y = d[0][0]*GY[0][0]+d[1][0]*GY[1][0]+d[2][0]*GY[2][0]+
                   d[0][1]*GY[0][1]+d[1][1]*GY[1][1]+d[2][1]*GY[2][1]+
                   d[0][2]*GY[0][2]+d[1][2]*GY[1][2]+d[2][2]*GY[2][2];
        gl_FragColor=vec4(vec3(length(vec2(x,y))),1.);
    }
    else if(vChangeType == 10){
        float bottomLeftIntensity = texture2D(uTexture, bottomLeftTextureCoordinate).r;
        float topRightIntensity = texture2D(uTexture, topRightTextureCoordinate).r;
        float topLeftIntensity = texture2D(uTexture, topLeftTextureCoordinate).r;
        float bottomRightIntensity = texture2D(uTexture, bottomRightTextureCoordinate).r;
        float leftIntensity = texture2D(uTexture, leftTextureCoordinate).r;
        float rightIntensity = texture2D(uTexture, rightTextureCoordinate).r;
        float bottomIntensity = texture2D(uTexture, bottomTextureCoordinate).r;
        float topIntensity = texture2D(uTexture, topTextureCoordinate).r;
        float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;
        float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;
        
        float mag = 1.0 - length(vec2(h, v));
        gl_FragColor = vec4(vec3(mag), 1.0);
    }
    else{
        gl_FragColor = textureColor;
    }

}