/**
 * Augmented Reality Rubik Cube Solver
 * 
 * Author: Steven P. Punte (aka Android Steve)
 * Date:   Nov 1st 2014
 * 
 * Project Description:
 *   Android application developed on a commercial Smart Phone which, when run on a pair 
 *   of Smart Glasses, guides a user through the process of solving a Rubik Cube.
 *   
 * File Description:
 *   Renders user instruction graphics; in particular wide white arrows to rotate entire
 *   cube or narrower colored arrows to rotate cube edges on a GL Surface.
 *   
 *   Also renders the "Pilot Cube" which appears on the right hand side in
 *   normal mode.  It tracks rotation of the cube, but not translation.
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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.ar.rubik.Constants;
import org.ar.rubik.CubeReconstructor;
import org.ar.rubik.MenuAndParams;
import org.ar.rubik.Constants.AppStateEnum;
import org.ar.rubik.Constants.FaceNameEnum;
import org.ar.rubik.StateModel;
import org.ar.rubik.gl.GLArrow.Amount;
import org.opencv.core.Scalar;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;


/**
 *  OpenGL Custom renderer used with GLSurfaceView 
 */
public class GLRenderer implements GLSurfaceView.Renderer {
	
	// Requested Rotation Type
	public enum Rotation { CLOCKWISE, COUNTER_CLOCKWISE, ONE_HUNDRED_EIGHTY };
	
	// Specify direction of arrow.
	public enum Direction { POSITIVE, NEGATIVE };

	// Main State Model
	private StateModel stateModel;
	
	// GL Objects that can be rendered
	private GLArrow arrowQuarterTurn;
	private GLArrow arrowHalfTurn;
	private GLCube  overlayGLCube;
    private GLCube  pilotGLCube;




	/**
	 * Constructor with global application stateModel
	 * 
	 * @param stateModel
	 */
	public GLRenderer(StateModel stateModel) {
		
		this.stateModel = stateModel;
		
		// Create the GL pilot cube
		pilotGLCube = new GLCube();
		
		// Create the GL overlay cube
		overlayGLCube = new GLCube();
		
		// Create two arrows: one half turn, one quarter turn.
		arrowQuarterTurn = new GLArrow(Amount.QUARTER_TURN);
		arrowHalfTurn = new GLArrow(Amount.HALF_TURN);
	}


	/**
	 * Call back when the surface is first created or re-created
	 * 
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition.khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	}


	/**
	 * Call back after onSurfaceCreated() or whenever the window's size changes
	 *  (non-Javadoc)
	 *  
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)
	 */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
	    
	       if (height == 0) height = 1;   // To prevent divide by zero

	        // Adjust the viewport based on geometry changes
	        // such as screen rotations
	        gl.glViewport(0, 0, width, height);

	        // make adjustments for screen ratio
//	        float ratio = (float) width / height;

	        gl.glMatrixMode(GL10.GL_PROJECTION);        // set matrix to projection mode
	        gl.glLoadIdentity();                        // reset the matrix to its default state
	        
