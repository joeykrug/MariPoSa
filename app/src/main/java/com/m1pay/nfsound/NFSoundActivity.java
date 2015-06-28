package com.m1pay.nfsound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException; 
import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.KeyListener;
import android.util.Log;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.m1pay.nfsound.NFSoundEngine;
import com.tbg.bitpaypos.app.R;

//import opensl_example.*;


import android.os.AsyncTask;

public class NFSoundActivity {
        private static final int RECORDER_BPP = 16;
        private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
        private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
        private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
        private static final int RECORDER_SAMPLERATE = 44100;
        private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        
        //private Decoder decoder = null;
        private AudioRecord recorder = null;
        private int bufferSize = 0;
        private Thread recordingThread = null;
        private boolean isRecording = false;
        private boolean runningFlag = false; 
        



        
		//private opensl_example dummy ;
		private Object myself ;
		private String receivedMessage;

        private String soundMessageText;

        private PlayThread soundThread;

        private AudioManager audioManager;
        


    /**
     * This allows you to send a sound message w/ a string
     * @param soundMessageText
     */
    public NFSoundActivity(String soundMessageText, AudioManager audioManager) {
        myself = this;
        this.soundMessageText = soundMessageText;
        this.audioManager = audioManager;
        sendString();

    }

    /**
     * this allows you to receive one
     * @param receiving
     */
    public NFSoundActivity(boolean receiving) {
        if(receiving) {
            startRecording();
        }
        else {
            // nothing
        }
    }
    



        private String getFilename(){
                String filepath = Environment.getExternalStorageDirectory().getPath();
                File file = new File(filepath,AUDIO_RECORDER_FOLDER);
                 
                if(!file.exists()){
                        file.mkdirs();
                }
                 
                return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
        }
        
        
        public void Receive(char buffer[],int length){  
            String msg = new String(buffer);  
            receivedMessage = msg;  
            
            System.out.println(msg);
            
            

            
            //Log.d("Test", msg);  
        }
         
 
         
        public void startRecording() {
        	recordingThread = new Thread() {
    			public void run() {
    				
    				
    				//setPriority(Thread.MAX_PRIORITY);
    	            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    	            // set the callback object to myself
    	            NFSoundEngine.register_callback(myself);
    	            //opensl_example.test_method(myself);
    	            // try to start recording
    	            NFSoundEngine.start_process();

    	            
    			}
    		};
    		recordingThread.start();   
    		
    		//mTextView.setText("decoding started...");
        }

        public void sendString() {
        	//String str = this.mEditView.getText().toString();
        	//NFSoundEngine.play_string(str);
        	
			soundThread = new PlayThread();
            soundThread.start();
        }
        
