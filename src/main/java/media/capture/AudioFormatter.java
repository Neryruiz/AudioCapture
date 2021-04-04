package media.capture;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import java.util.Map;
import java.util.HashMap;
import java.lang.Object;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.Date;

public class AudioFormatter implements Runnable{
    private SortBuffer fromTarget;
    private SortBuffer toTarget;

    //private ConcurrentLinkedQueue<ByteBuffer> inFromTargetQueue = new ConcurrentLinkedQueue<ByteBuffer>();
    //private ConcurrentLinkedQueue<ByteBuffer> inToTargetQueue = new ConcurrentLinkedQueue<ByteBuffer>();
    private ConcurrentLinkedQueue<ByteBuffer> outQueue = new ConcurrentLinkedQueue<ByteBuffer>();

    private ExecutorService formatter;
    private int nThreads = 1;
    
    private ByteBuffer buffer;
    private int capacity = 1000;  // Total number of byte for buffer

    private AudioFormat audioFormat;

    private boolean end = false;

    // Set SortBuffer and allocate main buffer
    public AudioFormatter(SortBuffer fromTarget, SortBuffer toTarget){
        this.fromTarget = fromTarget;
        this.toTarget = toTarget;

        this.buffer = ByteBuffer.allocate(this.capacity);
        this.formatter = Executors.newFixedThreadPool(this.nThreads);
        this.formatter.submit(this);
    }

    // Set SortBuffer, allocate main buffer, and set audio format
    public AudioFormatter(SortBuffer fromTarget, SortBuffer toTarget, int PayloadType){
        this.fromTarget = fromTarget;
        this.toTarget = toTarget;

        this.buffer = ByteBuffer.allocate(this.capacity);

        this.audioFormat = getAudioFormat(PayloadType);
        this.formatter = Executors.newFixedThreadPool(this.nThreads);
        this.formatter.submit(this);
    }

    // get current format
    public AudioFormat getFormat(){
        return this.audioFormat;
    }

    // Set audio format
    public void setFormat(AudioFormat format){
        this.audioFormat = format;
    }

