package org.artoolkitx.arx.calib_camera;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.artoolkitx.arx.calib_camera.utils.RotationConverters;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Size;

import android.util.Log;

@SuppressWarnings("ALL")
public class CameraCalibrator implements Serializable{
    private static final String TAG = "CameraCalibrator";

    private final Size mPatternSize = new Size(4, 11);
    private final int mCornersSize = (int)(mPatternSize.width * mPatternSize.height);

    private boolean mPatternWasFound = false;

    private MatOfPoint2f mCorners = new MatOfPoint2f();
    private List<Mat> mCornersBuffer = new ArrayList<>();
    private boolean mIsCalibrated = false;

    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    private int mFlags;
    private double mRms;
    private double mSquareSize = 0.0181;
    private Size mImageSize;
    private ArrayList<Mat> mObjectPoints = new ArrayList<>();
    private Mat mReprojectionErrors;

    public CameraCalibrator(int width, int height) {
        mImageSize = new Size(width, height);
        mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
                 Calib3d.CALIB_ZERO_TANGENT_DIST +
                 Calib3d.CALIB_FIX_ASPECT_RATIO +
                 Calib3d.CALIB_FIX_K4 +
                 Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
        Log.i(TAG, "Instantiated new " + this.getClass());
        mReprojectionErrors = new Mat();
    }


    public void processFrame(Mat grayFrame, Mat rgbaFrame) {
        findPattern(grayFrame);
        renderFrame(rgbaFrame);
    }

    public void calibrate() {
        ArrayList<Mat> rvecs = new ArrayList<>();
        ArrayList<Mat> tvecs = new ArrayList<>();

        ArrayList<Mat> objectPoints = new ArrayList<>();
        objectPoints.add(Mat.zeros(mCornersSize, 1, CvType.CV_32FC3));
        calcBoardCornerPositions(objectPoints.get(0));
        for (int i = 1; i < mCornersBuffer.size(); i++) {
            objectPoints.add(objectPoints.get(0));
        }

        Calib3d.calibrateCamera(objectPoints, mCornersBuffer, mImageSize,
                mCameraMatrix, mDistortionCoefficients, rvecs, tvecs, mFlags);

        mIsCalibrated = Core.checkRange(mCameraMatrix)
                && Core.checkRange(mDistortionCoefficients);

        mRms = computeReprojectionErrors(objectPoints, rvecs, tvecs, mReprojectionErrors);
        Log.i(TAG, String.format("Average re-projection error: %f", mRms));
        Log.i(TAG, "Camera matrix: " + mCameraMatrix.dump());
        Log.i(TAG, "Distortion coefficients: " + mDistortionCoefficients.dump());
        mObjectPoints = objectPoints;
    }

    public void clearCorners() {
        mCornersBuffer.clear();
    }

