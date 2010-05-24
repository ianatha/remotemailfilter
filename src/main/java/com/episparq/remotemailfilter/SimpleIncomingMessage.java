package com.episparq.remotemailfilter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;

class SimpleIncomingMessage implements IncomingMessage {
	private Message m;
	
	public String to() {
		try {
			
		String[] tos = m.getHeader("To");
		if (tos.length != 1) {
			throw new RuntimeException("1!");
		} 
		return tos[0];

		} catch (MessagingException m) {
			
		}
	return "";
	}
	
	public SimpleIncomingMessage(Message m) {
		this.m = m;
	}

	public void clear() {
		// TODO Auto-generated method stub
		
	}

	public boolean containsKey(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	public Set entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object get(Object key) {
		try {
			return m.getHeader((String) key);
		} catch (MessagingException e) {
			return null;
		}
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	public Set keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object put(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	public void putAll(Map t) {
		// TODO Auto-generated method stub
		
	}

	public Object remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Collection values() {
		// TODO Auto-generated method stub
		return null;
	}
}
