package org.apache.hadoop.BHW;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.BHW.physics.ParticleSystem;

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
			if(s.length==2)
				map.put(s[0], s[1]);
		}
		radius = map.containsKey("r")?Float.parseFloat(map.get("r")):100;
		center = map.containsKey("c")?new Vec3D(Float.parseFloat(map.get("c")), 0, 0):new Vec3D();
		velocity = map.containsKey("v")?new Vec3D(Float.parseFloat(map.get("v")),0,0):new Vec3D();
	}
	public void run(float acceleration)
	{
		center.add(Vec3D.mul(velocity, ParticleSystem.deltaT*0.5f));
		velocity.x += acceleration * ParticleSystem.deltaT;
		center.add(Vec3D.mul(velocity, ParticleSystem.deltaT*0.5f));
		System.out.println(""+acceleration+" "+velocity.x+" "+center.x);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append("r:"+radius);
		sb.append("\tc:"+center.x);
		sb.append("\tv:"+velocity.x);
		return sb.toString();
	}
}
