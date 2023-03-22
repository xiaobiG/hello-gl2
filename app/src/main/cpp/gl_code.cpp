/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// OpenGL ES 2.0 code

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <android/log.h>
#include <jni.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#define LOG_TAG "libgl2jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void printGLString(const char* name, GLenum s) {
  const char* v = (const char*)glGetString(s);
  LOGI("GL %s = %s\n", name, v);
}

static void checkGlError(const char* op) {
  for (GLint error = glGetError(); error; error = glGetError()) {
    LOGI("after %s() glError (0x%x)\n", op, error);
  }
}

auto gVertexShader =
    "attribute vec4 vPosition;\n"
    "void main() {\n"
    "  gl_Position = vPosition;\n"
    "}\n";

auto gFragmentShader =
    "precision mediump float;\n"
    "void main() {\n"
    "  gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n"
    "}\n";

GLuint loadShader(GLenum shaderType, const char* pSource) {
  GLuint shader = glCreateShader(shaderType);
  if (shader) {
    glShaderSource(shader, 1, &pSource, NULL);
    glCompileShader(shader);
    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
      GLint infoLen = 0;
      glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
      if (infoLen) {
        char* buf = (char*)malloc(infoLen);
        if (buf) {
          glGetShaderInfoLog(shader, infoLen, NULL, buf);
          LOGE("Could not compile shader %d:\n%s\n", shaderType, buf);
          free(buf);
        }
        glDeleteShader(shader);
        shader = 0;
      }
    }
  }
  return shader;
}

/**
用于创建着色器程序对象。它的输入参数是两个字符串，分别代表了顶点着色器和像素着色器的源码。

函数的第一步是调用loadShader函数来加载并编译顶点着色器和像素着色器。
如果加载或编译失败，则返回0，表示创建着色器程序对象失败。
如果顶点着色器和像素着色器都成功加载和编译，那么接下来就会创建一个新的着色器程序对象，并将顶点着色器和像素着色器附加到该对象上。
然后，使用glLinkProgram将它们链接在一起，最后检查链接过程是否成功。
如果链接成功，函数返回新创建的着色器程序对象的句柄，否则返回0。如果链接失败，函数将打印出错误信息并释放已经创建的着色器程序对象。
 */
GLuint createProgram(const char* pVertexSource, const char* pFragmentSource) {
  GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexSource);
  if (!vertexShader) {
    return 0;
  }

  GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, pFragmentSource);
  if (!pixelShader) {
    return 0;
  }

  GLuint program = glCreateProgram();
  if (program) {
    glAttachShader(program, vertexShader);
    checkGlError("glAttachShader");
    glAttachShader(program, pixelShader);
    checkGlError("glAttachShader");
    glLinkProgram(program);
    GLint linkStatus = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
    if (linkStatus != GL_TRUE) {
      GLint bufLength = 0;
      glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
      if (bufLength) {
        char* buf = (char*)malloc(bufLength);
        if (buf) {
          glGetProgramInfoLog(program, bufLength, NULL, buf);
          LOGE("Could not link program:\n%s\n", buf);
          free(buf);
        }
      }
      glDeleteProgram(program);
      program = 0;
    }
  }
  return program;
}

GLuint gProgram;
GLuint gvPositionHandle;

/**
该函数以屏幕宽度和高度为输入参数，并返回布尔值，指示设置是否成功。

函数开始时，调用printGLString()函数打印OpenGL实现的版本、供应商、渲染器和扩展名等信息。
接下来，函数使用gVertexShader和gFragmentShader变量创建一个程序。如果程序创建失败，则记录错误消息并返回false。
然后，函数使用glGetAttribLocation()检索程序中"vPosition"属性的位置，并将其存储在gvPositionHandle变量中。同时，函数使用checkGlError()检查是否有任何错误。
最后，函数将视口设置为屏幕的尺寸，并使用checkGlError()检查是否有任何错误。函数返回true，表示设置成功。
 */
bool setupGraphics(int w, int h) {
  printGLString("Version", GL_VERSION);
  printGLString("Vendor", GL_VENDOR);
  printGLString("Renderer", GL_RENDERER);
  printGLString("Extensions", GL_EXTENSIONS);

  LOGI("setupGraphics(%d, %d)", w, h);
  gProgram = createProgram(gVertexShader, gFragmentShader);
  if (!gProgram) {
    LOGE("Could not create program.");
    return false;
  }
  gvPositionHandle = glGetAttribLocation(gProgram, "vPosition");
  checkGlError("glGetAttribLocation");
  LOGI("glGetAttribLocation(\"vPosition\") = %d\n", gvPositionHandle);

  glViewport(0, 0, w, h);
  checkGlError("glViewport");
  return true;
}

// 包含三角形顶点位置信息的数组。三角形顶点的坐标分别为 (0.0, 0.5)，(-0.5,-0.5)，(0.5,-0.5)
const GLfloat gTriangleVertices[] = {0.0f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f};

/**
首先通过改变背景颜色的灰度值每帧增加一定量的灰度值，以产生灰度不断变化的效果。然后使用 glClearColor 函数清除深度缓冲区和颜色缓冲区。
接着调用 glUseProgram 函数选择要使用的程序对象，即 gProgram。
然后使用 glVertexAttribPointer 函数指定要绘制的图形的顶点数据，并启用它们。
最后使用 glDrawArrays 函数指定要绘制的图元类型和数量，即三角形。

在这段代码中还有几次调用 checkGlError 函数，用于检查 OpenGL ES 函数是否出错。
 */
void renderFrame() {
  static float grey;
  grey += 0.01f;
  if (grey > 1.0f) {
    grey = 0.0f;
  }
  glClearColor(grey, grey, grey, 1.0f);
  checkGlError("glClearColor");
  glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
  checkGlError("glClear");

  glUseProgram(gProgram);
  checkGlError("glUseProgram");

  glVertexAttribPointer(gvPositionHandle, 2, GL_FLOAT, GL_FALSE, 0,
                        gTriangleVertices);
  checkGlError("glVertexAttribPointer");
  glEnableVertexAttribArray(gvPositionHandle);
  checkGlError("glEnableVertexAttribArray");
  glDrawArrays(GL_TRIANGLES, 0, 3);
  checkGlError("glDrawArrays");
}

extern "C" {
JNIEXPORT void JNICALL Java_com_android_gl2jni_GL2JNILib_init(JNIEnv* env,
                                                              jobject obj,
                                                              jint width,
                                                              jint height);
JNIEXPORT void JNICALL Java_com_android_gl2jni_GL2JNILib_step(JNIEnv* env,
                                                              jobject obj);
};

JNIEXPORT void JNICALL Java_com_android_gl2jni_GL2JNILib_init(JNIEnv* env,
                                                              jobject obj,
                                                              jint width,
                                                              jint height) {
  setupGraphics(width, height);
}

JNIEXPORT void JNICALL Java_com_android_gl2jni_GL2JNILib_step(JNIEnv* env,
                                                              jobject obj) {
  renderFrame();
}
