package com.aurora.camera.compat.vignette;
import java.util.Locale;

/** AUTHVignette 1.1.0 formula and its original nonlinear strength scale. */
public final class AuthFormula239 {
    private AuthFormula239(){}
    public static int strength(int position){return position<=400?Math.round(position/2f):position-200;}
    public static int position(int percent){return percent<=200?percent*2:percent+200;}
    public static String formula(int strength,boolean protect,int range,boolean shape){
        if(strength<0||strength>400||range<0||range>150)throw new IllegalArgumentException("AUTH vignette adjustment outside supported range");
        if(strength==0||range==0)return "return color;\n";
        String mask=(shape?"uv.x = (uv.x - 0.5) * 0.75 + 0.5;\n":"")+"float d = distance(uv, vec2(0.5));\n"
            +(range==100?"":"d *= "+String.format(Locale.ROOT,"%.2f",range/100.0)+";\n")
            +"float m = smoothstep(PREFIX(SmoothStartValue), PREFIX(Falloff) * PREFIX(SmoothEndValue), d * darkStrength);\n";
        String gain=protect?"float y = dot(color.rgb, vec3(0.2125, 0.7154, 0.0721));\nfloat h = 1.0 / pow(100.0, 1.0-y);\nfloat gain = (1.0-h)*m+h;\n":"float gain = m;\n";
        return mask+gain+"return color * pow(max(gain, 0.0), "+String.format(Locale.ROOT,"%.2f",strength/100.0)+");\n";
    }
    public static String preview(String template,String body){
        if(!template.contains("/*PHOENIX_BODY*/"))throw new IllegalArgumentException("AUTH shader template marker missing");
        return "#version 310 es\n"+template.replace("varying vec2 tc;","in vec2 vTexCoord;\nout vec4 outColor;")
            .replace("uniform sampler2D tex;","uniform sampler2D text_source;")
            .replace("texture2D(tex,x)","texture(text_source,x)")
            .replace("/*PHOENIX_BODY*/",body)
            .replace("void main() { gl_FragColor = FUNCNAME(tc); }","void main() { outColor = FUNCNAME(vTexCoord); }");
    }
}
