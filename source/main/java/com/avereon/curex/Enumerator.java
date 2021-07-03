package com.avereon.curex;

import java.util.Enumeration;
import java.util.Iterator;

public class Enumerator<T> implements Iterator<T>, Iterable<T> {

	private final Enumeration<T> enumeration;

	public Enumerator( Enumeration<T> enmueration){
		this.enumeration = enmueration;
	}

	public boolean hasNext(){
		return enumeration.hasMoreElements();
	}

	public T next(){
		return enumeration.nextElement();
	}

	public void remove(){
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
		return this;
	}

}