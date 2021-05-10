package com.aserbao.androidcustomcamera.blocks.mediaMuxer.functions;

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
import android.os.Environment;
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
import com.aserbao.androidcustomcamera.blocks.mediaExtractor.primary.decoder.DecoderAudioAndGetDb;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import VideoHandle.EpEditor;
import VideoHandle.OnEditorListener;
import butterknife.BindView;
import butterknife.OnClick;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static android.media.MediaFormat.MIMETYPE_AUDIO_MPEG;

public class CreateVideoAddAudioToMp4 extends BaseActivity {
    private static final String TAG = "PrimaryMediaCodecActivi";
    private static final String MIME_TYPE = "video/avc";
    private static final int WIDTH = 720;
    private static final int HEIGHT = 1280;
    private static final int BIT_RATE = 4000000;
    private static final int FRAMES_PER_SECOND = 30;
    private static final int IFRAME_INTERVAL = 5;

    private static final int NUM_FRAMES = 1 * 100;
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
    private double mVolume;
    private float mMusictime;
    @OnClick({R.id.btn_recording, R.id.btn_watch})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_recording:
                String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                if (mBtnRecording.getText().equals("开始录制")) {
                    try {
//                        mOutputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), System.currentTimeMillis() + ".mp4");
                        mOutputFile = new File(FileUtils.getStorageMp4("PrimaryMediaCodecActivity"));
                        startRecording(mOutputFile);
                        mPrimaryMcTv.setText("文件保存路径为：" + mOutputFile.toString());
                        mBtnRecording.setText("停止录制");
                        isRecording = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        mBtnRecording.setText("出现异常了，请查明原因");
                    }
                } else if (mBtnRecording.getText().equals("停止录制")) {
                    mBtnRecording.setText("开始录制");
                    stopRecording();
                }
//                new DecoderAndGetAudioDb().start(path + "/own.m4a", MIMETYPE_AUDIO_AAC, new DecoderAndGetAudioDb.IGetVideoDbCallBackListener() {
                new DecoderAndGetAudioDb().start(path + "/five.mp3", MIMETYPE_AUDIO_MPEG, new DecoderAndGetAudioDb.DbCallBackListener() {
//                new DecoderAndGetAudioDb().start(path + "/dj_dance.mp3", MIMETYPE_AUDIO_MPEG, new DecoderAndGetAudioDb.IGetVideoDbCallBackListener() {
                    @Override
                    public void cuurentFrequenty(final int cuurentFrequenty, final double volume, final float decoderTime) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(volume != -1 && isRecording) {
                                    mVolume = volume / 100;
                                    mMusictime = decoderTime;
                                    int i = cuurFrame * 1000 / FRAMES_PER_SECOND;
                                    if(decoderTime > i) {
                                        update();
                                        Log.e(TAG, "run:volume =  " +mVolume + "  decoderTime = " + decoderTime + " 第"+ cuurFrame + "帧时间为："+ i);
                                    }
                                }else{
                                    Log.e(TAG, "run: out"  );
                                    if (isRecording) {
                                        drainEncoder(true);
                                    }
                                    releaseEncoder();
                                }
                            }
                        });
                    }
                });
                break;
            case R.id.btn_watch:
               /* String absolutePath = mOutputFile.getAbsolutePath();
                if (!TextUtils.isEmpty(absolutePath)) {
                    if(mBtnWatch.getText().equals("查看视频")) {
                        mBtnWatch.setText("删除视频");
                        mPrimaryVv.setVideoPath(absolutePath);
                        mPrimaryVv.start();
                    }else if(mBtnWatch.getText().equals("删除视频")){
                        if (mOutputFile.exists()){
                            mOutputFile.delete();
                            mBtnWatch.setText("查看视频");
                        }
                    }
                }else{
                    Toast.makeText(this, "请先录制", Toast.LENGTH_SHORT).show();
                }*/
                addMusicToMp4();
                break;
        }
    }

    public void update(){
        drainEncoder(false);
        generateFrame(cuurFrame);
        cuurFrame++;
        Log.e(TAG, "handleMessage: " + cuurFrame);
    }
    private Bitmap mBitmap;
    private static class MyHanlder extends Handler {
        private WeakReference<CreateVideoAddAudioToMp4> mPrimaryMediaCodecActivityWeakReference;

        public MyHanlder(CreateVideoAddAudioToMp4 activity) {
            mPrimaryMediaCodecActivityWeakReference = new WeakReference<CreateVideoAddAudioToMp4>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CreateVideoAddAudioToMp4 activity = mPrimaryMediaCodecActivityWeakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case START_RECORDING:
                        activity.update();
                        break;
                    case STOP_RECORDING:
                        Log.e(TAG, "handleMessage: STOP_RECORDING");
                        activity.drainEncoder(true);
                        activity.mBtnRecording.setText("开始录制");
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
        isRecording = false;
    }

    /**
     * 准备视频编码器，muxer，和一个输入表面。
     */
    private void prepareEncoder(File outputFile) throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);

        //1. 设置一些属性。没有指定其中的一些可能会导致MediaCodec.configure()调用抛出一个无用的异常。
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);//比特率(比特率越高，音视频质量越高，编码文件越大)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAMES_PER_SECOND);//设置帧速
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);//设置关键帧间隔时间

        //2.创建一个MediaCodec编码器，并配置格式。获取一个我们可以用于输入的表面，并将其封装到处理EGL工作的类中。
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();
        //3. 创建一个MediaMuxer。我们不能在这里添加视频跟踪和开始合成，因为我们的MediaFormat里面没有缓冲数据。
        // 只有在编码器开始处理数据后才能从编码器获得这些数据。我们实际上对多路复用音频没有兴趣。我们只是想要
        // 将从MediaCodec获得的原始H.264基本流转换为.mp4文件。
