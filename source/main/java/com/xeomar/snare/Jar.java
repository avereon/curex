package com.xeomar.snare;

public class Jar {

	private String name;

	private boolean standalone;

	public String getName() {
		return name;
	}

	public Jar setName( String name ) {
		this.name = name;
		return this;
	}

	public boolean isStandalone() {
		return standalone;
	}

	public Jar setStandalone( boolean standalone ) {
		this.standalone = standalone;
		return this;
	}
}
