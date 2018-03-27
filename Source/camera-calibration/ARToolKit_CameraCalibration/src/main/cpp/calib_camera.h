/*
 *  calib_camera.h
 *  ARToolKit for Android
 *
 *  This file is part of ARToolKit.
 *
 *  Copyright 2015-2016 Daqri LLC. All Rights Reserved.
 *  Copyright 2012-2015 ARToolworks, Inc. All Rights Reserved.
 *
 *  Author(s): Philip Lamb
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

#ifndef CALIB_CAMERA_H
#define CALIB_CAMERA_H

#include <ARX/AR/ar.h>

#ifdef __cplusplus
extern "C" {
#endif

extern JavaVM *jvm;
extern jobject objectCameraCalibActivity;

ARdouble getSizeFactor(ARdouble dist_factor[], int xsize, int ysize, int dist_function_version);
void convParam(float dist[4], int xsize, int ysize,float,float,float,float, ARParam *param);
void saveParam(const ARParam *param, ARdouble err_min, ARdouble err_avg, ARdouble err_max);

#ifdef __cplusplus
}
#endif
#endif // !CALIB_CAMERA_H
