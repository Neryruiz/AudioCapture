package media.frame;

import java.nio.ByteBuffer;

public class CCMediaFrame{
    private int sequence;
    private ByteBuffer buff;

    public CCMediaFrame(int seq, ByteBuffer buff){
        this.sequence = seq;
        this.buff = buff;
    }

    public ByteBuffer getBuffer(){
        return buff;
    }

    public int getSequenceNumber(){
        return sequence;
    }
}