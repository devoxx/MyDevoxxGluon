package com.devoxx.model;

import java.util.Objects;

public class Level {

	private String name;
	private int priority;

	public Level() {
	}

	public Level(String name, int priority) {
		super();
		this.name = name;
		this.priority = priority;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Level image = (Level) o;
		return Objects.equals(name, image.name) && (priority == image.priority);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, priority);
	}

}
