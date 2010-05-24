package com.episparq.remotemailfilter;

import groovy.lang.Closure;

import java.util.regex.Pattern;

import javax.mail.Message;

public class Rule {
	public Rule(Closure condition, Closure action) {
		
	}
	
	public Rule(Condition condition, Action action) {
		
	}
	
	public Action evaluate(Message m) {
		return null;
	}
}
