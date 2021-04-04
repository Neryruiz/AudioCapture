package media;

import java.io.*;
import java.util.*;
import java.nio.*;

import media.capture.SortBuffer;
import media.capture.AudioFormatter;
import media.capture.FrameCollector;
import media.capture.AudioInjector;
import media.capture.AudioWriter;

//For testing
import media.data.RTPGen;

public class MediaCapture{
    // public SortBuffer toTarget;
    public static void main(String args[]){
        System.out.println("Starting media capture test");
        String fileLocation = "test5.wav";

        // Media capture 
        File file = new File(fileLocation);
            
        SortBuffer toTarget = new SortBuffer(1000, 1000);
        SortBuffer fromTarget = new SortBuffer(1000, 1000);
        AudioFormatter formatter = new AudioFormatter(fromTarget, toTarget, 8);   //ALAW RTP
        FrameCollector collector = new FrameCollector(fromTarget, toTarget);
        AudioInjector injector = new AudioInjector(formatter.getFormat());
        AudioWriter writer = new AudioWriter(injector.getOutStream(), file, formatter.getFormat());
        
        //just for testing
        RTPGen toTargetAudio = new RTPGen("resource/audiosample/shortVoiceCall/00_Caller.wav");
        RTPGen fromTargetAudio = new RTPGen("resource/audiosample/shortVoiceCall/01_Caller.wav");

        try{
            System.out.println("Generating data");
            for(int i = 0; i < 10000; i++){

                // to collect and process CCMediaFrames
                collector.putFromTarget(fromTargetAudio.genFrame());
                collector.putToTarget(toTargetAudio.genFrame());
                ByteBuffer buff = formatter.getJoinedStream();
                if(buff != null){
                    injector.put(buff);
                }
                Thread.sleep(10);
            }
            System.out.println("Stop Generating data");
            System.out.println("Sleep 5");
            Thread.sleep(5000);
            System.out.println("Sleep done");
        }catch(Exception e){
            System.out.println("Did not sleep");
        }

        // Write to file
        writer.startCapture();

        // Close everything
        writer.closeCapture();
        formatter.close();
        collector.close();
        injector.close();
        

        System.out.println("Done");
    }
}