        private void stopRecording(){
        	NFSoundEngine.stop_process();
	    	//opensl_example.stop_process();
	    	/*
	    	try {
	    		recordingThread.destroy();
	    		recordingThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
	    	recordingThread = null;
	    	
    		//mTextView.setText("Sending...");
        }
                
    	class PlayThread extends Thread {
    		static final int frequency = 44100;
    		static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    		static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    		
    		public void run() {
        		AudioTrack audioTrack;
    			try {

    				int oldAudioMode = audioManager.getMode();
    				int oldRingerMode = audioManager.getRingerMode();
    				boolean isSpeakerPhoneOn = audioManager.isSpeakerphoneOn();

    				audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    				audioManager.setMode(AudioManager.MODE_NORMAL);
    				audioManager.setSpeakerphoneOn(true);
    				
    				int playBufSize=AudioTrack.getMinBufferSize(frequency,
    						channelConfiguration, audioEncoding);
    				// -----------------------------------------
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
    						channelConfiguration, audioEncoding,
    						playBufSize, AudioTrack.MODE_STREAM);

    				
    				audioTrack.play();//开始播放
    				if (true) {	
    					//String str = mEditView.getText().toString();
                        String str = soundMessageText;
    					byte[] tmpbuf = NFSoundEngine.encode_string(str);
    					audioTrack.write(tmpbuf, 0, tmpbuf.length);
                        Log.d("Sound message", "created");
    					
    					//audioTrack.write(buffer, 0, buffer.length);
    					//audioTrack.write(buffer, 0, bufferReadResult);

    				}
    				audioTrack.stop();
    				
    				audioManager.setSpeakerphoneOn(isSpeakerPhoneOn);
    				audioManager.setMode(oldAudioMode);
    				audioManager.setRingerMode(oldRingerMode);

    				
    				//audioRecord.stop();
    			} catch (Throwable t) {
    				//Toast.makeText(AudioEncoderPlayer.this, t.getMessage(), 1000);
    			}
    		}
    	};
        

        
        private void startRecording1(){
        	
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize*10);
                
                //decoder = new Decoder();
                //decoder.init_decoder();
                
                int i = recorder.getState();
                if(i==1)
                    recorder.startRecording();
                 
                isRecording = true;
                 
                recordingThread = new Thread(new Runnable() {
                         
                        @Override
                        public void run() {
                                writeAudioDataToFile();
                        }
                },"AudioRecorder Thread");
                 
                recordingThread.start();
        }
        
        private String getTempFilename(){
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath,AUDIO_RECORDER_FOLDER);
             
            if(!file.exists()){
                    file.mkdirs();
            }
             
            File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
             
            if(tempFile.exists())
                    tempFile.delete();
             
            return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }
         
        private void writeAudioDataToFile(){
                byte data[] = new byte[bufferSize];
                String filename = getTempFilename();
                FileOutputStream os = null;
                 
                try {
                        os = new FileOutputStream(filename);
                } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
                 
                int read = 0;
                 
                if(null != os){
                        while(isRecording){
                                short s0, s1;
                                int s,rc;
                        		read = recorder.read(data, 0, bufferSize);
                                
                        		/*
                                for (int i =0; i < read; i+=2) {
                                    s = 0; 
                                    s0 = (short) (data[i] & 0xff);// ????????? 
                                    s1 = (short) (data[i+1] & 0xff); 
                                    
                                    s1 <<= 8; 
                                    s = (int) (s0 | s1 + 65536); 
                                    s >>= 6;
                                	rc = decoder.process_sample(s);
                                	//rc = 0;
                                    if (rc !=0) {
                                		int a;
                                		a =1;
                                	}
                                }
                                */
                        		
                                
                                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                                        try {
                                        	
                                        
                                                os.write(data);
                                        } catch (IOException e) {
                                                e.printStackTrace();
                                        }
                                }
                                
                                
                        }
                         
                        try {
                                os.close();
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                }
        }
         
        

        
        private void stopRecording1(){
                if(null != recorder){
                        isRecording = false;
                         
                        int i = recorder.getState();
                        if(i==1)
                            recorder.stop();
                        recorder.release();
                         
                        recorder = null;
                        recordingThread = null;
                }
                 
                copyWaveFile(getTempFilename(),getFilename());
                deleteTempFile();
        }
 
        
        
        
        private void deleteTempFile() {
                File file = new File(getTempFilename());
                 
                file.delete();
        }
         
        private void copyWaveFile(String inFilename,String outFilename){
                FileInputStream in = null;
                FileOutputStream out = null;
                long totalAudioLen = 0;
                long totalDataLen = totalAudioLen + 36;
                long longSampleRate = RECORDER_SAMPLERATE;
                int channels = 2;
                long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
                 
                byte[] data = new byte[bufferSize];
                 
                
                
                try {
                        in = new FileInputStream(inFilename);
                        out = new FileOutputStream(outFilename);
                        totalAudioLen = in.getChannel().size();
                        totalDataLen = totalAudioLen + 36;
                         
                        AppLog.logString("File size: " + totalDataLen);
                         
                        WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                                        longSampleRate, channels, byteRate);
                         
                        while(in.read(data) != -1){
                                out.write(data);
                        }
                         
                        in.close();
                        out.close();
                } catch (FileNotFoundException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
 
        private void WriteWaveFileHeader(
                        FileOutputStream out, long totalAudioLen,
                        long totalDataLen, long longSampleRate, int channels,
                        long byteRate) throws IOException {
                 
                byte[] header = new byte[44];
                 
                header[0] = 'R';  // RIFF/WAVE header
                header[1] = 'I';
                header[2] = 'F';
                header[3] = 'F';
                header[4] = (byte) (totalDataLen & 0xff);
                header[5] = (byte) ((totalDataLen >> 8) & 0xff);
                header[6] = (byte) ((totalDataLen >> 16) & 0xff);
                header[7] = (byte) ((totalDataLen >> 24) & 0xff);
                header[8] = 'W';
                header[9] = 'A';
                header[10] = 'V';
                header[11] = 'E';
                header[12] = 'f';  // 'fmt ' chunk
                header[13] = 'm';
                header[14] = 't';
                header[15] = ' ';
                header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
                header[17] = 0;
                header[18] = 0;
                header[19] = 0;
                header[20] = 1;  // format = 1
                header[21] = 0;
                header[22] = (byte) channels;
                header[23] = 0;
                header[24] = (byte) (longSampleRate & 0xff);
                header[25] = (byte) ((longSampleRate >> 8) & 0xff);
                header[26] = (byte) ((longSampleRate >> 16) & 0xff);
                header[27] = (byte) ((longSampleRate >> 24) & 0xff);
                header[28] = (byte) (byteRate & 0xff);
                header[29] = (byte) ((byteRate >> 8) & 0xff);
                header[30] = (byte) ((byteRate >> 16) & 0xff);
                header[31] = (byte) ((byteRate >> 24) & 0xff);
                header[32] = (byte) (2 * 16 / 8);  // block align
                header[33] = 0;
                header[34] = RECORDER_BPP;  // bits per sample
                header[35] = 0;
                header[36] = 'd';
                header[37] = 'a';
                header[38] = 't';
                header[39] = 'a';
                header[40] = (byte) (totalAudioLen & 0xff);
                header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
                header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
                header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
 
                out.write(header, 0, 44);
        }

}