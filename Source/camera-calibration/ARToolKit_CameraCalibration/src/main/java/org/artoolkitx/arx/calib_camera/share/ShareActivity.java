package org.artoolkitx.arx.calib_camera.share;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import org.artoolkitx.arx.calib_camera.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class ShareActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = ShareActivity.class.getSimpleName();

    private ShareCalibrationsArrayAdapter shareCalibrationsArrayAdapterArrayAdapter;
    private ArrayList<File> mCalibsList;
    private ImageButton mDeleteButton;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File selectedFile = mCalibsList.get(position);
        Uri contentUri = FileProvider.getUriForFile(this, "org.artoolkitx.arx.fileprovider", selectedFile);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("application/octet-stream");
        sharingIntent.putExtra(Intent.EXTRA_STREAM,contentUri);
        startActivity(Intent.createChooser(sharingIntent,getResources().getString(R.string.share_text)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        ListView calibrationsListView = (ListView) findViewById(R.id.shareList);

        File calibsFile = new File(this.getCacheDir().getAbsolutePath()+"/calibs");
        Log.d(TAG,"calibsFile: " + calibsFile.toString());

        mCalibsList = new ArrayList<>(Arrays.asList(calibsFile.listFiles()));
        shareCalibrationsArrayAdapterArrayAdapter = new ShareCalibrationsArrayAdapter(this,
                mCalibsList);
        calibrationsListView.setAdapter(shareCalibrationsArrayAdapterArrayAdapter);
        calibrationsListView.setOnItemClickListener(this);

        mDeleteButton = (ImageButton) findViewById(R.id.button_shareDelete);
        mDeleteButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        ArrayList<File> filesToRemove = new ArrayList<>();
        if(v==mDeleteButton){
            for (File file:mCalibsList){
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                filesToRemove.add(file);
            }
            mCalibsList.removeAll(filesToRemove);
        }
        shareCalibrationsArrayAdapterArrayAdapter.notifyDataSetChanged();
    }
}
