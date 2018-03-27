package org.artoolkitx.arx.calib_camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.artoolkitx.arx.calib_camera.utils.ImageButtonDisable;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/**
 * This class shows a chart with the details of the camera calibration
 * The chart is a bar chart listing each frame horizontally and the reprojectionError vertically
 *
 MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
 MM~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~MMMMMMMM
 MM~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~MMMMMMMM
 MM~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~MMMMMMMM
 MM~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~MMMMMMMM
 MM~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~MMMOMMMM
 MM~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~MMMMMMMM
 MN~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~MMMMMMMM
 MN~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~MMMMMMMM
 NN~:::::::~~~~~~~~~~~~~~~~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~MMMMMMMM
 NN~:::::::~~~~~~~~~~~~~~~~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~NNMMMMMM
 NN~:::::::~~~~~~~~:::::::~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~NNMMMMMM
 NN~:::::::~~~~~~~~:::::::~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~NNMMMMMM
 NN~:::::::~~~~~~~~:::::::~::::::~~~~~~~~~~~~~~~~~~~~~~~~::::::~~~~~~~~~~NNMMMNMM
 NN~:::::::~~~~~~~~:::::::~::::::~:::::::~~~~~~~~~~~~~~~~::::::~~~~~~~~~~NNMMMNMM
 NN~:::::::~::::::~:::::::~::::::~:::::::~~~~~~~~~~~~~~~~::::::~~~~~~~~~~NNMMMMMM
 NN~:::::::~::::::~:::::::~::::::~:::::::~~~~~~~~~~~~~~~~::::::~~~~~~~~~~NNMMMMMM
 NN~:::::::~::::::~:::::::~::::::~:::::::~~~~~~~~:::::::~::::::~~~~~~~~~~NNMMMMMM
 NN~:::::::~::::::~:::::::~::::::~:::::::~::::::~:::::::~::::::~~~~~~~~~~NDMMMMMM
 NN~:::::::~::::::~:::::::~::::::~:::::::~::::::~:::::::~::::::~~~~~~~~~~NDMMMMMM
 DD~:::::::~::::::~:::::::~::::::~:::::::~::::::~:::::::~::::::~~~~~~~~~~DDMMMMMM
 DD~:::::::~::::::~:::::::~::::::~:::::::~::::::~:::::::~::::::~~~~~~~~~~DDMMMMMM
 DD~:::::::~::::::~:::::::~::::::~:::::::~::::::~:::::::~::::::~~~~~~~~~~DDMMMMMM
 DD~:::::::~::::::~:::::::~::::::~:::::::~::::::~:::::::~::::::~~~~~~~~~~DDMMMMMM
 DD~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~DDMMMMMM
 DD~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~DDMMMMMM
 DD~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~DDMMMMMM
 DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDMMMMMM
 DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDMMMMMM
 *
 * This activity lets you select one of the frames and then remove this frame from the calibration data
 * You then can choose to rerun the calibration taking only the left over frames into account.
 * The actual calibration data is passed back to the @CameraCalibrationActivity and the calibration
 * itself is also started from the CameraCalibrationActivity.
 *
 */

public class CalibrationDetails extends Activity implements OnChartValueSelectedListener, View.OnClickListener {

