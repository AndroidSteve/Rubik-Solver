/**
 * Augmented Reality Rubik Cube Wizard
 * 
 * Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
 * Date:   April 25th 2015
 * 
 * Project Description:
 *   Android application developed on a commercial Smart Phone which, when run on a pair 
 *   of Smart Glasses, guides a user through the process of solving a Rubik Cube.
 *   
 * File Description:
 *   An Arrow in 3D space made up of 180 separate triangles, two triangles in 
 *   each degree and each flat segment every one degree is drawn.  The arrow
 *   is drawn in a quarter turn in the X-Y plane in quadrant I at a radius of 1.0 with the head
 *   pointed at the X axis.  The width of the arrow is in the Z axis (+/- 0.3) with the 
 *   head having maximum Z axis dimensions of +/- 0.6.
 *   
 *   Optionally, an arrow can be constructed that spans two quadrants (I and II) 
 *   thus conveying a full 180 degrees instead of 90 degrees.
 * 
 * License:
 * 
 *  GPL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ar.rubik.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.opencv.core.Scalar;

import android.opengl.GLES20;

/**
 * An arrow in three-dimensional space for use as a drawn object in OpenGL ES 2.0.
 */
public class GLArrow {

    public enum Amount { QUARTER_TURN, HALF_TURN };
    
    // Buffer for Arrow vertex-array
    private FloatBuffer vertexBufferArrow;
    
    // Buffer for Arrow Outline vertex-array
    private FloatBuffer vertexBufferOutline;

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    
    // number of bytes in a float
    private static final int BYTES_PER_FLOAT = 4;

    // number of total bytes in vertex stride: 12 in this case.
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT;

    // number of vertices in the arrow arch
    private static final int VERTICES_PER_ARCH = 90 + 1;
    
    // OpenGL Opaque Black Color
    private static final float [] opaqueBlack = { 0.0f, 0.0f, 0.0f, 1.0f};

    
    
    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     * @param programID2 
     */
    public GLArrow(Amount amount) {
        
        double angleScale = (amount == Amount.QUARTER_TURN) ? 1.0 : 3.0;
        
        float[] verticesArrow = new float[VERTICES_PER_ARCH * 6];
        float[] verticesOutline = new float[VERTICES_PER_ARCH * 6];
        
        for(int i=0; i<VERTICES_PER_ARCH; i++)  {
            
            // Angle will range from 0 to 90, or 0 to 180.
            double angleRads = i * angleScale * Math.PI / 180.0;
            float x = (float) Math.cos(angleRads);
            float y = (float) Math.sin(angleRads);
            
            verticesArrow[i*6 + 0] = x;
            verticesArrow[i*6 + 1] = y;
            verticesArrow[i*6 + 2] = -1.0f * calculateWidth(angleRads);

            verticesArrow[i*6 + 3] = x;
            verticesArrow[i*6 + 4] = y;
            verticesArrow[i*6 + 5] = +1.0f * calculateWidth(angleRads);
            
            
            // Fill from 0 to 272
            verticesOutline[i*3 + 0] = x;
            verticesOutline[i*3 + 1] = y;
            verticesOutline[i*3 + 2] = -1.0f * calculateWidth(angleRads);

            // Fill from 545 to 273
            verticesOutline[(180 - i)*3 + 3] = x;
            verticesOutline[(180 - i)*3 + 4] = y;
            verticesOutline[(180 - i)*3 + 5] = calculateWidth(angleRads);
        }
        
        // Setup vertex-array buffer. Vertices in float. A float has 4 bytes
        // This reserves memory that GPU has direct access to (correct?).
        ByteBuffer arrowVbb = ByteBuffer.allocateDirect(verticesArrow.length * 4);
        arrowVbb.order(ByteOrder.nativeOrder()); // Use native byte order
        vertexBufferArrow = arrowVbb.asFloatBuffer(); // Convert from byte to float
        vertexBufferArrow.put(verticesArrow);         // Copy data into buffer
        vertexBufferArrow.position(0);           // Rewind
        
        ByteBuffer outlineVbb = ByteBuffer.allocateDirect(verticesOutline.length * 4);
        outlineVbb.order(ByteOrder.nativeOrder());
        vertexBufferOutline = outlineVbb.asFloatBuffer();
        vertexBufferOutline.put(verticesOutline);
        vertexBufferOutline.position(0);
    }
    
    
    /**
     * Calculate Width
     * 
     * The width of the arrow is dependent upon the angle with the X axis.  For the 
     * first 20 degrees, the arrow width increases to 0.6 to form the head, there after
     * the arrow width is constant at a width of 0.3.
     * 
     * @param angleRads
     * @return
     */
    private float calculateWidth(double angleRads) {
        
        double angleDegrees = angleRads * 180.0 / Math.PI;
        
        // Arrow Body - A constant width of 0.3.
        if(angleDegrees > 20.0)
            return 0.3f;
        
        // Arrow Head - Ranges from a width of 0.0 to 0.6.
        else
            return (float) (angleDegrees / 20.0 * 0.6);
    }

    
    
    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw this shape.
     * @param color - Color to apply to arrow
     */
    public void draw(float[] mvpMatrix, Scalar color, int programID) {
               
        // Add program to OpenGL environment
        GLES20.glUseProgram(programID);

        // get handle to vertex shader's vPosition member
        int vertexArrayID = GLES20.glGetAttribLocation(programID, "vPosition");

        // Enable a handle to the cube vertices
        GLES20.glEnableVertexAttribArray(vertexArrayID);
        
        // get handle to fragment shader's vColor member
        int colorID = GLES20.glGetUniformLocation(programID, "vColor");

        // get handle to shape's transformation matrix
        int mvpMatrixID = GLES20.glGetUniformLocation(programID, "uMVPMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mvpMatrixID, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");        
 

        // Draw Arrow Outer Surface
        drawArrowOuterSurface(color, colorID, vertexArrayID);
        
        // Draw Arrow Inner Surface (a bit darker than above)
        drawArrowInnerSurface(color, colorID, vertexArrayID);

        //Draw Arrow Outline
        drawArrowOutline(colorID, vertexArrayID);


        // Disable vertex array
        GLES20.glDisableVertexAttribArray(vertexArrayID);        
        
        // Disable cull face: i.e., will now attempt to render both sides instead of just front
        GLES20.glDisable(GLES20.GL_CULL_FACE);
    }


