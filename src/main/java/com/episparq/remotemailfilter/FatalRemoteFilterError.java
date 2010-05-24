package com.episparq.remotemailfilter;

public class FatalRemoteFilterError extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public FatalRemoteFilterError(String message) {
		super(message);
	}

	public FatalRemoteFilterError(Throwable cause) {
		super(cause);
	}

	public FatalRemoteFilterError(String message, Throwable cause) {
		super(message, cause);
	}

}
