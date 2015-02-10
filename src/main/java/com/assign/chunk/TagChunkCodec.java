
package com.assign.chunk;

import com.assign.tag.StringTagging;
import com.assign.tag.Tagging;
import com.assign.tag.TagLattice;

import java.util.Iterator;
import java.util.Set;


public interface TagChunkCodec {

    
    public Tagging<String> toTagging(Chunking chunking);

   
    public StringTagging toStringTagging(Chunking chunking);
    
    
    public Chunking toChunking(StringTagging tagging);

    
    public Set<String> tagSet(Set<String> chunkTypes);

   
    public boolean legalTags(String... tags);


    public boolean legalTagSubSequence(String... tags);

    

   
    public boolean isEncodable(Chunking chunking);

    
    public  boolean isDecodable(StringTagging tagging);

  
    public Iterator<Chunk> nBestChunks(TagLattice<String> lattice, 
                                       int[] tokenStarts, int[] tokenEnds,
                                       int maxResults);
    
}