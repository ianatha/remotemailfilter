package com.episparq.remotemailfilter;

import javax.mail.Message;

public interface ActionResult {
	void execute(Message m);
}