    private BarChart mCalibrationChart;
    private ArrayList<Float> mReprojectionArray;
    private int mSelectedValue = -1;
    private ImageButton mButtonRemoveFrame;
    private ImageButton mButtonStartCalibration;
    private View mCalibrationDetailView;
    private CameraCalibrator mCalibrator;
    private int mResultCode = RESULT_CANCELED;
    private List<Float> mRemovedItems = new ArrayList<>();
    private List<Mat> mRemovedFrames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_details);

        mCalibrationDetailView = this.findViewById(R.id.view_camera_calibration_detail);
        mCalibrationDetailView.setOnClickListener(this);

        mButtonRemoveFrame = (ImageButton) this.findViewById(R.id.button_calibDelete);
        mButtonRemoveFrame.setOnClickListener(this);
        ImageButtonDisable.setImageButtonEnabled(this.getApplicationContext(),false,mButtonRemoveFrame,R.drawable.ic_delete_forever_white_24px);

        mButtonStartCalibration = (ImageButton) this.findViewById(R.id.button_calibDeleteConfirm);
        mButtonStartCalibration.setOnClickListener(this);
        ImageButtonDisable.setImageButtonEnabled(this.getApplicationContext(),false,mButtonStartCalibration,R.drawable.ic_done_white_24px);


        // in this example, a LineChart is initialized from xml
        mCalibrationChart = (BarChart) findViewById(R.id.chart);
        mCalibrationChart.setOnChartValueSelectedListener(this);

        //Retrive the calibrator from the extras. The calibrator is passed when starting the intend
        mCalibrator = (CameraCalibrator) getIntent().getExtras().getSerializable(CameraCalibrationActivity.INTENT_EXTRA_CAMERA_CALIBRATOR);

        mReprojectionArray = mCalibrator.getReprojectionErrorArrayList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        generateChart(mReprojectionArray);
    }

    private void generateChart(List<Float> reprojectionArray) {
        List<BarEntry> entries = new ArrayList<>();
        float average = 0;

        for (int i = 0; i <= reprojectionArray.size() -1; i++) {
            // turn your data into Entry objects
            entries.add(new BarEntry(i, (Float) reprojectionArray.get(i)));
            average +=(float) reprojectionArray.get(i);
        }
        average /= reprojectionArray.size();

        BarDataSet dataSet = new BarDataSet(entries, "Label"); // add entries to dataset
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barData.setValueTextSize(10f);
        mCalibrationChart.setFitBars(true);
        mCalibrationChart.setData(barData);
        mCalibrationChart.setDescription("");
        mCalibrationChart.getLegend().setEnabled(false);
        YAxis axisLeft = mCalibrationChart.getAxisLeft();
        axisLeft.setEnabled(false);
        axisLeft.setDrawGridLines(false);
        mCalibrationChart.getAxisRight().setEnabled(false);
        mCalibrationChart.getXAxis().setEnabled(false);

        LimitLine ll = new LimitLine(average, (float) (Math.round(average * 1000))/1000 + " " + getString(R.string.string_averageReproErr));
        ll.setLineColor(ContextCompat.getColor(this, R.color.highlight_artk_light));
        ll.setLineWidth(1f);
        ll.setTextColor(Color.BLACK);
        ll.setTextSize(8f);
        ll.enableDashedLine(40,20,0);
        axisLeft.removeAllLimitLines();
        axisLeft.addLimitLine(ll);

        mCalibrationChart.setScaleEnabled(false);
        mCalibrationChart.setDrawGridBackground(false);
        mCalibrationChart.invalidate(); // refresh
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("TAG","Selected value" + e.getX() + " : " + mCalibrationChart.getHighlighted()[0].getX());
        ImageButtonDisable.setImageButtonEnabled(this.getApplicationContext(),true,mButtonRemoveFrame,R.drawable.ic_delete_forever_white_24px);
    }

    @Override
    public void onNothingSelected() {
        //mSelectedValue = -1;
        ImageButtonDisable.setImageButtonEnabled(this.getApplicationContext(),false,mButtonRemoveFrame,R.drawable.ic_delete_forever_white_24px);
    }

    @Override
    public void onClick(View view) {
        if(view.equals(mButtonRemoveFrame)){
            if(Math.round(mCalibrationChart.getHighlighted()[0].getX()) >=0) {
                ImageButtonDisable.setImageButtonEnabled(this.getApplicationContext(),true,mButtonStartCalibration,R.drawable.ic_done_white_24px);

                int selectedValueX = Math.round(mCalibrationChart.getHighlighted()[0].getX());
                mCalibrationChart.highlightValue(-1, -1, true);

                Log.i("TAG","Selected value" + selectedValueX);

                mCalibrationChart.getData().getDataSetByIndex(0).removeEntryByXPos(selectedValueX);
                mCalibrationChart.invalidate();

                mRemovedFrames.add(mCalibrator.getFrame(selectedValueX));
                mRemovedItems.add(mReprojectionArray.get(selectedValueX));

            }
            else{
                Toast toast = Toast.makeText(this, R.string.error_no_selection, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        if(view.equals(mButtonStartCalibration)){
            ImageButtonDisable.setImageButtonEnabled(this.getApplicationContext(),false,mButtonStartCalibration,R.drawable.ic_done_white_24px);
            mResultCode = RESULT_OK;
            applyModification();
            generateChart(mReprojectionArray);

            Toast toast = Toast.makeText(this, R.string.error_values_removed, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void applyModification() {
        for (Float item:mRemovedItems) {
            mReprojectionArray.remove(item);
        }

        for (Mat frame: mRemovedFrames){
            mCalibrator.removeFrame(frame);
        }

        mRemovedItems = new ArrayList<>();
        mRemovedFrames = new ArrayList<>();
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        if(mResultCode == RESULT_OK) {
            resultIntent.putExtra(CameraCalibrationActivity.INTENT_EXTRA_CAMERA_CALIBRATOR, mCalibrator);
        }
        setResult(mResultCode, resultIntent);
        super.finish();
    }


}
