/*
 * Copyright Matt Palmer 2011, All rights reserved.
 *
 * This code is licensed under a standard 3-clause BSD license:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 
 *  * The names of its contributors may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.domesdaybook.searcher.sequence;

import net.domesdaybook.object.LazyObject;
import java.io.IOException;
import java.util.Arrays;
import net.domesdaybook.matcher.sequence.SequenceMatcher;
import net.domesdaybook.matcher.singlebyte.SingleByteMatcher;
import net.domesdaybook.reader.Reader;
import net.domesdaybook.reader.Window;

/**
 *
 * @author Matt Palmer
 */
public final class SundayQuickSearcher extends AbstractSequenceSearcher {

    private final LazyObject<int[]> forwardInfo;
    private final LazyObject<int[]> backwardInfo;

    /**
     * Constructs a Sunday Quick searcher given a {@link SequenceMatcher}
     * to search for.
     * 
     * @param sequence The sequence to search for.
     */
    public SundayQuickSearcher(final SequenceMatcher sequence) {
        super(sequence);
        forwardInfo = new ForwardSearchInfo();
        backwardInfo = new BackwardSearchInfo();
    }


    /**
     * {@inheritDoc}
     */    
    @Override
    public int searchForwards(final byte[] bytes, final int fromPosition, final int toPosition) {
        
        // Get the objects needed to search:
        final int[] safeShifts = forwardInfo.get();
        final SequenceMatcher sequence = getMatcher();
        
        // Calculate safe bounds for the search:
        final int length = sequence.length();
        final int finalPosition = bytes.length - length;
        final int lastLoopPosition = finalPosition - 1;
        final int lastPosition = toPosition < lastLoopPosition?
                                 toPosition : lastLoopPosition;
        int searchPosition = fromPosition > 0?
                             fromPosition : 0;

        // Search forwards.  The loop does not check for the final
        // position, as we shift on the byte after the sequence.
        while (searchPosition <= lastPosition) {
            if (sequence.matchesNoBoundsCheck(bytes, searchPosition)) {
                return searchPosition;
            }
            searchPosition += safeShifts[bytes[searchPosition + length] & 0xFF];
        }
        
        // Check the final position if necessary:
        if (searchPosition == finalPosition && 
            toPosition     >= finalPosition &&
            sequence.matches(bytes, finalPosition)) {
            return finalPosition;
        }

        return NOT_FOUND;
    }        
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final long doSearchForwards(final Reader reader, 
            final long fromPosition, final long toPosition ) throws IOException {
        
        // Get the objects needed to search:
        final int[] safeShifts = forwardInfo.get();
        final SequenceMatcher sequence = getMatcher();
        
        // Initialise window search.
        // If there is no window immediately after the sequence,
        // then there is no match, since this is only invoked if the 
        // sequence is already crossing into another window.        
        final int length = sequence.length();
        long searchPosition = fromPosition;
        Window window = reader.getWindow(searchPosition + length);
        
        // While there is a window to search in:
        while (window != null) {
            
            // Initialise array search:
            final byte[] array = window.getArray();
            final int arrayStartPosition = reader.getWindowOffset(searchPosition + length);
            final int arrayEndPosition = window.length() - 1;
            final long distanceToEnd = toPosition - window.getWindowPosition();
            final int finalPosition = distanceToEnd < arrayEndPosition?
                                (int) distanceToEnd : arrayEndPosition;
            int arraySearchPosition = arrayStartPosition;
            
            // Search fowards in the array using the reader interface to match.
            // The loop does not check the final position, as we shift on the byte
            // after it.
            while (arraySearchPosition < finalPosition) {
                if (sequence.matches(reader, searchPosition)) {
                    return searchPosition;
                }
                final int shift = safeShifts[array[arraySearchPosition] & 0xFF];
                searchPosition += shift;
                arraySearchPosition += shift;
            }

            // Check final position if necessary:
            if (arraySearchPosition == finalPosition &&
                toPosition          >= searchPosition &&
                sequence.matches(reader, searchPosition)) {
                return searchPosition;
            }
            
            // If the search position is now past the last search position, we're finished:
            if (searchPosition > toPosition) {
                return NOT_FOUND;
            }
            
            // Otherwise, get the next window.  The search position plus the 
            // length is guaranteed to be in another window at this point.            
            window = reader.getWindow(searchPosition + length);
        }

        return NOT_FOUND;
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public int searchBackwards(final byte[] bytes, final int fromPosition, final int toPosition) {
        
        // Get objects needed to search:
        final int[] safeShifts = backwardInfo.get();
        final SequenceMatcher sequence = getMatcher();
        
        // Calculate safe bounds for the search:
        final int lastLoopPosition = toPosition > 1?
                                     toPosition : 1;
        final int firstPossiblePosition = bytes.length - sequence.length();
        int searchPosition = fromPosition < firstPossiblePosition ?
                             fromPosition : firstPossiblePosition;
        
        // Search backwards.  The loop does not check the
        // first position in the array, because we shift on the byte
        // immediately before the current search position.
        while (searchPosition >= lastLoopPosition) {
            if (sequence.matchesNoBoundsCheck(bytes, searchPosition)) {
                return searchPosition;
            }
            searchPosition -= safeShifts[bytes[searchPosition - 1] & 0xFF];             
        }
        
        // Check for first position if necessary:
        if (searchPosition == 0 &&
            toPosition < 1 &&
            sequence.matches(bytes, 0)) {
            return 0;
        }

        return NOT_FOUND;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final long doSearchBackwards(final Reader reader, 
            final long fromPosition, final long toPosition ) throws IOException {
        
        // Get the objects needed to search:
        final int[] safeShifts = forwardInfo.get();
        final SequenceMatcher sequence = getMatcher();
        
        // Initialise window search.
        // If there is no window immediately before the sequence,
        // then there is no match, since this is only invoked if the 
        // sequence is already crossing into another window.        
        long searchPosition = fromPosition;
        Window window = reader.getWindow(searchPosition - 1);
        
        // While there is a window to search in:
        while (window != null) {
            
            // Initialise array search:
            final byte[] array = window.getArray();
            final int arrayStartPosition = reader.getWindowOffset(searchPosition - 1);
            final long distanceToEnd = toPosition - window.getWindowPosition();
            final int finalPosition = distanceToEnd > 0?
                                (int) distanceToEnd : 0;
            int arraySearchPosition = arrayStartPosition;
            
            // Search backwards in the array using the reader interface to match.
            // The loop does not check the final position, as we shift on the byte
            // before it.
            while (arraySearchPosition > finalPosition) {
                if (sequence.matches(reader, searchPosition)) {
                    return searchPosition;
                }
                final int shift = safeShifts[array[arraySearchPosition] & 0xFF];
                searchPosition -= shift;
                arraySearchPosition -= shift;
            }

            // Check final position if necessary:
            if (arraySearchPosition == finalPosition &&
                toPosition          <= searchPosition &&
                sequence.matches(reader, searchPosition)) {
                return searchPosition;
            }
            
            // If the search position is now past the last search position, we're finished:
            if (searchPosition < toPosition) {
                return NOT_FOUND;
            }
            
            // Otherwise, get the next window.  The search position minus one 
            // is guaranteed to be in another window at this point.            
            window = reader.getWindow(searchPosition - 1);
        }
        
        return NOT_FOUND;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareForwards() {
        forwardInfo.get();
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareBackwards() {
        backwardInfo.get();
    }

  
    private class ForwardSearchInfo extends LazyObject<int[]> {

        public ForwardSearchInfo() {
        }

        /**
         * Calculates the safe shifts to use if searching forwards.
         * A safe shift is either the length of the sequence plus one, if the
         * byte does not appear in the {@link SequenceMatcher}, or
         * the shortest distance it appears from the end of the matcher.
         */
        @Override
        protected int[] create() {
            // First set the default shift to the length of the sequence plus one.
            final int[] shifts = new int[256];
            final SequenceMatcher theMatcher = getMatcher();
            final int numBytes = theMatcher.length();
            Arrays.fill(shifts, numBytes + 1);

            // Now set specific byte shifts for the bytes actually in
            // the sequence itself.  The shift is the distance of each character
            // from the end of the sequence, where the last position equals 1.
            // Each position can match more than one byte (e.g. if a byte class appears).
            for (int sequenceByteIndex = 0; sequenceByteIndex < numBytes; sequenceByteIndex++) {
                final SingleByteMatcher aMatcher = theMatcher.getMatcherForPosition(sequenceByteIndex);
                final byte[] matchingBytes = aMatcher.getMatchingBytes();
                final int distanceFromEnd = numBytes - sequenceByteIndex;
                for (final byte b : matchingBytes) {
                    shifts[b & 0xFF] = distanceFromEnd;
                }
            }

            return shifts;
        }
    }
    
    
    private class BackwardSearchInfo extends LazyObject<int[]> {

        public BackwardSearchInfo() {
        }
        
        /**
         * Calculates the safe shifts to use if searching backwards.
         * A safe shift is either the length of the sequence plus one, if the
         * byte does not appear in the {@link SequenceMatcher}, or
         * the shortest distance it appears from the beginning of the matcher.
         */        
        @Override
        protected int[] create() {
            // First set the default shift to the length of the sequence
            // (negative if search direction is reversed)
            final int[] shifts = new int[256];
            final SequenceMatcher theMatcher = getMatcher();
            final int numBytes = theMatcher.length();
            Arrays.fill(shifts, numBytes + 1);

            // Now set specific byte shifts for the bytes actually in
            // the sequence itself.  The shift is the distance of each character
            // from the start of the sequence, where the first position equals 1.
            // Each position can match more than one byte (e.g. if a byte class appears).
            for (int sequenceByteIndex = numBytes - 1; sequenceByteIndex >= 0; sequenceByteIndex++) {
                final SingleByteMatcher aMatcher = theMatcher.getMatcherForPosition(sequenceByteIndex);
                final byte[] matchingBytes = aMatcher.getMatchingBytes();
                final int distanceFromStart = sequenceByteIndex + 1;
                for (final byte b : matchingBytes) {
                    shifts[b & 0xFF] = distanceFromStart;
                }
            }

            return shifts;
        }
    }
    
}