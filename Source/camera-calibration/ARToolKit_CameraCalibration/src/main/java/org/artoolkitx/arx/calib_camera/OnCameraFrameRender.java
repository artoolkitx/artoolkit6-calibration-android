package org.artoolkitx.arx.calib_camera;

import java.util.ArrayList;
import java.util.List;

import org.artoolkitx.arx.calib_camera.guide.CalibrationGuide;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

abstract class FrameRender {
    CameraCalibrator mCalibrator;

    public abstract Mat render(CvCameraViewFrame inputFrame);
}

class PreviewFrameRender extends FrameRender {
    @Override
    public Mat render(CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }
}

class CalibrationFrameRender extends FrameRender {
    private CalibrationGuide mGuide = null;

    public CalibrationFrameRender(CameraCalibrator calibrator) {
        mCalibrator = calibrator;
    }
    public void setCalibrationGuide(CalibrationGuide guide){ this.mGuide = guide;}

    @Override
    public Mat render(CvCameraViewFrame inputFrame) {
        Mat rgbaFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();
        mCalibrator.processFrame(grayFrame, rgbaFrame);
        if(mGuide != null) {
            mGuide.processFrame(rgbaFrame, mCalibrator.patternWasFound(), mCalibrator.getCorners(), mCalibrator);
        }

        return rgbaFrame;
    }
}

class UndistortionFrameRender extends FrameRender {
    public UndistortionFrameRender(CameraCalibrator calibrator) {
        mCalibrator = calibrator;
    }

    @Override
    public Mat render(CvCameraViewFrame inputFrame) {
        Mat renderedFrame = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
        Imgproc.undistort(inputFrame.rgba(), renderedFrame,
                mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());

        return renderedFrame;
    }
}

class ComparisonFrameRender extends FrameRender {
    private final int mWidth;
    private final int mHeight;
    public ComparisonFrameRender(CameraCalibrator calibrator, int width, int height) {
        mCalibrator = calibrator;
        mWidth = width;
        mHeight = height;
    }

    @Override
    public Mat render(CvCameraViewFrame inputFrame) {
        Mat undistortedFrame = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
        Imgproc.undistort(inputFrame.rgba(), undistortedFrame,
                mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());

        Mat comparisonFrame = inputFrame.rgba();
        undistortedFrame.colRange(new Range(0, mWidth / 2)).copyTo(comparisonFrame.colRange(new Range(mWidth / 2, mWidth)));
        List<MatOfPoint> border = new ArrayList<>();
        final int shift = (int)(mWidth * 0.005);
        border.add(new MatOfPoint(new Point(mWidth / 2 - shift, 0), new Point(mWidth / 2 + shift, 0),
                new Point(mWidth / 2 + shift, mHeight), new Point(mWidth / 2 - shift, mHeight)));
        Imgproc.fillPoly(comparisonFrame, border, new Scalar(251, 253, 254));

        return comparisonFrame;
    }
}

class OnCameraFrameRender {
    private final FrameRender mFrameRender;
    public OnCameraFrameRender(FrameRender frameRender) {
        mFrameRender = frameRender;
    }
    public Mat render(CvCameraViewFrame inputFrame) {
        return mFrameRender.render(inputFrame);
    }

    /**
     * Compares if the currently active frame renderer is the same a the one specified in the parameter
     * @param aClass the class to compare to
     * @return true if same class, false if not
     */
    public boolean instanceOfFrameRenderer(Class aClass){
        return aClass.equals(mFrameRender.getClass());
    }

    public void setCalibrationGuide(CalibrationGuide guide){
        if(mFrameRender instanceof CalibrationFrameRender){
            ((CalibrationFrameRender) mFrameRender).setCalibrationGuide(guide);
        }
        else{
            throw new RuntimeException("Calibration guide not supported for this kind of frame renderer");
        }
    }
}
