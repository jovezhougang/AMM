package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class MainActivity extends AppCompatActivity {

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {

            Files.copy( getAssets().open("music.mp3"),new File(getExternalFilesDir(""),"music.mp3")
                    .toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        startDecoderAudio();
    }

    private void startDecoderAudio(){
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                MediaExtractor extractor = new MediaExtractor();
                try {
                    extractor.setDataSource(new File(getExternalFilesDir(""),"music.mp3")
                            .getAbsolutePath());
                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,48000, AudioFormat.CHANNEL_CONFIGURATION_STEREO
                            ,AudioFormat.ENCODING_PCM_16BIT,AudioTrack.getMinBufferSize(48000,AudioFormat.CHANNEL_CONFIGURATION_STEREO,AudioFormat.ENCODING_PCM_16BIT),AudioTrack.MODE_STREAM);
                    audioTrack.play();
                    extractor.selectTrack(0);
                    final MediaFormat format = extractor.getTrackFormat(0);
                    System.out.println("format==>"+format);
                    final String mime = format.getString(MediaFormat.KEY_MIME);
                    MediaCodec mediaCodec = MediaCodec.createDecoderByType(mime);
                    mediaCodec.configure(format,null,null,0);
                    mediaCodec.start();
                    ByteBuffer[] inputBuffer = mediaCodec.getInputBuffers();
                    ByteBuffer[] outBuffer = mediaCodec.getOutputBuffers();
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    for (;;){
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(500);
                        if(inputBufferIndex >= 0) {
                            final ByteBuffer byteBuffers = inputBuffer[inputBufferIndex];
                            int readIndex = extractor.readSampleData(byteBuffers, 0);
                            System.out.println("getSampleTime===>"+extractor.getSampleTime());
                            if (inputBufferIndex >= 0 && readIndex > 0) {
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, readIndex
                                        , 0, 0);
                                extractor.advance();
                            }
                            for (; ; ) {
                                int outBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                                if (outBufferIndex >= 0) {
                                    ByteBuffer buffers = outBuffer[outBufferIndex];
                                    buffers.position(bufferInfo.offset);
                                    buffers.limit(bufferInfo.offset + bufferInfo.size);
                                    final byte[] data = new byte[bufferInfo.size];
                                    buffers.get(data);
                                    presentationTimeUsSum += bufferInfo.presentationTimeUs;
                                    audioTrack.write(data,0,data.length);
                                    System.out.println("presentationTimeUsSum===>"+presentationTimeUsSum);
                                    mediaCodec.releaseOutputBuffer(outBufferIndex,false);
                                }else{
                                    break;
                                }
                            }
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    long presentationTimeUsSum;
}