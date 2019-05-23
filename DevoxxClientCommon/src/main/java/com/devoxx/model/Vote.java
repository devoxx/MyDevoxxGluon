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
package com.devoxx.model;

import java.util.UUID;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Vote {

	public static final String EMPTY_DELIVERY = "none";
	
    private String uuid;
    private String talkId;

    private final IntegerProperty value = new SimpleIntegerProperty(0);
    private final StringProperty delivery = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();
    private final StringProperty other = new SimpleStringProperty();

    public Vote() {
    }

    public Vote(String talkId) {
        this(talkId, -1, "", "", "");
    }

    public Vote(String talkId, int value, String delivery, String content, String other) {
        this.uuid = UUID.randomUUID().toString();
        this.talkId = talkId;
        this.value.set(value);
        this.delivery.set(delivery);
        this.content.set(content);
        this.other.set(other);
    }

    public String getUuid() {
        return uuid;
    }

    public String getTalkId() {
        return talkId;
    }

    public void setValue(int value) { this.value.set(value); }
    public int getValue() {
        return value.get();
    }
    public IntegerProperty valueProperty() { return value; }

    public String getDelivery() {
        return delivery.get() != null && delivery.get().trim().length() > 0 ? delivery.get() : EMPTY_DELIVERY;
    }
    public void setDelivery(String delivery) { this.delivery.set(delivery); }
    public StringProperty deliveryProperty() { return delivery; }

    public String getContent() {
        return content.get() != null ? content.get() : "";
    }
    public void setContent(String content) { this.content.set(content); }
    public StringProperty contentProperty() { return content; }

    public String getOther() {
        return other.get() != null? other.get() : "";
    }
    public void setOther(String other) { this.other.set(other); }
    public StringProperty otherProperty() { return other; }

    @Override
    public String toString() {
        return "Vote{" +
                "talkId='" + talkId + '\'' +
                ", value=" + value +
                ", delivery='" + delivery + '\'' +
                ", content='" + content + '\'' +
                ", other='" + other + '\'' +
                '}';
    }
}
