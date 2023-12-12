package com.avereon.curex;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleJar {

	private String name;

	private String module;

	private boolean ignoreMissing;

	private final List<String> modules = new ArrayList<>();

	private final Set<String> mergeJars = new HashSet<>();

	private File file;

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
		this.modules.addAll( modules );
		return this;
	}

	public Set<String> getMergeJars() {
		return mergeJars;
	}

	public ModuleJar setMergeJars( Set<String> mergeJars ) {
		this.mergeJars.addAll( mergeJars );
		return this;
	}

	public boolean isIgnoreMissing() {
		return ignoreMissing;
	}

	public void setIgnoreMissing( boolean ignoreMissing ) {
		this.ignoreMissing = ignoreMissing;
	}

	public File getFile() {
		return file;
	}

	public ModuleJar setFile( File file ) {
		this.file = file;
		return this;
	}
}
