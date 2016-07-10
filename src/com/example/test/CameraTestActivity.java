package com.example.test;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class CameraTestActivity extends Activity implements CvCameraViewListener {

	private int iNumberOfCameras = 0;
	private long lFrameCount = 0, lMilliStart = 0, lMilliNow = 0;
	private MatOfFloat mMOFerr;
    private MatOfByte mMOBStatus;
    private MatOfPoint2f  mMOP2fptsPrev, mMOP2fptsThis, mMOP2fptsSafe;
    private MatOfPoint MOPcorners;
    private Point pt, pt2;
	private JavaCameraView mOpenCvCameraView0;
	private JavaCameraView mOpenCvCameraView1;
	private Mat mRgba, matOpFlowPrev, matOpFlowThis;
	private double dTextScaleFactor;
	private List<Byte> byteStatus;
    private List<Point> cornersThis, cornersPrev;
    private int x, y;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				mOpenCvCameraView0.enableView();

				if (iNumberOfCameras > 1)
					mOpenCvCameraView1.enableView();

			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};
	Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		iNumberOfCameras = Camera.getNumberOfCameras();

		// Log.d(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		setContentView(R.layout.activity_camera_test);

		context = this;
		mOpenCvCameraView0 = (JavaCameraView) findViewById(R.id.java_surface_view0);

		if (iNumberOfCameras > 1)
			mOpenCvCameraView1 = (JavaCameraView) findViewById(R.id.java_surface_view1);

		mOpenCvCameraView0.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView0.setCvCameraViewListener(this);

		mOpenCvCameraView0.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		if (iNumberOfCameras > 1) {
			mOpenCvCameraView1.setVisibility(SurfaceView.GONE);
			mOpenCvCameraView1.setCvCameraViewListener(this);
			mOpenCvCameraView1.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView0 != null)
			mOpenCvCameraView0.disableView();
		if (iNumberOfCameras > 1)
			if (mOpenCvCameraView1 != null)
				mOpenCvCameraView1.disableView();
	}

	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView0 != null)
			mOpenCvCameraView0.disableView();
		if (iNumberOfCameras > 1)
			if (mOpenCvCameraView1 != null)
				mOpenCvCameraView1.disableView();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return true;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		byteStatus = new ArrayList<Byte>();
		cornersThis = new ArrayList<Point>();
        cornersPrev = new ArrayList<Point>();
        mMOP2fptsPrev = new MatOfPoint2f();
        mMOP2fptsThis = new MatOfPoint2f();
        mMOP2fptsSafe = new MatOfPoint2f();
        mMOFerr = new MatOfFloat();
        mMOBStatus = new MatOfByte();
        MOPcorners = new MatOfPoint();
        mRgba = new Mat();
        matOpFlowThis = new Mat();
        matOpFlowPrev = new Mat();

        pt = new Point(0, 0);
        pt2 = new Point(0, 0);

		DisplayMetrics dm = this.getResources().getDisplayMetrics();
		int densityDpi = dm.densityDpi;
		dTextScaleFactor = ((double) densityDpi / 240.0) * 0.9;

		mRgba = new Mat(height, width, CvType.CV_8UC4);
	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
        MOPcorners.release();
	}

	@Override
	public Mat onCameraFrame(Mat inputFrame) {

		if (lMilliStart == 0)
			lMilliStart = System.currentTimeMillis();

		if ((lMilliNow - lMilliStart) > 10000) {
			lMilliStart = System.currentTimeMillis();
			lFrameCount = 0;
		}

		lMilliNow = System.currentTimeMillis();
		lFrameCount++;
		inputFrame.copyTo(mRgba);
		 if (mMOP2fptsPrev.rows() == 0) {

             //Log.d("Baz", "First time opflow");
             // first time through the loop so we need prev and this mats
             // plus prev points
             // get this mat
             Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

             // copy that to prev mat
             matOpFlowThis.copyTo(matOpFlowPrev);

             // get prev corners
             Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, 40, 0.05, 20);
             mMOP2fptsPrev.fromArray(MOPcorners.toArray());

             // get safe copy of this corners
             mMOP2fptsPrev.copyTo(mMOP2fptsSafe);
         } else {
             //Log.d("Baz", "Opflow");
             // we've been through before so
             // this mat is valid. Copy it to prev mat
             matOpFlowThis.copyTo(matOpFlowPrev);

             // get this mat
             Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

             // get the corners for this mat
             Imgproc.goodFeaturesToTrack(matOpFlowThis, MOPcorners, 40, 0.05, 20);
             mMOP2fptsThis.fromArray(MOPcorners.toArray());

             // retrieve the corners from the prev mat
             // (saves calculating them again)
             mMOP2fptsSafe.copyTo(mMOP2fptsPrev);

             // and save this corners for next time through

             mMOP2fptsThis.copyTo(mMOP2fptsSafe);
         }


    	/*
        Parameters:
    		prevImg first 8-bit input image
    		nextImg second input image
    		prevPts vector of 2D points for which the flow needs to be found; point coordinates must be single-precision floating-point numbers.
    		nextPts output vector of 2D points (with single-precision floating-point coordinates) containing the calculated new positions of input features in the second image; when OPTFLOW_USE_INITIAL_FLOW flag is passed, the vector must have the same size as in the input.
    		status output status vector (of unsigned chars); each element of the vector is set to 1 if the flow for the corresponding features has been found, otherwise, it is set to 0.
    		err output vector of errors; each element of the vector is set to an error for the corresponding feature, type of the error measure can be set in flags parameter; if the flow wasn't found then the error is not defined (use the status parameter to find such cases).
     */
         Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev, mMOP2fptsThis, mMOBStatus, mMOFerr);

         cornersPrev = mMOP2fptsPrev.toList();
         cornersThis = mMOP2fptsThis.toList();
         byteStatus = mMOBStatus.toList();

         y = byteStatus.size() - 1;

         Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGBA2GRAY);
         for (x = 0; x < y; x++) {
             if (byteStatus.get(x) == 1) {
                 pt = cornersThis.get(x);
                 pt2 = cornersPrev.get(x);
                 Imgproc.circle(mRgba, pt, 5, new Scalar(255,0,0), 1);
                 Imgproc.line(mRgba, pt, pt2, new Scalar(255,0,0), 2);
             }
         }
 

		String string = String.format("FPS: %2.1f", (float) (lFrameCount * 1000) / (float) (lMilliNow - lMilliStart));

        ShowTitle(string, 2, new Scalar(255,0,0));
        
		return mRgba;
	}

	private void ShowTitle(String s, int iLineNum, Scalar color) {

		Imgproc.putText(mRgba, s, new Point(10, (int) (dTextScaleFactor * 60 * iLineNum)), Core.FONT_HERSHEY_SIMPLEX,
				dTextScaleFactor, color, 2);
	}

	String TAG = "OpencvActivity";

}