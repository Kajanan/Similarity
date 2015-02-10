
package com.assign.chunk;

import com.assign.util.ScoredObject;

import java.util.Iterator;

public interface NBestChunker extends Chunker {

    public Iterator<ScoredObject<Chunking>>
        nBest(char[] cs, int start, int end, int maxNBest);


}
