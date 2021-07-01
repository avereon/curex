package com.avereon.curex;

import java.util.Objects;
import java.util.Set;

public class ModuleRef {

	private String name;

	private String module;

	private String jar;

	private Set<ModuleRef> dependencies;

	public String getName() {
		return name;
	}

	public ModuleRef setName( String name ) {
		this.name = name;
		return this;
	}

	public String getModule() {
		return module;
	}

	public ModuleRef setModule( String module ) {
		this.module = module;
		return this;
	}

	public String getJar() {
		return jar;
	}

	public ModuleRef setJar( String jar ) {
		this.jar = jar;
		return this;
	}

	public Set<ModuleRef> getDependencies() {
		return dependencies;
	}

	public ModuleRef setDependencies( Set<ModuleRef> dependencies ) {
		this.dependencies = dependencies;
		return this;
	}

	@Override
	public boolean equals( Object object ) {
		if( this == object ) return true;
		if( object == null || getClass() != object.getClass() ) return false;
		ModuleRef that = (ModuleRef)object;
		return Objects.equals( name, that.name ) && Objects.equals( module, that.module ) && Objects.equals( jar, that.jar );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, module, jar );
	}
}
