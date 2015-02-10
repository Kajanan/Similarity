

package com.assign.chunk;

import com.assign.util.Scored;

import java.util.Comparator;


public interface Chunk extends Scored {

    
    public int start();

    public int end();

    public String type();

    public double score();

    public boolean equals(Object that);

    public int hashCode();

    public static final Comparator<Chunk> TEXT_ORDER_COMPARATOR
    = new Comparator<Chunk>() {
        public int compare(Chunk c1, Chunk c2) {
            if (c1.start() < c2.start()) return -1;
            if (c1.start() > c2.start()) return 1;
            if (c1.end() < c2.end()) return -1;
            if (c1.end() > c2.end()) return 1;
            return 0;
        }
        };


    public static final Comparator<Chunk> LONGEST_MATCH_ORDER_COMPARATOR
    = new Comparator<Chunk>() {
        public int compare(Chunk c1, Chunk c2) {
            if (c1.start() < c2.start()) return -1;
            if (c1.start() > c2.start()) return 1;
            if (c1.end() < c2.end()) return 1;
            if (c1.end() > c2.end()) return -1;
            if (c1.score() > c2.score()) return -1;
            if (c1.score() < c2.score()) return 1;
            return c1.type().compareTo(c2.type());
        }
        };
}


