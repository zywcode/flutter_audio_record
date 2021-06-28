package me.kbs.flutter_audio_record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.StringCodec;

/**
 * FlutterAudioRecordPlugin
 */
public class FlutterAudioRecordPlugin implements FlutterPlugin, BasicMessageChannel.MessageHandler<String>, BasicMessageChannel.Reply<String> {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private BasicMessageChannel<String> channel;

    private boolean isRecord = false;//是否在录制，默认没在录制
    private AudioRecordThread audioRecordThread;
    private int samples_per_frame = 2048;
    private int sampleRateInHz = 8000;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.channel = new BasicMessageChannel<>(flutterPluginBinding.getBinaryMessenger(), "flutter_audio_record", StringCodec.INSTANCE);
        channel.setMessageHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMessageHandler(null);
    }

    @Override
    public void onMessage(@Nullable String message, @NonNull BasicMessageChannel.Reply<String> reply) {
        Log.i("FlutterPluginRecord: ", message);
        reply.reply("FlutterAudioRecordPlugin收到：" + message);//可以通过reply进行回复
        if (message.equals("startRecord")) {
            startRecord1();
        } else if (message.equals("stopRecord")) {
            stopRecord();
        }
    }

    /**
     * 向Dart发送消息，并接受Dart的反馈
     *
     * @param message  要给Dart发送的消息内容
     * @param callback 来自Dart的反馈
     */
    void send(String message, BasicMessageChannel.Reply<String> callback) {
        channel.send(message, callback);
    }

    @Override
    public void reply(@Nullable String reply) {
        Log.i("FlutterPluginRecord: ", reply);
    }

    //开始录制
    public void startRecord1() {
        isRecord = true;
        //1.开启录音线程并准备录音
        audioRecordThread = new AudioRecordThread();
        audioRecordThread.start();
    }

    //停止录制
    public void stopRecord() {
        isRecord = false;
    }

    public class AudioRecordThread extends Thread {
        private AudioRecord audioRecord;
        private int bufferSize;

        AudioRecordThread() {
            Log.i("FlutterPluginRecord: ", "AudioRecordThread");
            /**
             * 1.设置缓冲区大小
             * 参数:采样率 16k; 通道数 单通道; 采样位数
             */
            int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT * 1);
            /**
             * 2.初始化AudioRecord
             * 参数:录音来源 麦克风; 采样率 16k; 通道数 单通道 ;采样位数/数据格式 pcm; 缓冲区大小
             */
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            Log.i("FlutterPluginRecord: ", "AudioRecordThread Run");
            super.run();
            try {
                audioRecord.startRecording();
                while (isRecord) {
                    byte[] byteBuffer = new byte[bufferSize];
                    int end = audioRecord.read(byteBuffer, 0, byteBuffer.length);
                    final String base64 = Base64.getEncoder().encodeToString(byteBuffer);
                    Log.i("bufferSize: ", String.valueOf(bufferSize));
                    Log.i("data: ", base64);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            channel.send(base64);
                        }
                    });
                    if (end == android.media.AudioRecord.ERROR_BAD_VALUE || end == android.media.AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e("wqs", "Read error");
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } finally {
                isRecord = false;// 停止文件写入
                audioRecord.stop();
                audioRecord.release();// 释放资源
                audioRecord = null;
            }
        }
    }

}