//        mMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getAbsolutePath() + "/output_aserbao.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        mMuxer = new MediaMuxer(absolutePath+ "/input_aserbao1.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        mMuxerStarted = false;
        mTrackIndex = -1;
    }

    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();//在输入信号end-of-stream。相当于提交一个空缓冲区。视频编码完结
        }
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        while (true) {
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            Log.e(TAG, "drainEncoder: " + outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {//没有可以输出的数据使用时
                if (!endOfStream) {
                    break;      // out of while
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                //输出缓冲区已经更改，客户端必须引用新的
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //输出格式发生了变化，后续数据将使用新的数据格式。
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
                    //当我们得到的时候，编解码器的配置数据被拉出来，并给了muxer。这时候可以忽略。
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    //调整ByteBuffer值以匹配BufferInfo。
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mBufferInfo.presentationTimeUs = mFakePts;
                    mFakePts += 1000000L / FRAMES_PER_SECOND;

                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.e(TAG, "意外结束");
                    } else {
                        Toast.makeText(this, "已完成……", Toast.LENGTH_SHORT).show();
//
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
            int color1 = changeHue((float) mVolume);
            canvas.drawColor(color1);
            paint.setTextSize(100);
            paint.setColor(0xff000000);
            canvas.drawText("第"+ String.valueOf(frameNum) + "帧",width/2,height/2,paint);
            Rect srcRect = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            int margain = 30;
            Rect decRect = new Rect(margain, margain, width - margain, height-margain);
            canvas.drawBitmap(mBitmap,srcRect,decRect,paint);

            int roundMargain = 60;
            int roundHeight = 300;
            int roundRadius = 25;
            int roundLineWidth = 10;
            paint.setStyle(Paint.Style.FILL);//充满
            paint.setAntiAlias(true);// 设置画笔的锯齿效果
            RectF roundRect1 = new RectF(roundMargain - roundLineWidth,roundMargain - roundLineWidth,width - roundMargain + roundLineWidth,roundHeight + roundMargain + roundLineWidth);
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(roundRect1,roundRadius,roundRadius,paint);
            paint.setColor(color1);
            RectF roundRect2 = new RectF(roundMargain,roundMargain,width - roundMargain,roundHeight + roundMargain);
            canvas.drawRoundRect(roundRect2,roundRadius,roundRadius,paint);

//            paint.setStyle(Paint.Style.STROKE);//充满
            int timeMargain = roundMargain + 50;
            String sTime = "2018/12/29 00:39";
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(40);
            paint.setColor(Color.BLACK);
            canvas.drawText(sTime,width/2,timeMargain,paint);

            int soundMargain = timeMargain + 80;
            String soundTime = "party 是我家";
            String soundTime2 = "party party 是我家";
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(80);
            canvas.drawText(soundTime,width/2,soundMargain,paint);
            canvas.drawText(soundTime2,width/2,soundMargain + 80,paint);

        } finally {
            mInputSurface.unlockCanvasAndPost(canvas);
        }

    }

    /**
     * @param progress 0 ~ 360
     * @return
     */
    public int changeHue(float progress){
        float[] hsbVals = new float[3];
        int inputColor = Color.parseColor("#FFF757");
        Color.colorToHSV(inputColor,hsbVals);
        float v = (float) progress / (float) 360;
        hsbVals[0] = progress;
        int color = Color.HSVToColor(hsbVals);
        return color;
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

    private String outputVideoPath;
    public void addMusicToMp4(){
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String inputMusic = absolutePath + "/five.mp3";
        outputVideoPath = absolutePath + "/output_aserbao1.mp4";
        String inputVideo = absolutePath+ "/input_aserbao1.mp4";
        String cmd = "-y -i "+ inputVideo + " -ss 0 -t "+ 35 + " -i "+ inputMusic + " -acodec copy -vcodec copy "+ outputVideoPath;
        EpEditor.execCmd(cmd, 10000,new OnEditorListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "sssshahhah onSuccess: " );
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "sssshahhah  onFailure: " );
            }

            @Override
            public void onProgress(float v) {
            }
        });
    }

}
