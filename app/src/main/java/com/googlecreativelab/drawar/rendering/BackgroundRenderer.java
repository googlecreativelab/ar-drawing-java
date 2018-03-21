/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecreativelab.drawar.rendering;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.googlecreativelab.drawar.R;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class renders the AR background from camera feed. It creates and hosts the texture
 * given to ARCore to be filled with the camera image.
 */
public class BackgroundRenderer {
    private static final String TAG = BackgroundRenderer.class.getSimpleName();

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int FLOAT_SIZE = 4;

    private FloatBuffer mQuadVertices;
    private FloatBuffer mQuadTexCoord;
    private FloatBuffer mQuadTexCoordTransformed;

    private int mQuadProgram;

    private int mQuadPositionParam;
    private int mQuadTexCoordParam;
    private int mTextureId = -1;
    private int mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    public BackgroundRenderer() {
    }

    /**
     * @return
     */
    public int getTextureId() {
        return mTextureId;
    }

    /**
     * Allocates and initializes OpenGL resources needed by the background renderer.  Must be
     * called on the OpenGL thread, typically in
     * {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
     *
     * @param context Needed to access shader source.
     */
    public void createOnGlThread(Context context) {
        // Generate the background texture.
        int textures[] = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        GLES20.glBindTexture(mTextureTarget, mTextureId);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        int numVertices = 4;
        if (numVertices != QUAD_COORDS.length / COORDS_PER_VERTEX) {
          throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer.");
        }

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
        bbVertices.order(ByteOrder.nativeOrder());
        mQuadVertices = bbVertices.asFloatBuffer();
        mQuadVertices.put(QUAD_COORDS);
        mQuadVertices.position(0);

        ByteBuffer bbTexCoords = ByteBuffer.allocateDirect(
                numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoords.order(ByteOrder.nativeOrder());
        mQuadTexCoord = bbTexCoords.asFloatBuffer();
        mQuadTexCoord.put(QUAD_TEXCOORDS);
        mQuadTexCoord.position(0);

        ByteBuffer bbTexCoordsTransformed = ByteBuffer.allocateDirect(
            numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
        mQuadTexCoordTransformed = bbTexCoordsTransformed.asFloatBuffer();

        int vertexShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_VERTEX_SHADER, R.raw.screenquad_vertex);
        int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.screenquad_fragment_oes);

        mQuadProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mQuadProgram, vertexShader);
        GLES20.glAttachShader(mQuadProgram, fragmentShader);
        GLES20.glLinkProgram(mQuadProgram);
        GLES20.glUseProgram(mQuadProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        mQuadPositionParam = GLES20.glGetAttribLocation(mQuadProgram, "a_Position");
        mQuadTexCoordParam = GLES20.glGetAttribLocation(mQuadProgram, "a_TexCoord");

        ShaderUtil.checkGLError(TAG, "Program parameters");
    }

    /**
     * Draws the AR background image.  The image will be drawn such that virtual content rendered
     * with the matrices provided by {@link Frame#getViewMatrix(float[], int)} and
     * {@link Session#getProjectionMatrix(float[], int, float, float)} will accurately follow
     * static physical objects.  This must be called <b>before</b> drawing virtual content.
     *
     * @param frame The last {@code Frame} returned by {@link Session#update()}.
     */
    public void draw(Frame frame) {
        if (frame == null) return;

        // If display rotation changed (also includes view size change), we need to re-query the uv
        // coordinates for the screen rect, as they may have changed as well.
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformDisplayUvCoords(mQuadTexCoord, mQuadTexCoordTransformed);
        }

        // No need to test or write depth, the screen quad has arbitrary depth, and is expected
        // to be drawn first.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);

        GLES20.glUseProgram(mQuadProgram);

        // Set the vertex positions.
        GLES20.glVertexAttribPointer(
            mQuadPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mQuadVertices);

        // Set the texture coordinates.
        GLES20.glVertexAttribPointer(mQuadTexCoordParam, TEXCOORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, mQuadTexCoordTransformed);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mQuadPositionParam);
        GLES20.glEnableVertexAttribArray(mQuadTexCoordParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mQuadPositionParam);
        GLES20.glDisableVertexAttribArray(mQuadTexCoordParam);

        // Restore the depth state for further drawing.
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        ShaderUtil.checkGLError(TAG, "Draw");
    }

    public static final float[] QUAD_COORDS = new float[]{
            -1.0f, -1.0f, 0.0f,
            -1.0f, +1.0f, 0.0f,
            +1.0f, -1.0f, 0.0f,
            +1.0f, +1.0f, 0.0f,
    };

    public static final float[] QUAD_TEXCOORDS = new float[]{
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };
}
