package com.dll;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Structure;

public class Box extends Structure{
	public static class ByReference extends Box implements Structure.ByReference{			
	}
	
	public static class ByValue extends Box implements Structure.ByValue{
	}
	
	public int xmin;
	public int ymin;
	public int xmax;
	public int ymax;
	public float prob;
	public int obj_id;
	public int track_id;
	public int frames_counter;
	public int is_warning;
	
	public void initBox(int xmin, int ymin, int xmax, int ymax, float prob, int obj_id, int track_id, int frames_counter,
			int is_warning) {
//		super();
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.prob = prob;
		this.obj_id = obj_id;
		this.track_id = track_id;
		this.frames_counter = frames_counter;
		this.is_warning = is_warning;
	}

	@Override
	protected List<String> getFieldOrder() {
		List<String> Field = new ArrayList<String>();
		Field.add("xmin");
		Field.add("ymin");
		Field.add("xmax");
		Field.add("ymax");
		Field.add("prob");
		Field.add("obj_id");
		Field.add("track_id");
		Field.add("frames_counter");
		Field.add("is_warning");

		return Field;
	}
}	