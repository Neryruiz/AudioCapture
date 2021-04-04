package media.capture;

import java.nio.ByteBuffer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import media.frame.CCMediaFrame;

/**FrameCollector */

public class FrameCollector implements Runnable{
    SortBuffer fromTarget;
    SortBuffer toTarget;
    ConcurrentLinkedQueue<CCMediaFrame> fromTargetQueue;
    ConcurrentLinkedQueue<CCMediaFrame> toTargetQueue;

    private ExecutorService collectProcess;
    private int nThreads = 1;

    private boolean end = false;

    public FrameCollector(SortBuffer fromTarget, SortBuffer toTarget){
        this.fromTarget = fromTarget;
        this.toTarget = toTarget;
        this.fromTargetQueue = new ConcurrentLinkedQueue<CCMediaFrame>();
        this.toTargetQueue = new ConcurrentLinkedQueue<CCMediaFrame>();

        this.collectProcess = Executors.newFixedThreadPool(nThreads);
        this.collectProcess.submit(this);
    }

    public void putFromTarget(CCMediaFrame frame){
        fromTargetQueue.add(frame);
    }

    public void putToTarget(CCMediaFrame frame){
        toTargetQueue.add(frame);
    }

    public void close(){
        try{
            end = true;
            collectProcess.shutdown();
            if(!collectProcess.awaitTermination(30, TimeUnit.SECONDS)){
                collectProcess.shutdownNow();
                if(!collectProcess.awaitTermination(30, TimeUnit.SECONDS)){
                    System.out.println("ERROR: AudioCollector service did not stop");
                }else{
                    System.out.println("WARNING: AudioCollector service did stop when asked to");
                }
            }else{
                System.out.println("AudioCollector service did stop");
            }
        }catch(Exception e){
            System.out.println("Error in Collector service: "+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        do{
            sendToSort();
        }while (!end);

        while(!toTargetQueue.isEmpty() || !fromTargetQueue.isEmpty()){
            sendToSort();
        }
    }

    public void sendToSort(){
        CCMediaFrame toTargetFrame = toTargetQueue.poll();
        CCMediaFrame fromTargetFrame = fromTargetQueue.poll();

        //note that in CCMediaFrame getBuffer is a Buffer object so getArray();
        if(toTargetFrame != null){
            toTarget.put(toTargetFrame.getSequenceNumber(), toTargetFrame.getBuffer().array());
        }

        if(fromTargetFrame != null){
            fromTarget.put(fromTargetFrame.getSequenceNumber(), fromTargetFrame.getBuffer().array());
        }
    }

    public void flushSort(){
        toTarget.flushBuffer();
        fromTarget.flushBuffer();
    }
}
