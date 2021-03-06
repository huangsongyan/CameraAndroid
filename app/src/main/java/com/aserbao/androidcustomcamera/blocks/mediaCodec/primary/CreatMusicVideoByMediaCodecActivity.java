package com.aserbao.androidcustomcamera.blocks.mediaCodec.primary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.aserbao.androidcustomcamera.R;
import com.aserbao.androidcustomcamera.base.activity.BaseActivity;
import com.aserbao.androidcustomcamera.base.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.OnClick;

public class CreatMusicVideoByMediaCodecActivity extends BaseActivity {
    private static final String TAG = "PrimaryMediaCodecActivi";
    private static final String MIME_TYPE = "video/avc";
    private static final int WIDTH = 720;
    private static final int HEIGHT = 1280;
    private static final int BIT_RATE = 4000000;
    private static final int FRAMES_PER_SECOND = 4;
    private static final int IFRAME_INTERVAL = 5;

    private static final int NUM_FRAMES = 4 * 100;
    private static final int START_RECORDING = 0;
    private static final int STOP_RECORDING = 1;

    @BindView(R.id.btn_recording)
    Button mBtnRecording;
    @BindView(R.id.btn_watch)
    Button mBtnWatch;
    @BindView(R.id.primary_mc_tv)
    TextView mPrimaryMcTv;
    public MediaCodec.BufferInfo mBufferInfo;
    public MediaCodec mMediaCodec;
    @BindView(R.id.primary_vv)
    VideoView mPrimaryVv;
    private Surface mInputSurface;
    public MediaMuxer mMuxer;
    private boolean mMuxerStarted;
    private int mTrackIndex;
    private long mFakePts;
    private boolean isRecording;

    private int cuurFrame = 0;

    private MyHanlder mMyHanlder = new MyHanlder(this);
    public File mOutputFile;

