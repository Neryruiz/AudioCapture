package media.data;

import java.io.*;
import java.nio.*;
import java.util.*;

import media.frame.CCMediaFrame;

public class RTPGen{
    public byte [] frames;
    public int frame_pos = 0;

    public int RTPSeq = 0;
    public int RTPPayload_len = 32;

    public RTPGen(String file){
        try{
            Random rand = new Random();
            RTPSeq = rand.nextInt();
            RandomAccessFile audio = new RandomAccessFile(file, "r");
            
            int audio_numFrames = (int)audio.length();

            System.out.println("Total frames number of frames for audio = "+ String.valueOf(audio_numFrames));

            frames = new byte[audio_numFrames];
            
            // Reading the whole wave file and storing in memory
            audio.seek(58);

            int totalFramesRead = audio.read(frames, 0, audio_numFrames);

            System.out.println("Total frames read for audio = "+ String.valueOf(totalFramesRead));
            //frames = new byte[10];
            //Arrays.fill(frames, (byte)85);
            System.out.println("Starting Seq:"+String.valueOf(RTPSeq)); 
        }catch (Exception e){
            System.out.println("RTP gen error");
        }
    }

    public CCMediaFrame genFrame(){
        ByteBuffer buff = ByteBuffer.wrap(RTPPayload(RTPPayload_len));
        CCMediaFrame CCFrame = new CCMediaFrame(RTPSeq, buff);
        RTPSeq++;
        return CCFrame;
    }

    public byte [] RTPPayload(int size){
        byte [] RTPFrame = new byte [size];
        for(int i = 0; i < size; i++){
            RTPFrame[i] = frames[frame_pos];
            frame_pos++;
            if(frame_pos >= frames.length){
                frame_pos = 0;
            } 
        }
        return RTPFrame;
    }
}