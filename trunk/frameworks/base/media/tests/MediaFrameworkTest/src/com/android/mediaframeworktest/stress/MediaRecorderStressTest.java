/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mediaframeworktest.stress;


import com.android.mediaframeworktest.MediaFrameworkTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Junit / Instrumentation test case for the media player api
 
 */  
public class MediaRecorderStressTest extends ActivityInstrumentationTestCase2<MediaFrameworkTest> {    
    
  
    private String TAG = "MediaRecorderStressTest";
    private MediaRecorder mRecorder;
    private Camera mCamera;
   
    private static final int NUMBER_OF_CAMERA_STRESS_LOOPS = 100;
    private static final int NUMBER_OF_RECORDER_STRESS_LOOPS = 100;
    private static final int NUMBER_OF_RECORDERANDPLAY_STRESS_LOOPS = 50;
    private static final int NUMBER_OF_SWTICHING_LOOPS_BW_CAMERA_AND_RECORDER = 200;
    private static final long WAIT_TIME_CAMERA_TEST = 3000;  // 3 second
    private static final long WAIT_TIME_RECORDER_TEST = 6000;  // 6 second
    private static final long WAIT_TIME_RECORD = 10000;  // 10 seconds
    private static final long WAIT_TIME_PLAYBACK = 6000;  // 6 second
    private static final String OUTPUT_FILE = "/sdcard/temp";
    private static final String OUTPUT_FILE_EXT = ".3gp";
    private static final String MEDIA_STRESS_OUTPUT =
        "/sdcard/mediaStressOutput.txt";
    private Looper mCameraLooper = null;
    private Looper mRecorderLooper = null;
    private final Object lock = new Object();
    private final Object recorderlock = new Object();
    private static int WAIT_FOR_COMMAND_TO_COMPLETE = 10000;  // Milliseconds.
    private final CameraErrorCallback mCameraErrorCallback = new CameraErrorCallback();
    private final RecorderErrorCallback mRecorderErrorCallback = new RecorderErrorCallback();

    public MediaRecorderStressTest() {
        super("com.android.mediaframeworktest", MediaFrameworkTest.class);
    }

    protected void setUp() throws Exception {
        getActivity();
        super.setUp();      
    }

    private final class CameraErrorCallback implements android.hardware.Camera.ErrorCallback {
        public void onError(int error, android.hardware.Camera camera) {
            if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {
                assertTrue("Camera test mediaserver died", false);
            }
        }
    }

    private final class RecorderErrorCallback implements MediaRecorder.OnErrorListener {
        public void onError(MediaRecorder mr, int what, int extra) {
            // fail the test case no matter what error come up
            assertTrue("mediaRecorder error", false);
        }
    }

    private void initializeCameraMessageLooper() {
        Log.v(TAG, "start looper");
        new Thread() {
            @Override
            public void run() {
                // Set up a looper to be used by camera.
                Looper.prepare();
                Log.v(TAG, "start loopRun");
                mCameraLooper = Looper.myLooper();
                mCamera = Camera.open();
                synchronized (lock) {
                    lock.notify();
                }
                Looper.loop();
                Log.v(TAG, "initializeMessageLooper: quit.");
            }
        }.start();
    }

