package com.m1pay.nfsound;

public class NFSoundEngine {

	 static {
		    try {
		        java.lang.System.loadLibrary("nfsound");
		    } catch (UnsatisfiedLinkError e) {
		        java.lang.System.err.println("native code library failed to load.\n" + e);
		        java.lang.System.exit(1);
		    }
		  }

		  public final static native void start_process();
		  public final static native void stop_process();
		  public final static native int  is_recording();
		  
		  // register java callback func via obj and reflection
		  public final static native void   register_callback(Object obj);
		  public final static native byte[] encode_string(String str);
		  public final static native void   play_string(String str);
	
}
