

package com.assign.chunk;

import java.util.Set;


public interface Chunking {

    public Set<Chunk> chunkSet();

    public CharSequence charSequence();

    public boolean equals(Object that);

    public int hashCode();


}
