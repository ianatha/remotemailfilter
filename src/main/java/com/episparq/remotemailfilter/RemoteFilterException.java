package com.episparq.remotemailfilter;

public class RemoteFilterException extends Exception {
	private static final long serialVersionUID = -7220480237277334267L;

	public RemoteFilterException(String message, Throwable cause) {
		super(message, cause);
	}

	public RemoteFilterException(String message) {
		super(message);
	}

	public RemoteFilterException(Throwable cause) {
		super(cause);
	}
}