    private void calcBoardCornerPositions(Mat corners) {
        final int cn = 3;
        float positions[] = new float[mCornersSize * cn];

        for (int i = 0; i < mPatternSize.height; i++) {
            for (int j = 0; j < mPatternSize.width * cn; j += cn) {
                positions[(int) (i * mPatternSize.width * cn + j + 0)] =
                        (2 * (j / cn) + i % 2) * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 1)] =
                        i * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 2)] = 0;
            }
        }
        corners.create(mCornersSize, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }

    private double computeReprojectionErrors(List<Mat> objectPoints,
            List<Mat> rvecs, List<Mat> tvecs, Mat perViewErrors) {
        MatOfPoint2f cornersProjected = new MatOfPoint2f();
        double totalError = 0;
        double error;
        float viewErrors[] = new float[objectPoints.size()];

        MatOfDouble distortionCoefficients = new MatOfDouble(mDistortionCoefficients);
        int totalPoints = 0;
        for (int i = 0; i < objectPoints.size(); i++) {
            MatOfPoint3f points = new MatOfPoint3f(objectPoints.get(i));
            Calib3d.projectPoints(points, rvecs.get(i), tvecs.get(i),
                    mCameraMatrix, distortionCoefficients, cornersProjected);
            error = Core.norm(mCornersBuffer.get(i), cornersProjected, Core.NORM_L2);

            int n = objectPoints.get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError  += error * error;
            totalPoints += n;
        }
        perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    private void findPattern(Mat grayFrame) {
        mPatternWasFound = Calib3d.findCirclesGrid(grayFrame, mPatternSize,
                mCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        if(mPatternWasFound)
            Log.i(TAG,"Corners: "+ mCorners.toArray());
    }

    public void addCorners() {
        if (mPatternWasFound) {
            mCornersBuffer.add(mCorners.clone());
        }
    }

    private void drawPoints(Mat rgbaFrame) {
        Calib3d.drawChessboardCorners(rgbaFrame, mPatternSize, mCorners, mPatternWasFound);
    }

    private void renderFrame(Mat rgbaFrame) {
        drawPoints(rgbaFrame);
    }

    public Mat getCameraMatrix() {
        return mCameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return mDistortionCoefficients;
    }

    public int getCornersBufferSize() {
        return mCornersBuffer.size();
    }

    public double getAvgReprojectionError() {
        return mRms;
    }

    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    public void setCalibrated() {
        mIsCalibrated = true;
    }

    public boolean checkLastFrame()
    {
        boolean isFrameBad = false;

        //check if we already have a previous frame
        if(mCornersBuffer.size() > 0) {

            Mat tmpCamMatrix = new Mat();
            double badAngleThresh = 40;

            if (mCameraMatrix.total() > 0) {
                tmpCamMatrix = Mat.eye(3, 3, CvType.CV_64F);
                tmpCamMatrix.put(0, 0, 20000);
                tmpCamMatrix.put(1, 1, 20000);
                tmpCamMatrix.put(0, 2, mImageSize.height / 2);
                tmpCamMatrix.put(1, 2, mImageSize.width / 2);
            } else {
                mCameraMatrix.copyTo(tmpCamMatrix);
            }

            Mat r, t, angles;
            r = new Mat();
            t = new Mat();
            angles = new MatOfPoint2f();

            MatOfPoint3f previousObjectPoints = new MatOfPoint3f(mObjectPoints.get(mObjectPoints.size() -1));

            //current points are located in mCorners
            MatOfPoint2f currentPoints = new MatOfPoint2f(mCorners);

            MatOfDouble distortionCoefficients = new MatOfDouble(mDistortionCoefficients);

            Calib3d.solvePnP(previousObjectPoints, currentPoints, tmpCamMatrix, distortionCoefficients, r, t);
            angles = RotationConverters.rodriguesToEuler(r, RotationConverters.CALIB_DEGREES);

            if (angles != null) {
                if (Math.abs(angles.get(0, 0)[0]) > badAngleThresh || Math.abs(angles.get(1, 0)[0]) > badAngleThresh) {
                    //            mCalibData->objectPoints.pop_back();
                    //            mCalibData->imagePoints.pop_back();
                    //mCornersBuffer.remove(currentPoints);
                    isFrameBad = true;
                }
            } else {
                //Somehow the frame evaluation failed. Just take the frame and continue.
                Log.e(TAG, "Frame evaluation failed");
            }
        }
        return isFrameBad;
    }

    public final Mat getReprojectionErrors(){
        return mReprojectionErrors;
    }

    public void removeFrame(Mat itemToRemove) {
        mCornersBuffer.remove(itemToRemove);
    }

    public Mat getFrame(int index) {
        return mCornersBuffer.get(index);
    }

    public boolean patternWasFound() {
        return mPatternWasFound;
    }

    public final MatOfPoint2f getCorners() {
        return mCorners;
    }

    public ArrayList<Float> getReprojectionErrorArrayList(){
        ArrayList<Float> reprojectionArray = new ArrayList<>();

        for(int i = 0; i <= this.getCornersBufferSize()-1;i++){
            reprojectionArray.add((float) mReprojectionErrors.get(i,0)[0]);
        }
        return reprojectionArray;
    }
}
