package media.capture;

import java.nio.ByteBuffer;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioInputStream;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioWriter implements Runnable{
    private DataLine.Info info;
    private TargetDataLine line;
    private AudioFormat format;
    private String fileLocation;
    private File file;
    private AudioFileFormat.Type fileFormat = AudioFileFormat.Type.WAVE;

    private ByteArrayOutputStream outputStream;

    private ExecutorService writerProcess;
    private int nThreads = 1;

    private boolean pause = false;
    private boolean close = false;

    /*public AudioWriter(DataLine.Info info, File file, AudioFormat format){
        try{
            this.info = info;
            this.format = format;
            this.file = file;
            if(!AudioSystem.isLineSupported(this.info)){
                System.out.println("ERROR: Line is not supported");
            }else{
                this.line = (TargetDataLine) AudioSystem.getLine(info);
            }
            
        }catch(Exception e){
            System.out.println("Error at Audio Writer consturctor: "+e.getMessage());
            e.printStackTrace();
        }
        this.writerProcess = Executors.newFixedThreadPool(this.nThreads);
    }*/

    public AudioWriter(ByteArrayOutputStream stream, File file, AudioFormat format){
        try{
            this.outputStream = stream;
            this.format = format;
            this.file = file;
            
        }catch(Exception e){
            System.out.println("Error at Audio Writer consturctor: "+e.getMessage());
            e.printStackTrace();
        }
        this.writerProcess = Executors.newFixedThreadPool(this.nThreads);
    }

    public void startCapture(){
        writerProcess.submit(this);
    }

    public void pauseCapture(){
        pause = true;
    }

    public void closeCapture(){
        try{
            close = true;
            writerProcess.shutdown();
            if(!writerProcess.awaitTermination(30, TimeUnit.SECONDS)){
                writerProcess.shutdownNow();
                if(!writerProcess.awaitTermination(30, TimeUnit.SECONDS)){
                    System.out.println("ERROR: AudioWriter service did not stop");
                }else{
                    System.out.println("WARNING: AudioWriter service did stop when asked to");
                }
            }else{
                System.out.println("AudioWriter service did stop");
            }
        }catch(Exception e){
            System.out.println("Error at closing AudioWriter: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public void record(){
        try{
            /*if(AudioSystem.isLineSupported(info)){
                line.open(format);
                line.start();

                System.out.println("Audio is being captured");
                AudioInputStream stream = new AudioInputStream(line);

                System.out.println("Audio is being recored to "+ fileLocation);
                AudioSystem.write(stream, fileFormat, file);
            }else{
                System.out.println("ERROR: Recording session is dropped");
            }*/;
            ByteArrayInputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
            AudioInputStream audioIn = new AudioInputStream(inStream, format, inStream.available());

            AudioSystem.write(audioIn, fileFormat, file);
        }catch(Exception e){
            System.out.println("Error when recording at AudioWriter: "+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        System.out.println("Staring audiowriter capture");
        //while (!close) {
            //if(!pause){
        record();
            //}
        //}
        System.out.println("Endding audiowriter capture service");
    }

}
