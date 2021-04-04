package media.capture;

import java.nio.ByteBuffer;
import java.net.Socket;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
//import java.lang.Object.CircularByteBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class creates a audio injector
 */
public class AudioInjector implements Runnable{
    private AudioFormat format;
    private int bufferSize = 4096;
    private DataLine.Info dataLineInfo;  // no good
    private SourceDataLine sourceDataLine; // no good
    private ConcurrentLinkedQueue<ByteBuffer> audioBufferQueue;

    ///\private Socket s = new Socket("127.0.0.1", 0);  // could be a issue for large amount of calls
    //private CircularByteBuffer stream = new CircularByteBuffer();  // can be resized
    private ByteArrayOutputStream stream = new ByteArrayOutputStream();

    private ExecutorService streamer;
    private int nThreads = 1;

    private boolean closed = false;

    public AudioInjector(AudioFormat.Encoding encoding, float samplerate, int sampleSizeInBits, int channels, int frameSize, float frameRate, boolean bigEndian){
        this.format = new AudioFormat(encoding, samplerate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        this.configStream();
    }

    public AudioInjector(AudioFormat.Encoding encoding, float samplerate, int sampleSizeInBits, int channels, int frameSize, float frameRate){
        this.format = new AudioFormat(encoding, samplerate, sampleSizeInBits, channels, frameSize, frameRate, false);
        this.configStream();
    }

    public AudioInjector(AudioFormat format){
        this.format = format;
        this.configStream();
    }

    public void configStream(){
        try{
            //this.dataLineInfo = new DataLine.Info(SourceDataLine.class, this.format);
            //this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(this.dataLineInfo);
            this.audioBufferQueue = new ConcurrentLinkedQueue<ByteBuffer>();
            this.streamer = Executors.newFixedThreadPool(this.nThreads);
            this.streamer.submit(this);
        }catch (Exception e){
            System.out.println("Error in configuring audio injector: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public DataLine.Info getLineInfo(){
        return dataLineInfo;
    }

    public ByteArrayOutputStream getOutStream(){
        return stream;
        //stream.getInputStream();
    }

    public AudioFormat getFormat(){
        return format;
    }

    public void close(){
        try{
            closed = true;
            streamer.shutdown();
            if(!streamer.awaitTermination(30, TimeUnit.SECONDS)){
                streamer.shutdownNow();
                if(!streamer.awaitTermination(30, TimeUnit.SECONDS)){
                    System.out.println("ERROR: Streamer service did not stop");
                }else{
                    System.out.println("WARNING: Streamer service did stop when asked to");
                }
            }else{
                System.out.println("Streamer service did stop");
            }
            closed = false;
        }catch(Exception e){
            System.out.println("Error in closing audio injector service: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public void put(ByteBuffer buffer){
         audioBufferQueue.add(buffer);
    }

    @Override
    public void run(){
        System.out.println("Started Streaming service");
        try{
            //sourceDataLine.open(format, bufferSize);
            //sourceDataLine.start();
            int position = 0;
            do{
                streamToLine(position);
            }while(!closed);

            System.out.println("Sreaming serivce is ending");
            while(!audioBufferQueue.isEmpty()){
                streamToLine(position);
            }
            //sourceDataLine.stop();
            //sourceDataLine.close();
        }catch(Exception e){
            System.out.println("Error at Streamer thread: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void streamToLine(int position){
        ByteBuffer buff = audioBufferQueue.poll();
        if(buff != null){
            byte [] bytebuffer = buff.array();
            //sourceDataLine.write(bytebuffer, position, bytebuffer.length);
            //sourceDataLine.drain();
            //position = position + bytebuffer.length;

            stream.write(bytebuffer, 0, bytebuffer.length);
            
        }
    }

}
