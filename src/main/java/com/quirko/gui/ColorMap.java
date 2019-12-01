package com.quirko.gui;

import java.util.HashMap;


public class ColorMap {
	HashMap<Integer,Character> map = new HashMap<Integer,Character>();
	
	public ColorMap() {
		map.put(0,(char)0);//black
		map.put(1, 'c');//cyan
		map.put(2, 'm');//mangenta
     	map.put(3, 'g');//green
    	map.put(4, 'y');//yellow
    	map.put(5, 'r');//red
    	map.put(6, 'b');//blue
    	map.put(7, 'p');//purple
    	map.put(8, 'w');//white
	}
	
	public byte getColor(int color) {
		return (byte)Character.toLowerCase(map.get(color));
	}
}
