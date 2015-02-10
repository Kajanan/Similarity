

package com.assign.chunk;


public interface Chunker {

    public Chunking chunk(CharSequence cSeq);
    public Chunking chunk(char[] cs, int start, int end);

}
