#extension GL_OES_standard_derivatives : enable
precision mediump float;

uniform float visibility;
uniform float alphaTest;
uniform float drawMode;
uniform float nearCutOff;
uniform float farCutOff;
varying vec4 vColor;
varying float depth;
varying float vCounters;
float map(float value, float inMin, float inMax, float outMin, float outMax) {
//  return outMin + (outMax - outMin) * (value - inMin) / (inMax - inMin);
  return ((value - inMin) / (inMax - inMin) * (outMax - outMin) + outMin);
}


void main() {
    vec4 c = vColor;
	if( c.a < alphaTest ) discard;

	if(drawMode > 0.0){
        if(depth <= nearCutOff){
            c.gb *= map(depth, nearCutOff, nearCutOff * 0.95f, 0.0, 1.0);
        }
        if(depth > nearCutOff && depth < farCutOff){
             c.gb *= 0.0f;
        }
        if(depth >= farCutOff){
            c.gb *= map(depth, farCutOff, farCutOff * 1.05f, 0.0, 1.0);
        }
	}


    gl_FragColor = c;

	gl_FragColor.a *= step(vCounters,visibility);
} 