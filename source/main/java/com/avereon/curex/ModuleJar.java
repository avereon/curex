package com.avereon.curex;

import java.util.ArrayList;
import java.util.List;

public class ModuleJar {

	private String name;

	private String module;

	private List<String> modules = new ArrayList<>();

	public String getName() {
		return name;
	}

	public ModuleJar setName( String name ) {
		this.name = name;
		return this;
	}

	public String getModule() {
		return module;
	}

	public ModuleJar setModule(String module) {
		this.module = module;
		return this;
	}

	public List<String> getModules() {
		return modules;
	}

	public ModuleJar setModules( List<String> modules ) {
		this.modules = modules;
		return this;
	}

}
