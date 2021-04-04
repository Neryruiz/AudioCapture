package media.capture;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
    This class is to collect bytes, sort them accorrding to the seqence and join them to a bytebuffer. 
 */

public class SortBuffer{
    private int capacity;  // Buffer size
    private ByteBuffer buffer; // buffer for data to be written to

    private int SortMapLimit; // limits the number of bytebuffer stored at Sortmap
    private Map<Integer, ByteBuffer> SortMap = new HashMap<Integer, ByteBuffer>();  // temporary buffer map to sort data 
    private ConcurrentLinkedQueue<ByteBuffer> outQueue = new ConcurrentLinkedQueue<ByteBuffer>();  // hold output buffers to be collected later

    public SortBuffer(int mapLimit, int capacity){
        SortMapLimit = mapLimit;
        this.capacity = capacity;
        this.buffer = ByteBuffer.allocate(capacity);
        System.out.println("Testing sortbuffer");
    }

    public void put(int sequence, byte [] data){
        // adds byte array to sort map, sorting by sequnce number
        ByteBuffer buff = ByteBuffer.allocate(data.length);
        buff.put(data);
        Integer seq = new Integer(sequence);
        SortMap.put(seq, buff);
        if(SortMap.size() <= SortMapLimit){
            // sort map limit reached, write to buffer in correct order depending on sequence
            this.writeToBuff();
        }
    }

    public void put(byte [] data){
        // write directly to buffer
        buffer.put(data);
    }
    
    public ByteBuffer deepCopy(ByteBuffer original){
        ByteBuffer out = ByteBuffer.allocate(capacity);
        original.rewind();
        out.put(original.array());
        return out;
    }

    public void writeToBuff(){
        // write buffers from sort map to main buffer
        for(Map.Entry item: SortMap.entrySet()){
            ByteBuffer buff = (ByteBuffer) item.getValue();
            if(buffer.capacity() > buffer.position()+ buff.capacity()){
                // if data fit in buffer
                buff.rewind();
                buffer.put(buff.array());
            }else{
                // when full add buffer to output queue to be collected later and create new buffer
                /*byte fillValue1 = buffer.get(buffer.position()-2); // Not a good fix for fill the gaps
                byte fillValue2 = buffer.get(buffer.position()-1);
                while(buffer.position() < buffer.capacity()){
                    buffer.put(fillValue1);
                    buffer.put(fillValue2);
                }*/
                if(buffer.position()%2 != 0){
                    byte fillValue = buffer.get(buffer.position()-2);
                    buffer.put(fillValue);
                }
                int size = buffer.position();
                ByteBuffer newBuff = ByteBuffer.allocate(size);//ByteBuffer.wrap(buffer.array());
                newBuff.put(buffer.array(),0, size);
                outQueue.add(newBuff);  // (this.deepCopy(buffer));
                buffer.clear();
                buffer = ByteBuffer.allocate(capacity);
                buffer.put(buff.array());
            }
        }
        SortMap.clear();
    }

    public void flushBuffer(){
        for(Map.Entry item: SortMap.entrySet()){
            ByteBuffer buff = (ByteBuffer) item.getValue();
            if(buffer.capacity() > buffer.position()+ buff.capacity()){
                // if data fit in buffer
                buff.rewind();
                buffer.put(buff.array());
            }else{
                // when full add buffer to output queue to be collected later and create new buffer
                if(buffer.position()%2 != 0){
                    byte fillValue = buffer.get(buffer.position()-2);
                    buffer.put(fillValue);
                }
                int size = buffer.position();
                outQueue.add(this.deepCopy(buffer));
                buffer.clear();
                buffer = ByteBuffer.allocate(capacity);
                buffer.put(buff.array());
            }
        }
        outQueue.add(this.deepCopy(buffer)); 
        buffer.clear();
        SortMap.clear();
    }

    public boolean isEmpty(){
        return outQueue.isEmpty();
    }

    public ByteBuffer poll(){
        return outQueue.poll();
    }
}