	        gl.glMultMatrixf(stateModel.cameraParameters.getOpenGLProjectionMatrix(), 0);
	}

	

	/**
	 * On Draw Frame
	 * 
	 * Possibly Render:
	 *  1) An arrow to rotate the entire cube.
	 *  2) An arrow to rotate an edge of the cube.
	 *  3) An Overlay Cube (i.e., should be observed as exactly over the physical cube).
	 *  4) An Pilot Cube off to the right, at a fixed size and location, but with rotation of the physical cube.
	 * 
	 * @param gl
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)
	 */
	@Override
	public void onDrawFrame(GL10 gl) {

	    //      Log.e(Constants.TAG, "GL Thread ID = " + Thread.currentThread().getId());

	    // Clear color and depth buffers using clear-value set earlier
	    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

	    // Unless one of these conditions is true, we don't need to render anything.
	    if( (MenuAndParams.pilotCubeDisplay == false) &&
	            (MenuAndParams.cubeOverlayDisplay == false)  &&
	            (stateModel.appState != AppStateEnum.ROTATE) && 
	            (stateModel.appState != AppStateEnum.DO_MOVE) )
	        return;

	    // Make copy reference to Cube Reconstructor.
	    // This is to avoid asynchronous OpenGL and OpenCV problems. 
	    CubeReconstructor myCubeReconstructor = stateModel.cubeReconstructor;

	    // Check and if null don't render.
	    if(myCubeReconstructor == null)
	        return;

	    // Set GL_MODELVIEW transformation mode
	    gl.glMatrixMode(GL10.GL_MODELVIEW);
	    gl.glLoadIdentity();   // reset the matrix to its default state

	    // When using GL_MODELVIEW, you must set the view point
	    // Sets the location, direction, and orientation of camera, but not zoom
	    GLU.gluLookAt(gl,  
	            0,    0,    0,    // Camera Location
	            0f,   0f,  -1f,   // Camera points down Z axis.
	            0f, 1.0f, 0.0f);  // Specifies rotation of camera: in this case, standard upwards orientation.


	    // Render User Instruction Arrows and possibly Overlay Cube
	    if( (MenuAndParams.cubeOverlayDisplay == true)  ||
	            (stateModel.appState == AppStateEnum.ROTATE) || 
	            (stateModel.appState == AppStateEnum.DO_MOVE) ) {

	        // Save this Matrix State
	        gl.glPushMatrix();
	        
	        // Translate Cube per Pose Estimator
	        gl.glTranslatef(
	                myCubeReconstructor.x, 
	                myCubeReconstructor.y, 
	                myCubeReconstructor.z);

	        // Rotation Cube per Pose Estimator 
	        gl.glMultMatrixf(myCubeReconstructor.rotationMatrix, 0);

	        // Scale
	        // =+= I believe the need for this has something to do with the difference between camera and screen dimensions.
	        float scale = (float) MenuAndParams.scaleOffsetParam.value;
	        gl.glScalef(scale, scale, scale);

	        // If desire, render what we think is the cube location and orientation.
	        if(MenuAndParams.cubeOverlayDisplay == true)
	            overlayGLCube.draw(gl, true);

	        // Possibly Render either Entire Cube Rotation arrow or Cube Edge Rotation arrow.
	        switch(stateModel.appState) {

	        case ROTATE:
	            renderCubeFullRotationArrow(gl);
	            break;

	        case DO_MOVE:
	            renderCubeEdgeRotationArrow(gl);
	            break;

	        default:
	            break;
	        }
	        
	        // Return Matrix State
	        gl.glPopMatrix();
	    }

	    // Render Pilot Cube
	    if(MenuAndParams.pilotCubeDisplay == true && stateModel.renderPilotCube == true) {

	        // Instead of using pose esitmator coordinates, instead position cube at
	        // fix location.  We really just desire to observe rotation.
	        gl.glTranslatef(-4.0f, 0.0f, -10.0f);

	        // Rotation Cube per Pose Estimator 
	        gl.glMultMatrixf(myCubeReconstructor.rotationMatrix, 0);

	        pilotGLCube.draw(gl, false);
	    }
	}
	
    
    /**
	 * Render Cube Edge Rotation Arrow
	 * 
	 * Render an Rubik Cube Edge Rotation request/instruction.
	 * This is used after a solution has been computed and to instruct 
	 * the user to rotate one edge at a time.
	 * 
	 * @param gl
	 */
	private void renderCubeEdgeRotationArrow(GL10 gl) {
		
		String moveNumonic = stateModel.solutionResultsArray[stateModel.solutionResultIndex];

		Rotation rotation;
		Amount amount;
		
		if(moveNumonic.length() == 1)  {
			rotation = Rotation.CLOCKWISE;
			amount = Amount.QUARTER_TURN;
		}
		else if(moveNumonic.charAt(1) == '2') {
			rotation = Rotation.ONE_HUNDRED_EIGHTY;
			amount = Amount.HALF_TURN;
		}
		else if(moveNumonic.charAt(1) == '\'') {
			rotation = Rotation.COUNTER_CLOCKWISE;
			amount = Amount.QUARTER_TURN;
		}
		else
			throw new java.lang.Error("Unknow rotation amount");
		
		
		Scalar color = null;
		Direction direction = null;
		
		// Rotate and Translate Arrow as required by Rubik Logic Solution algorithm. 
		switch(moveNumonic.charAt(0)) {
		case 'U':
			color = stateModel.getFaceByName(FaceNameEnum.UP).observedTileArray[1][1].color;
			gl.glTranslatef(0.0f, +2.0f, 0.0f);
			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			break;
		case 'D':
			color = stateModel.getFaceByName(FaceNameEnum.DOWN).observedTileArray[1][1].color;		
			gl.glTranslatef(0.0f, -2.0f, 0.0f);
			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE; 
			break;
		case 'L':
			color = stateModel.getFaceByName(FaceNameEnum.LEFT).observedTileArray[1][1].color;
			gl.glTranslatef(-2.0f, 0.0f, 0.0f);
			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'R':
			color = stateModel.getFaceByName(FaceNameEnum.RIGHT).observedTileArray[1][1].color;
			gl.glTranslatef(+2.0f, 0.0f, 0.0f);
			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE;
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'F':
			color = stateModel.getFaceByName(FaceNameEnum.FRONT).observedTileArray[1][1].color;
			gl.glTranslatef(0.0f, 0.0f, +2.0f);
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'B':
			color = stateModel.getFaceByName(FaceNameEnum.BACK).observedTileArray[1][1].color;
			gl.glTranslatef(0.0f, 0.0f, -2.0f);
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		}

		// Specify direction of arrow
		if(direction == Direction.NEGATIVE)  {
			gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
			gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180
		}
		
		if(amount == Amount.QUARTER_TURN)
			arrowQuarterTurn.draw(gl, color);
		else
			arrowHalfTurn.draw(gl, color);
	}


	/**
	 * Render Cube Full Rotation Arrow
	 * 
	 * Render a Rubik Cube Body Rotation request/instruction.
	 * This is used during the exploration phase to observed all six
	 * sides of the cube before any solution is compute or attempted.
	 * 
	 * @param gl
	 */
	private void renderCubeFullRotationArrow(GL10 gl) {		
		
		// Render Front Face to Top Face Arrow Rotation
		if(stateModel.getNumObservedFaces() % 2 == 0) {
			gl.glTranslatef(0.0f, +1.5f, +1.5f);
			gl.glRotatef(-90f, 0.0f, 1.0f, 0.0f);  // Y rotation of -90
		}
		
		// Render Right Face to Top Face Arrow Rotation
		else {
			gl.glTranslatef(+1.5f, +1.5f, 0.0f);
		}
		
		
		// Reverse direction of arrow.
		gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
		gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180

		// Make Arrow Wider than normal by a factor of three.
		gl.glScalef(1.0f, 1.0f, 3.0f);
		
		// Render Quarter Turn Arrow
		arrowQuarterTurn.draw(gl, Constants.ColorWhite);
	}

}