    private void initializeRecorderMessageLooper() {
        Log.v(TAG, "start looper");
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Log.v(TAG, "start loopRun");
                mRecorderLooper = Looper.myLooper();
                mRecorder = new MediaRecorder();
                synchronized (recorderlock) {
                    recorderlock.notify();
                }
                Looper.loop();  // Blocks forever until Looper.quit() is called.
                Log.v(TAG, "initializeMessageLooper: quit.");
            }
        }.start();
    }

    /*
     * Terminates the message looper thread.
     */
    private void terminateCameraMessageLooper() {
        mCameraLooper.quit();
        try {
            Thread.sleep(1000);
        } catch (Exception e){
            Log.v(TAG, e.toString());
        }
        mCamera.release();
    }

    /*
     * Terminates the message looper thread.
     */
    private void terminateRecorderMessageLooper() {
        mRecorderLooper.quit();
        try {
            Thread.sleep(1000);
        } catch (Exception e){
            Log.v(TAG, e.toString());
        }
        mRecorder.release();
    }

    //Test case for stressing the camera preview.
    @LargeTest
    public void testStressCamera() throws Exception {
        SurfaceHolder mSurfaceHolder;
        mSurfaceHolder = MediaFrameworkTest.mSurfaceView.getHolder();
        File stressOutFile = new File(MEDIA_STRESS_OUTPUT);
        Writer output = new BufferedWriter(new FileWriter(stressOutFile, true));
        output.write("Camera start preview stress:\n");
        output.write("Total number of loops:" +
                NUMBER_OF_CAMERA_STRESS_LOOPS + "\n");
        try {
            Log.v(TAG, "Start preview");
            output.write("No of loop: ");

            for (int i = 0; i< NUMBER_OF_CAMERA_STRESS_LOOPS; i++){
                synchronized (lock) {
                    initializeCameraMessageLooper();
                    try {
                        lock.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
                    } catch(Exception e) {
                        Log.v(TAG, "wait was interrupted.");
                    }
                }
                mCamera.setErrorCallback(mCameraErrorCallback);
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                Thread.sleep(WAIT_TIME_CAMERA_TEST);
                mCamera.stopPreview();
                terminateCameraMessageLooper();
                output.write(" ," + i);
            }
        } catch (Exception e) {
            assertTrue("CameraStressTest", false);
            Log.v(TAG, e.toString());
        }
        output.write("\n\n");
        output.close();
    }

    //Test case for stressing the camera preview.
    @LargeTest
    public void testStressRecorder() throws Exception {
        String filename;
        SurfaceHolder mSurfaceHolder;
        mSurfaceHolder = MediaFrameworkTest.mSurfaceView.getHolder();
        File stressOutFile = new File(MEDIA_STRESS_OUTPUT);
        Writer output = new BufferedWriter(new FileWriter(stressOutFile, true));
        output.write("H263 video record- reset after prepare Stress test\n");
        output.write("Total number of loops:" +
                NUMBER_OF_RECORDER_STRESS_LOOPS + "\n");
        try {
            output.write("No of loop: ");
            Log.v(TAG, "Start preview");
            for (int i = 0; i < NUMBER_OF_RECORDER_STRESS_LOOPS; i++){
                synchronized (recorderlock) {
                    initializeRecorderMessageLooper();
                    try {
                        recorderlock.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
                    } catch(Exception e) {
                        Log.v(TAG, "wait was interrupted.");
                    }
                }
                Log.v(TAG, "counter = " + i);
                filename = OUTPUT_FILE + i + OUTPUT_FILE_EXT;
                Log.v(TAG, filename);
                mRecorder.setOnErrorListener(mRecorderErrorCallback);
                mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile(filename);
                mRecorder.setVideoFrameRate(20);
                mRecorder.setVideoSize(176,144);
                Log.v(TAG, "setEncoder");
                mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
                mSurfaceHolder = MediaFrameworkTest.mSurfaceView.getHolder();
                Log.v(TAG, "setPreview");
                mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                Log.v(TAG, "prepare");
                mRecorder.prepare();
                Log.v(TAG, "before release");
                Thread.sleep(WAIT_TIME_RECORDER_TEST);
                mRecorder.reset();
                terminateRecorderMessageLooper();
                output.write(", " + i);
            }
        } catch (Exception e) {
            assertTrue("Recorder Stress test", false);
            Log.v(TAG, e.toString());
        }
        output.write("\n\n");
        output.close();
    }

    //Stress test case for switching camera and video recorder preview.
    @LargeTest
    public void testStressCameraSwitchRecorder() throws Exception {
        String filename;
        SurfaceHolder mSurfaceHolder;
        mSurfaceHolder = MediaFrameworkTest.mSurfaceView.getHolder();
        File stressOutFile = new File(MEDIA_STRESS_OUTPUT);
        Writer output = new BufferedWriter(new FileWriter(stressOutFile, true));
        output.write("Camera and video recorder preview switching\n");
        output.write("Total number of loops:"
                + NUMBER_OF_SWTICHING_LOOPS_BW_CAMERA_AND_RECORDER + "\n");
        try {
            Log.v(TAG, "Start preview");
            output.write("No of loop: ");
            for (int i = 0; i < NUMBER_OF_SWTICHING_LOOPS_BW_CAMERA_AND_RECORDER; i++){
                synchronized (lock) {
                    initializeCameraMessageLooper();
                    try {
                        lock.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
                    } catch(Exception e) {
                        Log.v(TAG, "wait was interrupted.");
                    }
                }
                mCamera.setErrorCallback(mCameraErrorCallback);
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                Thread.sleep(WAIT_TIME_CAMERA_TEST);
                mCamera.stopPreview();
                terminateCameraMessageLooper();
                mCamera = null;
                Log.v(TAG, "release camera");
                filename = OUTPUT_FILE + i + OUTPUT_FILE_EXT;
                Log.v(TAG, filename);
                synchronized (recorderlock) {
                    initializeRecorderMessageLooper();
                    try {
                        recorderlock.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
                    } catch(Exception e) {
                        Log.v(TAG, "wait was interrupted.");
                    }
                }
                mRecorder.setOnErrorListener(mRecorderErrorCallback);
                mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile(filename);
                mRecorder.setVideoFrameRate(20);
                mRecorder.setVideoSize(176,144);
                Log.v(TAG, "Media recorder setEncoder");
                mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
                Log.v(TAG, "mediaRecorder setPreview");
                mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                Log.v(TAG, "prepare");
                mRecorder.prepare();
                Log.v(TAG, "before release");
                Thread.sleep(WAIT_TIME_CAMERA_TEST);
                terminateRecorderMessageLooper();
                Log.v(TAG, "release video recorder");
                output.write(", " + i);
            }
        } catch (Exception e) {
            assertTrue("Camer and recorder switch mode", false);
                Log.v(TAG, e.toString());
        }
        output.write("\n\n");
        output.close();
    }

    //Stress test case for record a video and play right away.
    @LargeTest
    public void testStressRecordVideoAndPlayback() throws Exception {
        String filename;
        SurfaceHolder mSurfaceHolder;
        mSurfaceHolder = MediaFrameworkTest.mSurfaceView.getHolder();
        File stressOutFile = new File(MEDIA_STRESS_OUTPUT);
        Writer output = new BufferedWriter(new FileWriter(stressOutFile, true));
        output.write("Video record and play back stress test:\n");
        output.write("Total number of loops:"
                + NUMBER_OF_RECORDERANDPLAY_STRESS_LOOPS + "\n");
        try {
            output.write("No of loop: ");
            for (int i = 0; i < NUMBER_OF_RECORDERANDPLAY_STRESS_LOOPS; i++){
                filename = OUTPUT_FILE + i + OUTPUT_FILE_EXT;
                Log.v(TAG, filename);
                synchronized (recorderlock) {
                    initializeRecorderMessageLooper();
                    try {
                        recorderlock.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
                    } catch(Exception e) {
                        Log.v(TAG, "wait was interrupted.");
                    }
                }
                mRecorder.setOnErrorListener(mRecorderErrorCallback);
                mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile(filename);
                mRecorder.setVideoFrameRate(20);
                mRecorder.setVideoSize(352,288);
                mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                Log.v(TAG, "mediaRecorder setPreview");
                mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                mRecorder.prepare();
                mRecorder.start();
                Thread.sleep(WAIT_TIME_RECORD);
                Log.v(TAG, "Before stop");
                mRecorder.stop();
                terminateRecorderMessageLooper();
                //start the playback
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(filename);
                mp.setDisplay(MediaFrameworkTest.mSurfaceView.getHolder());
                mp.prepare();
                mp.start();
                Thread.sleep(WAIT_TIME_PLAYBACK);
                mp.release();
                output.write(", " + i);
            }
        } catch (Exception e) {
            assertTrue("record and playback", false);
                Log.v(TAG, e.toString());
        }
        output.write("\n\n");
        output.close();
    }
}