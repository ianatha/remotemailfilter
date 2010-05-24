package com.episparq.remotemailfilter;

import java.util.regex.Matcher;

import groovy.lang.Closure;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

public class Action {
	public static ActionResult nop() {
		return new ActionResult() {
			public void execute(Message m) {
				// nop
			}
		};
	}
	
	public static void RegExCase(Matcher m, Closure c) {
		if (m.find()) {
			c.call(m);
		}
	}
	
	public static ActionResult move(final String destinationFolder) {
		return new ActionResult() {
			
			public void execute(Message m) {
				try {
					System.out.println(m.getSubject());
				//	Message[] mm = new Message[] { m };
					//Folder f = m.getFolder();
					//f.copyMessages(mm, f.getFolder(destinationFolder));
			        //f.setFlags(mm, new Flags(Flags.Flag.DELETED), true);
			        //System.out.println(m.getSubject() + " -> " + destinationFolder);
				} catch (MessagingException e) {
					// ignore.
				}
			}
		};
	}
}
