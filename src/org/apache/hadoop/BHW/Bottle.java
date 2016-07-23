package org.apache.hadoop.BHW;

import java.util.HashMap;
import java.util.Map;

public class Bottle {
	public float radius;
	public Vec3D center;
	public Vec3D velocity;
	public Bottle(String str) {
		String[] items = str.split("\\s+");
		Map<String, String> map = new HashMap<String, String>();
		for(String item:items)
		{
			String[] s = item.split(":");
			map.put(s[0], s[1]);
		}
		radius = map.containsKey("r")?Float.parseFloat(map.get("r")):100;
		center = map.containsKey("c")?new Vec3D(map.get("c")):new Vec3D();
		velocity = map.containsKey("v")?new Vec3D(Float.parseFloat(map.get("v")),0,0):new Vec3D();
	}
	public void run(float acceleration)
	{
		velocity.x += acceleration;
		center.add(velocity);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append("r:"+radius);
		sb.append("\tc:"+center.toString());
		sb.append("\tv:"+velocity.x);
		return "";
	}
}
