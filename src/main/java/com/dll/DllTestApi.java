package com.dll;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
 
 
public interface DllTestApi extends Library {
 
//	public static DllTestApi dta = (DllTestApi)Native.loadLibrary("ConsoleApplication2", DllTestApi.class);
	
	DllTestApi INSTANCE = (DllTestApi)Native.load("ConsoleApplication2", DllTestApi.class);
	
	void setBox(Box.ByReference box, float xmin, float ymin, float xmax, float ymax, int type);
	
	Box.ByReference getBox( float xmin, float ymin, float xmax, float ymax, int type);
}
