

package com.assign.chunk;

import java.util.Iterator;


public interface ConfidenceChunker {

    public Iterator<Chunk> nBestChunks(char[] cs, int start, int end, 
                                       int maxNBest);
    

}
