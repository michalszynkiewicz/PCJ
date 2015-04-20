package org.pcj.internal.faulttolerance;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 2/1/15
 * Time: 10:30 PM
 */
public class NodeFailedException extends RuntimeException {
    public NodeFailedException() {}
    public NodeFailedException(Exception cause) {
        super(cause);
    }
    // mstodo fill with failed nodes ids and THREADS ids
}
