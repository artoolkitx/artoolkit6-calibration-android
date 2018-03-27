// This sample is based on "Camera calibration With OpenCV" tutorial:
// http://docs.opencv.org/doc/tutorials/calib3d/camera_calibration/camera_calibration.html
//
// It uses standard OpenCV asymmetric circles grid pattern 11x4:
// https://github.com/Itseez/opencv/blob/2.4/doc/acircles_pattern.png.
// The results are the camera matrix and 5 distortion coefficients.
//
// Tap on highlighted pattern to capture pattern corners for calibration.
// Move pattern along the whole screen and capture data.
//
// When you've captured necessary amount of pattern corners (usually ~20 are enough),
// press "Calibrate" button for performing camera calibration.

package org.artoolkitx.arx.calib_camera;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.artoolkitx.arx.calib_camera.guide.CalibrationGuide;
import org.artoolkitx.arx.calib_camera.guide.CalibrationGuideListener;
import org.artoolkitx.arx.calib_camera.menu.MenuArrayAdapter;
import org.artoolkitx.arx.calib_camera.share.ShareActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.StaticHelper;
import org.opencv.core.Mat;

import java.io.File;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class CameraCalibrationActivity extends Activity implements CvCameraViewListener2,
        View.OnClickListener, Animation.AnimationListener, AdapterView.OnItemClickListener, CalibrationGuideListener {
    public static final int COMPARE_MENU_POS = 0;
    public static final int UNDISTORETED_VIDEO_POS = 1;
    public static final int NEW_CALIBRATION = 2;
    public static final int SHARE_POS = 3;
//    public static final int PRINT_POS = 4;
    public static final int SETTINGS_MENU_POS = 5;
    public static final int HELP_POS = 6;
    public static final int CALIB_MESSAGE_POS = 7;
    public static final int CALIB_STATS = 4;

    private static final String TAG = "OCVSample::Activity";
    private static final String ANDROID_CAMERA_CALIBRATION_HELP_URL = "https://github.com/artoolkit/artoolkit6/wiki/Camera-calibration-Android";
    private static final int CALIBRATION_DETAIL_REQ_CODE = 1;
    public static final String INTENT_EXTRA_CAMERA_CALIBRATOR = "Calibrator";
    public static boolean GUIDE_MODE = false;

    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("calibration_upload_native");
    }

    private CameraBridgeViewBase mOpenCvCameraView;
    private CameraCalibrator mCalibrator;
    private OnCameraFrameRender mOnCameraFrameRender;
    private int mWidth;
    private int mHeight;
    private ImageButton mStartCalibrationButton;
    private DrawerLayout mDrawerLayout;
    private ImageButton mMenuButton;
    private TextView mGuidingText;
    private Animation mFadeInAnimation;
    private MenuArrayAdapter menuArrayAdapter;
    private Animation mFadeOutAnimation;
    private ViewGroup mCompareVideoView;
    private SharedPreferences mPrefs;
    private ProgressBar mProgress;
    private ImageButton mGuideButton;
    private ProgressBar mUploadStatus;
    private PopupWindow mPopupWindow;
    private TextView mTextUploadStatus;
    private ImageView mButtonUploadError;

    public CameraCalibrationActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public static native void nativeSaveParam(double[] cameraMatrix, double[] distortionCoefficientsArray,
                                              int sizeX, int sizeY, float average, float min, float max);

    public static native boolean nativeInitialize(Context ctx,String calibrationServerURL, int cameraId, boolean isFrontFacing, String hashedToken);

    public static native void nativeStop();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        //Load the OpenCV libs native libs
        if (!StaticHelper.initOpenCV(false)) {
            Log.d(TAG, "Internal OpenCV library not found. This should not happen!");
            finish();
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            Log.i(TAG, "OpenCV loaded successfully");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.camera_calibration_surface_view);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_calibration_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnClickListener(this);

        mStartCalibrationButton = (ImageButton) findViewById(R.id.button_startCalibration);
        mStartCalibrationButton.setOnClickListener(this);

        mMenuButton = (ImageButton) findViewById(R.id.button_menu);
        mMenuButton.setOnClickListener(this);

        mGuideButton = (ImageButton) findViewById(R.id.button_guideMode);
        mGuideButton.setOnClickListener(this);

        //Create slide in menu
        String[] menuEntries = getResources().getStringArray(R.array.menuItems);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ListView drawerList = (ListView) findViewById(R.id.menuList);

        // Set the adapter for the list view
        menuArrayAdapter = new MenuArrayAdapter(this, new ArrayList<>(Arrays.asList(menuEntries)));
        drawerList.setAdapter(menuArrayAdapter);
        drawerList.setOnItemClickListener(this);

        //Get guiding textView
        mGuidingText = (TextView) findViewById(R.id.text_guiding);

        //Animation for 'start calibration button'
        mFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mFadeInAnimation.setAnimationListener(this);
        mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        mFadeOutAnimation.setAnimationListener(this);

        //Get handle to compare view
        mCompareVideoView = (ViewGroup) findViewById(R.id.view_compare);

        //Get handle to guide view progress and upload status
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        mUploadStatus = (ProgressBar) findViewById(R.id.uploadStatusBar);
        mUploadStatus.setOnClickListener(this);

        //Get all handles for the uploadStatus feature
        View uploadStatusViewLayout = getLayoutInflater().inflate(R.layout.upload_status_layout,null);
        mTextUploadStatus = (TextView) uploadStatusViewLayout.findViewById(R.id.text_uploadStatus);

        mPopupWindow = new PopupWindow(uploadStatusViewLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mButtonUploadError = (ImageView) findViewById(R.id.button_uploadError);
        mButtonUploadError.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onResume() {
        super.onResume();

        mOpenCvCameraView.enableView();
        int cameraId = Integer.parseInt(mPrefs.getString(CalibCameraPreferences.PREF_CAMERA_INDEX, this.getString(R.string.pref_defaultValue_cameraIndex)));


        mOpenCvCameraView.setCameraIndex(cameraId);

        //TODO: Check implication on phones running API level 15 to 19
        mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        boolean shareWithArtkCommunity = mPrefs.getBoolean(CalibCameraPreferences.PREF_CALIBRATION_SERVER_SHARE_WITH_ARTK,Boolean.parseBoolean(this.getString(R.string.pref_calibrationSendToARK_default)));

        String cameraCalibrationServer = "";
        String hashedToken = "";
        String token = "";
        if(shareWithArtkCommunity) {
            cameraCalibrationServer = this.getString(R.string.pref_calibrationServerARTK);
            token = this.getString(R.string.pref_calibrationServerTokenARTK);
            hashedToken = md5(token);
        }else{
            cameraCalibrationServer = mPrefs.getString(CalibCameraPreferences.PREF_CALIBRATION_SERVER,this.getString(R.string.pref_calibrationServerDefault));
            token = mPrefs.getString(CalibCameraPreferences.PREF_CALIBRATION_SERVER_TOKEN,this.getString(R.string.pref_calibrationServerTokenDefault));
            hashedToken = md5(token);
        }

        boolean frontFacing = false;

        CameraManager manager = (CameraManager) mOpenCvCameraView.getContext().getSystemService(Context.CAMERA_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId + "");
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontFacing = true;
                }
            } catch (Exception e) {

            }
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) frontFacing = true;
        }
        else{
            Log.e(TAG, "We are running on an Android version that we don't support. That is very weird!");
        }

        if (!CameraCalibrationActivity.nativeInitialize(this,cameraCalibrationServer,cameraId,frontFacing,hashedToken)) {
            Log.e(TAG, "Native initialize failed. This will cause the calibration upload to fail");
        }

        mButtonUploadError.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                nativeStop();
                return null;
            }
        };

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private void startCalibration(final CameraCalibrator calibrator) {
        final Resources res = getResources();

        mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog calibrationProgress;

            @Override
            protected void onPreExecute() {
                calibrationProgress = new ProgressDialog(CameraCalibrationActivity.this);
                calibrationProgress.setTitle(res.getString(R.string.calibrating));
                calibrationProgress.setMessage(res.getString(R.string.please_wait));
                calibrationProgress.setCancelable(false);
                calibrationProgress.setIndeterminate(true);
                calibrationProgress.show();
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                calibrator.calibrate();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                calibrationProgress.dismiss();
                String resultMessage = (calibrator.isCalibrated()) ?
                        res.getString(R.string.calibration_successful) + " " + calibrator.getAvgReprojectionError() :
                        res.getString(R.string.calibration_unsuccessful);
                (Toast.makeText(CameraCalibrationActivity.this, resultMessage, Toast.LENGTH_SHORT)).show();

                //Only have one previous calibration message in the list
                if (menuArrayAdapter.getCount() > CALIB_MESSAGE_POS) {
                    menuArrayAdapter.remove(menuArrayAdapter.getItem(CALIB_MESSAGE_POS));
                }
                menuArrayAdapter.insert(resultMessage, CALIB_MESSAGE_POS);
                menuArrayAdapter.notifyDataSetChanged();

                if (calibrator.isCalibrated()) {
                    CalibrationResult.save(CameraCalibrationActivity.this,
                            calibrator.getCameraMatrix(), calibrator.getDistortionCoefficients(), mWidth, mHeight);
                    uploadCalibration();
                    mGuidingText.setText(R.string.text_calibrationFinished);
                }
                mCalibrator = calibrator;
            }
        }.execute();
    }

    private void uploadCalibration() {

        Mat cameraMatrix = mCalibrator.getCameraMatrix();
        Mat distortionCoefficients = mCalibrator.getDistortionCoefficients();

        double[] cameraMatrixArray = new double[CalibrationResult.CAMERA_MATRIX_ROWS * CalibrationResult.CAMERA_MATRIX_COLS];
        cameraMatrix.get(0, 0, cameraMatrixArray);

        double[] distortionCoefficientsArray = new double[CalibrationResult.DISTORTION_COEFFICIENTS_SIZE];
        distortionCoefficients.get(0, 0, distortionCoefficientsArray);

        ArrayList<Float> reprojectionErrorArrayList = mCalibrator.getReprojectionErrorArrayList();
        float average = 0;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int i = 0; i <= reprojectionErrorArrayList.size() -1; i++) {
            // turn your data into Entry objects
            average +=(float) reprojectionErrorArrayList.get(i);
            if(reprojectionErrorArrayList.get(i) < min){
                min = reprojectionErrorArrayList.get(i);
            }
            if(reprojectionErrorArrayList.get(i) > max){
                max = reprojectionErrorArrayList.get(i);
            }
        }
        average /= reprojectionErrorArrayList.size();

        CameraCalibrationActivity.nativeSaveParam(cameraMatrixArray, distortionCoefficientsArray, mWidth, mHeight,average, min, max);
    }

    public void onCameraViewStarted(int width, int height) {
        mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            mGuideButton.setBackgroundResource(R.drawable.hexagon);

            if (CalibrationResult.tryLoad(this, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients(), mWidth, mHeight)) {
                mCalibrator.setCalibrated();
                mGuidingText.setText(R.string.guidingText_Preloaded);
            }
            mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
        }
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return mOnCameraFrameRender.render(inputFrame);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick invoked on View: " + v);
        if (v.equals(mStartCalibrationButton)) {
            String picsTaken = getResources().getQuantityString(R.plurals.numberOfPicturesTaken, 0, 0);
            mGuidingText.setText(picsTaken);
            mStartCalibrationButton.startAnimation(mFadeOutAnimation);
            startCalibration(mCalibrator);
        } else if (v.equals(mMenuButton)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        } else if (v.equals(mGuideButton)) {
            guideButtonLogic();
        } else if(v.equals(mUploadStatus)){
            if(mPopupWindow.isShowing())
                mPopupWindow.dismiss();
            else
                mPopupWindow.showAsDropDown(this.mGuideButton,50,30);
        } else if(v.equals(mButtonUploadError)){
            if(mPopupWindow.isShowing())
                mPopupWindow.dismiss();
            else
                mPopupWindow.showAsDropDown(this.mGuideButton,50,30);
        }
        else {
            if (mOnCameraFrameRender.instanceOfFrameRenderer(CalibrationFrameRender.class) && !GUIDE_MODE) {
                mCalibrator.addCorners();
                this.pictureAdded(mCalibrator.getCornersBufferSize());
            }
        }
    }

    private void guideButtonLogic() {
        mCalibrator.clearCorners();
        if (!GUIDE_MODE) {
            GUIDE_MODE = true;
            mProgress.setVisibility(View.VISIBLE);
            mGuidingText.setText(R.string.guidemode_intro_text);

            CalibrationGuide guide = new CalibrationGuide(mWidth, mHeight, this);
            guide.registerCalibrationGuideListener(this);
            mOnCameraFrameRender.setCalibrationGuide(guide);
            mGuideButton.setBackgroundResource(R.drawable.hexagon_gray);
        } else {
            GUIDE_MODE = false;
            mProgress.setVisibility(View.INVISIBLE);
            final String picsTaken = getResources().getQuantityString(R.plurals.numberOfPicturesTaken,
                    mCalibrator.getCornersBufferSize(), mCalibrator.getCornersBufferSize());
            mGuidingText.setText(picsTaken);
            mOnCameraFrameRender.setCalibrationGuide(null);
            mGuideButton.setBackgroundResource(R.drawable.hexagon);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation.equals(mFadeInAnimation))
            mStartCalibrationButton.setVisibility(View.VISIBLE);
        else if (animation.equals(mFadeOutAnimation))
            mStartCalibrationButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    @Override
    //Handles the clicks on the menu
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Context context = parent.getContext();
        mCompareVideoView.setVisibility(View.INVISIBLE);
        mGuidingText.setVisibility(View.VISIBLE);
        if (position == SETTINGS_MENU_POS) {
            context.startActivity(new Intent(context, CalibCameraPreferences.class));
        } else if (position == COMPARE_MENU_POS && checkIfCalibrationIsAvailable()) {
            mGuideButton.setVisibility(View.INVISIBLE);
            mCompareVideoView.setVisibility(View.VISIBLE);
            mOnCameraFrameRender =
                    new OnCameraFrameRender(new ComparisonFrameRender(mCalibrator, mWidth, mHeight));
            mGuidingText.setVisibility(View.INVISIBLE);
        } else if (position == UNDISTORETED_VIDEO_POS && checkIfCalibrationIsAvailable()) {
            mGuideButton.setVisibility(View.INVISIBLE);
            mOnCameraFrameRender =
                    new OnCameraFrameRender(new UndistortionFrameRender(mCalibrator));
            mGuidingText.setText(R.string.undistorted);
        } else if (position == NEW_CALIBRATION) {
            mGuideButton.setVisibility(View.VISIBLE);
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            mOnCameraFrameRender =
                    new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
            mGuidingText.setText(R.string.guidingText_Start);
        } else if (position == SHARE_POS) {
            File calibsFile = new File(this.getCacheDir().getAbsolutePath() + "/calibs");
            Log.d(TAG, "calibsFile: " + calibsFile.toString());
            File[] fileArray = calibsFile.listFiles();

            if (fileArray != null && (Array.getLength(fileArray) != 0)) {
                Intent share = new Intent(this, ShareActivity.class);
                startActivity(share);
            } else {
                Toast toast = Toast.makeText(this, R.string.error_no_calibration, Toast.LENGTH_LONG);
                toast.show();
            }
        } else if (position == HELP_POS) {
            Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(ANDROID_CAMERA_CALIBRATION_HELP_URL));
            startActivity(browse);
        } else if (position == CALIB_STATS && checkIfCalibrationIsAvailable()) {
            Intent openStatistics = new Intent(this,CalibrationDetails.class);
            //openStatistics.putExtra("Calibration_reprojectionArray",reprojectionArray);
            openStatistics.putExtra(INTENT_EXTRA_CAMERA_CALIBRATOR,mCalibrator);
            startActivityForResult(openStatistics, CALIBRATION_DETAIL_REQ_CODE);
        }
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private boolean checkIfCalibrationIsAvailable() {

        if (mCalibrator.isCalibrated()) {
            return true;
        } else {
            (Toast.makeText(this, getResources().getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
        }
        return false;
    }

    public void pictureAdded(final int cornerBufferListSize) {
        final String picsTaken = getResources().getQuantityString(R.plurals.numberOfPicturesTaken, cornerBufferListSize, cornerBufferListSize);

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGuidingText.setText(picsTaken);
                //Make start calibration button visible
                if (cornerBufferListSize == 5 && !GUIDE_MODE) {
                    mStartCalibrationButton.startAnimation(mFadeInAnimation);
                }
            }
        });

    }

    @Override
    public void calibrationGuideFinish() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startCalibration(mCalibrator);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CALIBRATION_DETAIL_REQ_CODE){
            if(resultCode == RESULT_OK){
                CameraCalibrator cameraCalibrator = (CameraCalibrator) data.getSerializableExtra(CameraCalibrationActivity.INTENT_EXTRA_CAMERA_CALIBRATOR);
                if(cameraCalibrator.getCornersBufferSize() > 1) {
                    startCalibration(cameraCalibrator);
                }
            }
        }
    }

    private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    //Called from native
    public void setUploadStatusText(final String status){
        this.runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   mUploadStatus.setVisibility(View.VISIBLE);
                                   mTextUploadStatus.setText(status);
                               }
                           });
    }

    public void addUploadStatusText(final String status){
        this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextUploadStatus.append("\n" + status);
                                }});
    }

    public void uploadFinished(final boolean success){
        this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mUploadStatus.setVisibility(View.INVISIBLE);
                                    mButtonUploadError.setVisibility(success?View.INVISIBLE:View.VISIBLE);
                                }
        });
    }
    //End called from native
}
