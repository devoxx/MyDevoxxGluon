package com.devoxx.model;

public class News {
    
    private String uuid;
    private long creationDate;
    private String title;
    private String content;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "News{" +
                "uuid='" + uuid + '\'' +
                ", creationDate=" + creationDate +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}