    @OnClick({R.id.btn_recording, R.id.btn_watch})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_recording:
                if (mBtnRecording.getText().equals("????????????")) {
                    try {
//                        mOutputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), System.currentTimeMillis() + ".mp4");
                        mOutputFile = new File(FileUtils.getStorageMp4("PrimaryMediaCodecActivity"));
                        startRecording(mOutputFile);
                        mPrimaryMcTv.setText("????????????????????????" + mOutputFile.toString());
                        mBtnRecording.setText("????????????");
                        isRecording = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        mBtnRecording.setText("?????????????????????????????????");
                    }
                } else if (mBtnRecording.getText().equals("????????????")) {
                    mBtnRecording.setText("????????????");
                    stopRecording();
                }
                break;
            case R.id.btn_watch:
                String absolutePath = mOutputFile.getAbsolutePath();
                if (!TextUtils.isEmpty(absolutePath)) {
                    if(mBtnWatch.getText().equals("????????????")) {
                        mBtnWatch.setText("????????????");
                        mPrimaryVv.setVideoPath(absolutePath);
                        mPrimaryVv.start();
                    }else if(mBtnWatch.getText().equals("????????????")){
                        if (mOutputFile.exists()){
                            mOutputFile.delete();
                            mBtnWatch.setText("????????????");
                        }
                    }
                }else{
                    Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private Bitmap mBitmap;
    private static class MyHanlder extends Handler {
        private WeakReference<CreatMusicVideoByMediaCodecActivity> mPrimaryMediaCodecActivityWeakReference;

        public MyHanlder(CreatMusicVideoByMediaCodecActivity activity) {
            mPrimaryMediaCodecActivityWeakReference = new WeakReference<CreatMusicVideoByMediaCodecActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CreatMusicVideoByMediaCodecActivity activity = mPrimaryMediaCodecActivityWeakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case START_RECORDING:
                        activity.drainEncoder(false);
                        activity.generateFrame(activity.cuurFrame);
                        Log.e(TAG, "handleMessage: " + activity.cuurFrame);
                        if (activity.cuurFrame < NUM_FRAMES) {
                            this.sendEmptyMessage(START_RECORDING);
                        } else {
                            activity.drainEncoder(true);
                            activity.mBtnRecording.setText("????????????");
                            activity.releaseEncoder();
                        }
                        activity.cuurFrame++;
                        break;
                    case STOP_RECORDING:
                        Log.e(TAG, "handleMessage: STOP_RECORDING");
                        activity.drainEncoder(true);
                        activity.mBtnRecording.setText("????????????");
                        activity.releaseEncoder();
                        break;
                }
            }
        }
    }

    @Override
    protected int setLayoutId() {
        return R.layout.activity_primary_media_codec;
    }


    private void startRecording(File outputFile) throws IOException {
        cuurFrame = 0;
        mBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.katong);
        prepareEncoder(outputFile);
        mMyHanlder.sendEmptyMessage(START_RECORDING);
    }

    private void stopRecording() {
        mMyHanlder.removeMessages(START_RECORDING);
        mMyHanlder.sendEmptyMessage(STOP_RECORDING);
    }

    /**
     * ????????????????????????muxer???????????????????????????
     */
    private void prepareEncoder(File outputFile) throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);

        //1. ???????????????????????????????????????????????????????????????MediaCodec.configure()????????????????????????????????????
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);//?????????(????????????????????????????????????????????????????????????)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAMES_PER_SECOND);//????????????
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);//???????????????????????????

        //2.????????????MediaCodec??????????????????????????????????????????????????????????????????????????????????????????????????????EGL??????????????????
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();
        //3. ????????????MediaMuxer???????????????????????????????????????????????????????????????????????????MediaFormat???????????????????????????
        // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // ??????MediaCodec???????????????H.264??????????????????.mp4?????????
        mMuxer = new MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        mMuxerStarted = false;
        mTrackIndex = -1;
    }

    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();//???????????????end-of-stream?????????????????????????????????????????????????????????
        }
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        while (true) {
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            Log.e(TAG, "drainEncoder: " + outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {//????????????????????????????????????
                if (!endOfStream) {
                    break;      // out of while
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                //?????????????????????????????????????????????????????????
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //????????????????????????????????????????????????????????????????????????
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (outputBufferIndex < 0) {
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[outputBufferIndex];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    //??????????????????????????????????????????????????????????????????????????????muxer???????????????????????????
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    //??????ByteBuffer????????????BufferInfo???
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mBufferInfo.presentationTimeUs = mFakePts;
                    mFakePts += 1000000L / FRAMES_PER_SECOND;

                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.e(TAG, "????????????");
                    } else {
                        Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "????????????");
                    }
                    isRecording = false;
                    break;
                }
            }
        }
    }


    private void generateFrame(int frameNum){
        Canvas canvas = mInputSurface.lockCanvas(null);
        Paint paint = new Paint();
        try {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            String  color = "#FFCA39";
            if (frameNum %2 == 0 ){
                color = "#FFCA39";
            }else{
                color = "#FFF353";
            }
            int color1 = Color.parseColor(color);
            canvas.drawColor(color1);
            paint.setTextSize(100);
            paint.setColor(0xff000000);
            canvas.drawText("???"+ String.valueOf(frameNum) + "???",width/2,height/2,paint);
            Rect srcRect = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            int margain = 30;
            Rect decRect = new Rect(margain, margain, width - margain, height-margain);
            canvas.drawBitmap(mBitmap,srcRect,decRect,paint);

            int roundMargain = 60;
            int roundHeight = 300;
            int roundRadius = 25;
            int roundLineWidth = 10;
            paint.setStyle(Paint.Style.FILL);//??????
            paint.setAntiAlias(true);// ???????????????????????????
            RectF roundRect1 = new RectF(roundMargain - roundLineWidth,roundMargain - roundLineWidth,width - roundMargain + roundLineWidth,roundHeight + roundMargain + roundLineWidth);
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(roundRect1,roundRadius,roundRadius,paint);
            paint.setColor(color1);
            RectF roundRect2 = new RectF(roundMargain,roundMargain,width - roundMargain,roundHeight + roundMargain);
            canvas.drawRoundRect(roundRect2,roundRadius,roundRadius,paint);

//            paint.setStyle(Paint.Style.STROKE);//??????
            int timeMargain = roundMargain + 50;
            String sTime = "2018/12/29 00:39";
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(40);
            paint.setColor(Color.BLACK);
            canvas.drawText(sTime,width/2,timeMargain,paint);

            int soundMargain = timeMargain + 80;
            String soundTime = "party ?????????";
            String soundTime2 = "party party ?????????";
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(80);
            canvas.drawText(soundTime,width/2,soundMargain,paint);
            canvas.drawText(soundTime2,width/2,soundMargain + 80,paint);

        } finally {
            mInputSurface.unlockCanvasAndPost(canvas);
        }

    }

    private void releaseEncoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }
}
