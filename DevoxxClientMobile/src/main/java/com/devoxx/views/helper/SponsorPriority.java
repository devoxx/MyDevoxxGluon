/**
 * Copyright (c) 2016, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 *    or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.devoxx.views.helper;

import javafx.css.PseudoClass;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SponsorPriority {

    private static Map<Integer, PseudoClass> priorityPseudoClassMap = new HashMap<>();
    private static List<String> classes = Arrays.asList("priority-color0",
            "priority-color1", "priority-color2", "priority-color3",
            "priority-color4"
    );
    private static int index = 0;

    public static PseudoClass fetchPseudoClassForPriority(Integer priorityId) {
        PseudoClass pseudoClass = priorityPseudoClassMap.get(priorityId);
        if (pseudoClass == null) {
            if (index > classes.size() - 1) {
                index = 0; // exhausted all colors, re-use
            }
            pseudoClass = PseudoClass.getPseudoClass(classes.get(index++));
            priorityPseudoClassMap.put(priorityId, pseudoClass);
        }
        return pseudoClass;
    }

    public static String fetchStyleClassForPriority(Integer priorityId) {
        return fetchPseudoClassForPriority(priorityId).getPseudoClassName();
    }
}
