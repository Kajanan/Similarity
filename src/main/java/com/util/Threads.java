/**
 * This source code is part of wordster project.
 *
 * This library is free software; you can redistribute it and/or modify it under 
 * the terms of the GNU Lesser General Public License (LGPL) as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more 
 * details.
 */
package com.util;




/**
 * @author Ryan Shaw
 * @created Jun 25, 2009
 * @version 1.0
 * @since JDK1.6
 */
public final class Threads
{
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
    
    
    public static void waitCompleted(Thread thread) {
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public static void waitCompleted(Thread thread, long timeoutMilliSec) {
        if (thread != null) {
            try {
                thread.join(timeoutMilliSec);
            } catch (InterruptedException e) {
            }
        }
    }
}


