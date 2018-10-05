package com.xeomar.snare;

import java.util.ArrayList;
import java.util.List;

public class Jar {

	private String name;

	private List<String> modules = new ArrayList<>();

	public String getName() {
		return name;
	}

	public Jar setName( String name ) {
		this.name = name;
		return this;
	}

	public List<String> getModules() {
		return modules;
	}

	public Jar setModules( List<String> modules ) {
		this.modules = modules;
		return this;
	}

}
