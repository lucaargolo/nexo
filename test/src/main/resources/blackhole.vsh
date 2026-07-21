in vec3 aPosition;
in vec4 aColor;
in vec2 aTexCoord0;

uniform mat4 iModelView;
uniform mat4 iProjection;

out vec2 vTexCoord;
out vec4 vColor;

void main() {
    gl_Position = iProjection * iModelView * vec4(aPosition, 1.0);
    vTexCoord = aTexCoord0;
    vColor = aColor;
}