    public void setFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits, int channels, int frameSize, float frameRate){
        this.audioFormat = getAudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate);
    }

    public void setFormat(int PayloadType){
        this.audioFormat = getAudioFormat(PayloadType);
    }

    // generate new audio format object
    public AudioFormat getAudioFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits, int channels, int frameSize, float frameRate){  // boolean bigEndian
        return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, false);
    }

    // generate new audio format object from payload type 
    public AudioFormat getAudioFormat(int PayloadType){
        AudioFormat format = null;
        switch(PayloadType){
            case 0:  //PCMU, TU-T G.711 PCM μ-Law 
                format = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 2, 1, 8000, false);
                System.out.print("Setting audio formate for ULAW");
                break;
            case 1:  // FS-1016 CELP 
            case 2:  // ITU-T G.721 ADPCM
            case 3:  // GSM
            case 4:  // G723, ITU-T G.723.1 
            case 5:  // DVI4, IMA ADPCM audio 32 kbit/s
            case 6:  // DVI4, IMA ADPCM audio 64 kbit/s
            case 7:  // LPC, Linear Predictive Coding audio 5.6 kbit/s
                System.out.print("Not Supported Payload type");
                break;
            case 8:  //PCMA, ITU-T G.711 PCM A-Law audio 64 kbit/s
                format = new AudioFormat(AudioFormat.Encoding.ALAW, 8000, 8, 2, 1, 8000, false);
                System.out.print("Setting audio formate for ALAW");
                break;
            case 9:  //	G722, ITU-T G.722 audio 64 kbit/s
                System.out.print("Not Supported Payload type");
                break;
            case 10:  // L16, Linear PCM 16-bit Stereo audio 1411.2 kbit/s, uncompressed
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 2, 44100, false);
                System.out.print("Setting audio formate for LPCM");
                break;
            case 11:  // L16, Linear PCM 16-bit audio 705.6 kbit/s, uncompressed
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);
                System.out.print("Setting audio formate for mono LPCM");
                break;
            case 12:  // QCELP, Qualcomm Code Excited Linear Prediction
            case 13:  // CN, Comfort noise
            case 14:  // MPA, MPEG-1 or MPEG-2 audio only
            case 15:  // G728, ITU-T G.728 audio 16 kbit/s
            case 16:  // DVI4, IMA ADPCM audio 44.1 kbit/s
            case 17:  // DVI4, IMA ADPCM audio 88.2 kbit/s
            case 18:  // G729, ITU-T G.729 and G.729a audio 8 kbit/s
            case 19:  // CN, Comfort noise
                System.out.print("Not Supported Payload type");
                break;
            default:
                System.out.print("Payload type is not listed in supported types");
                break;
        }
        return format;
    }

    // add bytebuffer from external class
    //public void putFromTarget(ByteBuffer buff){
    //    inFromTargetQueue.add(buff);
    //}

    //public void putToTarget(ByteBuffer buff){
    //    inToTargetQueue.add(buff);
    //} 

    // mix bytebuffer to be collected later
    public ByteBuffer getJoinedStream(){
        return outQueue.poll();
    }

    public void close(){
        try{
            fromTarget.flushBuffer();
            toTarget.flushBuffer();
            end = true;
            formatter.shutdown();
            if(!formatter.awaitTermination(30, TimeUnit.SECONDS)){
                formatter.shutdownNow();
                if(!formatter.awaitTermination(30, TimeUnit.SECONDS)){
                    System.out.println("ERROR Audio Formatter did not stop!!!");
                }else{
                    System.out.println("WARNING Audio Formatter did stop when asked.");
                }
            }else{
                System.out.println("Audio Formatter did stop");
            }
        }catch(Exception e){
            System.out.println("ERROR Audio Formatter: "+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        //ByteBuffer fromTargetBuff = null;
        //ByteBuffer toTargetBuff = null;
        System.out.println("Starting to format audio streams");
        if(audioFormat != null){
            do{
                outQueue.add(mixChannels());
            }while(!end);
            System.out.println("Empting queues format audio streams");
            // mix remaining data
            while(!fromTarget.isEmpty() || !toTarget.isEmpty()){
                outQueue.add(mixChannels());
            }
            System.out.println("Stoping to format audio streams");
        }else{
            System.out.println("Error format not set for audio streams");
        }
    }

    public ByteBuffer mixChannels(){
        ByteBuffer fromTargetBuff = null;
        ByteBuffer toTargetBuff = null;
        ByteBuffer outBuff;

        Date date = new Date();
        long delay = 125 + 50; // 125ms + some compute delay; This is ok for now but this should corrlate to the sample size, the size of the buffer, the of each packet and time it should take to process; EX: 4ms of audio * (1000 bytes / 32 byte per packet) 
        long timeout = date.getTime() + delay;
        boolean timedout = false;
        
        // wait for buffer be ready from both channels
        while(fromTargetBuff == null || toTargetBuff == null){
            if(fromTargetBuff == null){
                fromTargetBuff = fromTarget.poll();
            }

            if(toTargetBuff == null){
                toTargetBuff = toTarget.poll();
            }

            if(date.getTime() > timeout && (fromTargetBuff != null || toTargetBuff != null)){
                timedout = true;
                break;
            }

            if(end){
                return null;
            }
        } 

        // allocate the correct size for buffer
        if(timedout){
            // this will leave gaps of audio bc data might be missing
            if(fromTargetBuff != null){
                outBuff = ByteBuffer.allocate(fromTargetBuff.capacity()*2);
            }else if(toTargetBuff != null){
                outBuff = ByteBuffer.allocate(toTargetBuff.capacity()*2);
            }else{
                System.out.println("ERROR Could not mix channels, no data");
                return null;
            }
        }else{
            // outBuff size must be even or will cause channels to flip
            if(toTargetBuff.capacity() >= fromTargetBuff.capacity()){
                outBuff = ByteBuffer.allocate(toTargetBuff.capacity()*2);
            }else{
                outBuff = ByteBuffer.allocate(fromTargetBuff.capacity()*2);
            }
        }

        // assuming that one of these buffer are not null
        byte [] fromArray;
        byte [] toArray;
        if(fromTargetBuff != null){
            fromArray = fromTargetBuff.array();
        }else{
            fromArray = new byte [toTargetBuff.capacity()];
        }

        if(toTargetBuff != null){
            toArray = toTargetBuff.array(); 
        }else{
            toArray = new byte [fromTargetBuff.capacity()];
        }


        // filling outBuffer with from and to array according to frame size
        int offset = audioFormat.getFrameSize();
        for(int i = 0, j = 0; i < fromArray.length && j < toArray.length; i = i + offset, j = j + offset){
            if(i < fromArray.length){
                for(int ii = 0; ii < offset; ii++){
                    outBuff.put(fromArray[i+ii]);  // FromTarget is channel 1
                }
            }

            if(j < fromArray.length){
                for(int jj = 0; jj < offset; jj++){
                    outBuff.put(toArray[j+jj]);  // Totarget is channel 2
                }
            }
        }

        return outBuff;
    }
}