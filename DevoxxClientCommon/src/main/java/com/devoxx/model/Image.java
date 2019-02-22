package com.devoxx.model;

import java.util.Objects;

public class Image {
	
	private String src;
	private String alt;
	
	public Image() {
	}

	public Image(String src, String alt) {
		super();
		this.src = src;
		this.alt = alt;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getAlt() {
		return alt;
	}

	public void setAlt(String alt) {
		this.alt = alt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Image image = (Image) o;
		return Objects.equals(src, image.src) && Objects.equals(alt, image.alt);
	}
	
	
	@Override
	public int hashCode() {
		return Objects.hash(src, alt);
	}	

}