    /**
     * Draw Arrow Outline
     * 
     * @param colorID 
     * @param vertexArrayID
     */
    private void drawArrowOutline(int colorID, int vertexArrayID) {
        
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        
        // Prepare the cube coordinate data
        GLES20.glVertexAttribPointer(
                vertexArrayID, 
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                vertexBufferOutline);
        

        GLES20.glUniform4fv(colorID, 1, opaqueBlack, 0);
        
        GLES20.glLineWidth(10.0f);

        // Draw Triangles
        GLES20.glDrawArrays(
                GLES20.GL_LINE_STRIP, 
                0, 
                VERTICES_PER_ARCH * 2);  // Number of triangles to be drawn
    }


    /**
     * Draw Arrow Outer Side
     * 
     * @param color
     * @param colorID
     * @param vertexArrayID
     */
    private void drawArrowOuterSurface(Scalar color, int colorID, int vertexArrayID) {
        
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        
        // Prepare the cube coordinate data
        GLES20.glVertexAttribPointer(
                vertexArrayID, 
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                vertexBufferArrow);
        
        // Draw Outer Side to specified color
        // Translate to GL Color
        float [] glFrontSideColor = {
                (float)color.val[0] / 256.0f,
                (float)color.val[1] / 256.0f,
                (float)color.val[2] / 256.0f,
                1.0f
        };
        GLES20.glUniform4fv(colorID, 1, glFrontSideColor, 0);

        GLES20.glCullFace(GLES20.GL_FRONT);

        // Draw Triangles
        GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_STRIP, 
                0, 
                VERTICES_PER_ARCH * 2);  // Number of triangles to be drawn
    }


    /**
     * Draw Arrow Inner Side
     * 
     * @param color
     * @param colorID
     * @param vertexArrayID
     */
    private void drawArrowInnerSurface(Scalar color, int colorID, int vertexArrayID) {
        
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        
        // Prepare the cube coordinate data
        GLES20.glVertexAttribPointer(
                vertexArrayID, 
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                vertexBufferArrow);
        
        // Draw Inner Side a bit darker
        // Translate to GL Color and make a bit darker
        float [] glBackSideColor = {
                (float)color.val[0] / (256.0f + 256.0f),
                (float)color.val[1] / (256.0f + 256.0f),
                (float)color.val[2] / (256.0f + 256.0f),
                1.0f
        };
        GLES20.glUniform4fv(colorID, 1, glBackSideColor, 0);

        GLES20.glCullFace(GLES20.GL_BACK);

        // Draw Triangles
        GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_STRIP, 
                0, 
                VERTICES_PER_ARCH * 2);  // Number of triangles to be drawn
    }
}