/**
 * 
 */
package org.ar.rubik;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.opengl.Matrix;

/**
 * @author android.steve@testlens.com
 * 
 * =+= Important Issue: viewing angle has a factor of 2 w.r.t. trig functions.
 *
 */
public class CameraCalibration {
	
	public double focalLengthPixels;
	
	// Android Camera Parameters
    private Parameters parameters;
    
    // Image size.  =+= Will this always be the same as onFrame() arg?
    private Size size;

    // Image width in pixels
    private int widthPixels;

    // Image height in pixels
    private int heightPixels;

    // Field of horizontal axis view in radians
    private float fovX;
    
    // Field of vertical axis view in radians
    private float fovY;

	/**
	 * Camera Parameters Constructor
	 */
	public CameraCalibration() {
		
		Camera camera = Camera.open();
		parameters = camera.getParameters();
		camera.release();
		
		size = parameters.getPictureSize();
		widthPixels = size.width;
		heightPixels = size.height;

		fovX = parameters.getVerticalViewAngle() * (float)(Math.PI / 180.0);
		fovY = parameters.getHorizontalViewAngle() * (float)(Math.PI / 180.0);

		// Make calculations across diagonal
		double diagonalPixels = Math.sqrt( widthPixels * widthPixels + heightPixels * heightPixels);
		double diagnoalFOV = Math.sqrt(fovX * fovX + fovY * fovY);
		
		// =+= Why?  The formula obtained from "Android Applications Programming with OpenCV" book.
		focalLengthPixels = diagonalPixels / ( 2.0 * Math.tan(0.5 * diagnoalFOV));
		
//		Log.e(Constants.TAG, "dPOV=" + diagnoalFOV + " dPx=" + diagonalPixels);
//		Log.e(Constants.TAG, "Camera Focal Length in Pixels Calibration: " + focalLengthPixels);
	}
	
	
	/**
	 * Get OpenCV Camera Matrix
	 * 
	 * =+= note, should elements 00 and 11 differ by aspect ratio?
	 * 
	 * =+= Also, use of Android Camera Size not working correct here.  hmm.
	 * 
	 * @return
	 */
	public Mat getOpenCVCameraMatrix () {
	    
//      Mat cameraMatrix          = new Mat(3, 3, CvType.CV_64FC1);
//      cameraMatrix.put(0, 0, focalLengthPixels);
//      cameraMatrix.put(0, 1, 0.0);
//      cameraMatrix.put(0, 2, widthPixels * 0.5f);
//      cameraMatrix.put(1, 0, 0.0);
//      cameraMatrix.put(1, 1, focalLengthPixels);
//      cameraMatrix.put(1, 2, heightPixels * 0.5f);
//      cameraMatrix.put(2, 0, 0.0);
//      cameraMatrix.put(2, 1, 0.0);
//      cameraMatrix.put(2, 2, 1.0);
      
      
	    Mat cameraMatrix          = new Mat(3, 3, CvType.CV_64FC1);
	    cameraMatrix.put(0, 0, focalLengthPixels);
	    cameraMatrix.put(0, 1, 0.0);
	    cameraMatrix.put(0, 2, 1280.0/2.0);
	    cameraMatrix.put(1, 0, 0.0);
	    cameraMatrix.put(1, 1, focalLengthPixels);
	    cameraMatrix.put(1, 2, 720.0/2.0);
	    cameraMatrix.put(2, 0, 0.0);
	    cameraMatrix.put(2, 1, 0.0);
	    cameraMatrix.put(2, 2, 1.0);
      
      return cameraMatrix;
	}


	
	/**
	 * Get OpenGL Projection Matrix
	 * 
	 * This is derived from the Android Camera Parameters.
	 * 
	 * @return
	 */
	public float[] getOpenGLProjectionMatrix() {

	    // =+= How should these numbers be determined?  In particular, near is used in calculations below.
	    // =+= Seems that that number should be the "image z axis".
	    float near = 1.0f;
	    float far  = 100.0f;
	    
	    float top =   (float)Math.tan(fovX * 0.5f) * near;
	    float right = (float)Math.tan(fovY * 0.5f) * near;

	    float [] glProjectionMatrix = new float[16];
	    
        Matrix.frustumM( glProjectionMatrix, 0, -right, right, -top, top, near, far);

        return glProjectionMatrix;
	}
}
