/**
 * Copyright (c) 2016, 2018 Gluon Software
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

import com.devoxx.util.DevoxxBundle;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class Placeholder extends PlaceholderBase {

    private static final String FAILURE_PLACEHOLDER_TITLE = DevoxxBundle.getString("OTN.PLACEHOLDER.FAILED.TITLE");

    public Placeholder(String messageText, MaterialDesignIcon image) {
        this(null, messageText, image);
    }

    public Placeholder(String titleText, String messageText, MaterialDesignIcon image) {
        getStyleClass().add("placeholder");
        getChildren().add(getNodeFromIcon(image));

        if (titleText != null && !titleText.isEmpty()) {
            Label title = new Label(titleText);
            title.getStyleClass().add("title");
            getChildren().add(title);
        }

        this.message.setText(messageText);
        getChildren().add(message);
    }

    /**
     * Creates a place holder with the following attributes:
     * 1. Title - Oh no!
     * 2. Message - Retrieval of {0} failed
     * 3. A warning graphic
     * 4. Any additional nodes passed to the method
     * @param text Text to replace {0} in message
     * @param nodes Adds nodes as the children of placeholder
     * @return A Placeholder with above nodes.
     */
    public static Placeholder failure(String text, Node... nodes) {
        final String message = DevoxxBundle.getString("OTN.PLACEHOLDER.FAILED.MESSAGE", text);
        final Placeholder failurePlaceholder = new Placeholder(FAILURE_PLACEHOLDER_TITLE, message, MaterialDesignIcon.WARNING);
        for (Node node : nodes) {
            if (node != null) {
                failurePlaceholder.getChildren().add(node);
            }
        }
        return failurePlaceholder;
    }

    /**
     * Creates a place holder with the following attributes:
     * 1. Title -
     * 2. Message - There are no {0} for {1} yet.
     * 3. A broken image graphic
     * @param text Text to replace {0} in message
     * @param conferenceName Text to replace {1} in message
     * @return A Placeholder with above nodes.
     */
    public static Placeholder empty(String text, String conferenceName) {
        final String message = DevoxxBundle.getString("OTN.PLACEHOLDER.EMPTY.MESSAGE", text, conferenceName);
        // TODO: Use an image instead of Material Design Icon
        // https://material.io/design/communication/empty-states.html#
        return new Placeholder(message, MaterialDesignIcon.BROKEN_IMAGE);
    }
}

