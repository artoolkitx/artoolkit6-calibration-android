package org.artoolkitx.arx.calib_camera.share;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import org.artoolkitx.arx.calib_camera.R;

import java.io.File;
import java.util.ArrayList;

/**
 *  ARToolKit6
 *
 *  This file is part of ARToolKit.
 *
 *  Copyright 2015-2016 Daqri, LLC.
 *  Copyright 2010-2015 ARToolworks, Inc.
 *
 *  Author(s): Philip Lamb
 *
 *  Created by thorstenbux on 24/08/16.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
class ShareCalibrationsArrayAdapter extends ArrayAdapter<File> {
    private final int mMenuResource;
    private final ArrayList<File> mCacheFiles;

    public ShareCalibrationsArrayAdapter(ShareActivity shareActivity, ArrayList<File> cacheFiles) {
        super(shareActivity,R.layout.share_list_item,cacheFiles);
        this.mMenuResource = R.layout.share_list_item;
        this.mCacheFiles = cacheFiles;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView =  LayoutInflater.from(parent.getContext()).inflate(mMenuResource, parent, false);
            viewHolder.shareItemTextView = (TextView) convertView.findViewById(R.id.shareItem_Text);
            convertView.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String calibFileText = mCacheFiles.get(position).getName();
        viewHolder.shareItemTextView.setText(calibFileText);
        return convertView;
    }

    private static class ViewHolder{
        TextView shareItemTextView;
    }
}
