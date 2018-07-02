/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj;

import java.util.function.BinaryOperator;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 4/1/18
 */
public class StreamUtils {
    public static final BinaryOperator<Boolean> ONE_MATCHES = BinaryOperator.maxBy(Boolean::compareTo);
}
