package org.artoolkitx.arx.calib_camera.utils;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * ARToolKit6
 * <p>
 * This file is part of ARToolKit.
 * <p>
 * Copyright 2015-2016 Daqri, LLC.
 * Copyright 2010-2015 ARToolworks, Inc.
 * <p>
 * Author(s): Philip Lamb
 * <p>
 * Created by Thorsten Bux on 6/09/16.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *     http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 */
public abstract class RotationConverters {

    public static final double CALIB_PI=3.14159265358979323846;
    public static final double CALIB_PI_2=1.57079632679489661923;
    public static final int CALIB_DEGREES=1;
    public static final int CALIB_RADIANS=0;


    static Mat Euler(Mat src, int argType)
    {
        Mat dst = new Mat(3, 1, CvType.CV_64F);

        if((src.rows() == 3) && (src.cols() == 3))
        {
            //convert rotaion matrix to 3 angles (pitch, yaw, roll)
            double pitch, yaw, roll;

            if(src.get(0,2)[0] < -0.998)
            {
                pitch = -Math.atan2(src.get(1,0)[0], src.get(1,1)[0]);
                yaw = -CALIB_PI_2;
                roll = 0.;
            }
            else if(src.get(0,2)[0] > 0.998)
            {
                pitch = Math.atan2(src.get(1,0)[0], src.get(1,1)[0]);
                yaw = CALIB_PI_2;
                roll = 0.;
            }
            else
            {
                pitch = Math.atan2(-src.get(1,2)[0], src.get(2,2)[0]);
                yaw = Math.asin(src.get(0,2)[0]);
                roll = Math.atan2(-src.get(0,1)[0], src.get(0,0)[0]);
            }

            if(argType == CALIB_DEGREES)
            {
                pitch *= 180./CALIB_PI;
                yaw *= 180./CALIB_PI;
                roll *= 180./CALIB_PI;
            }

            else if(argType != CALIB_RADIANS)
                throw new CvException("Invalid argument type: " + Core.StsBadFlag);

            dst.put(0,0,pitch);
            dst.put(1,0,yaw);
            dst.put(2,0,roll);
        }
        else if( (src.cols() == 1 && src.rows() == 3) ||
                (src.cols() == 3 && src.rows() == 1 ) )
        {
            //convert vector which contains 3 angles (pitch, yaw, roll) to rotaion matrix
            double pitch, yaw, roll;
            if(src.cols() == 1 && src.rows() == 3)
            {
                pitch = src.get(0,0)[0];
                yaw = src.get(1,0)[0];
                roll = src.get(2,0)[0];
            }
            else{
                pitch = src.get(0,0)[0];
                yaw = src.get(0,1)[0];
                roll = src.get(0,2)[0];
            }

            if(argType == CALIB_DEGREES)
            {
                pitch *= CALIB_PI / 180.;
                yaw *= CALIB_PI / 180.;
                roll *= CALIB_PI / 180.;
            }
            else if(argType != CALIB_RADIANS)
                throw new CvException("Invalid argument type: " + Core.StsBadFlag);

            Mat M = new Mat (3, 3, CvType.CV_64F);
            Mat i = Mat.eye(3, 3, CvType.CV_64F);
            i.copyTo(dst);
            i.copyTo(M);

            //0 = 0,0
            //1 = 0,1
            //2 = 0,2
            //3 = 1,0
            //4 = 1,1
            //5 = 1,2
            //6 = 2,0
            //7 = 2,1
            //8 = 2,2

            //double* pR = dst.ptr<double>();
            //pR[4] = Math.cos(pitch);
            dst.put(1,1,Math.cos(pitch));
            //pR[7] = Math.sin(pitch);
            dst.put(2,1,Math.sin(pitch));
            //pR[8] = pR[4];
            dst.put(2,2,dst.get(1,1)[0]);
            //pR[5] = -pR[7];
            dst.put(1,2,-dst.get(2,1)[0]);

            //double* pM = M.ptr<double>();
            //pM[0] = cos(yaw);
            M.put(0,0,Math.cos(yaw));
            //pM[2] = sin(yaw);
            M.put(0,2,Math.sin(yaw));
            //pM[8] = pM[0];
            M.put(2,2,M.get(0,0)[0]);
            //pM[6] = -pM[2];
            M.put(2,2,M.get(0,0)[0]);


            //dst *= M;
            dst = dst.mul(M);
            i.copyTo(M);
            //pM[0] = Math.cos(roll);
            M.put(0,0,Math.cos(roll));
            //pM[3] = Math.sin(roll);
            M.put(1,0,Math.sin(roll));
            //pM[4] = pM[0];
            M.put(1,1,M.get(0,0)[0]);
            //pM[1] = -pM[3];
            M.put(0,1,M.get(1,0)[0]);

//            dst *= M;
            dst = dst.mul(M);
        }
        else
            throw new CvException("Input matrix must be 1x3, 3x1 or 3x3" + Core.StsBadFlag);
        return dst;
    }

    public static Mat rodriguesToEuler( Mat src, int argType)
    {
        Mat dst = null;
        if((src.cols() == 1 && src.rows() == 3) || (src.cols() == 3 && src.rows() == 1)) {
            //CV_Assert((src.cols == 1 && src.rows == 3) || (src.cols == 3 && src.rows == 1));
            Mat R = new Mat();
            Calib3d.Rodrigues(src, R);
            dst = RotationConverters.Euler(R, argType);
        }
        return dst;
    }
}
