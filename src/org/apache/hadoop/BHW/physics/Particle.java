package org.apache.hadoop.BHW.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.BHW.Vec3D;

public class Particle {
	private Vec3D oldPos;
	private Vec3D newPos;
	private Vec3D velocity;
	private Vec3D force;
	private Vec3D deltaP;
	private float lambda;
//	private float pConstraint;
	private ArrayList<Particle> neighbors;
//	private Cell cell;
	public Particle(String str)
	{
		String[] items = str.split("\\s+");
		Map<String, String> map = new HashMap<String, String>();
		for(String item:items)
		{
			String[] s = item.split(":");
			if(s.length==2)
				map.put(s[0], s[1]);
		}
		oldPos = new Vec3D(map.get("p"));
		lambda = map.containsKey("l")?Float.parseFloat(map.get("l")):0f;
		newPos =map.containsKey("n")?new Vec3D(map.get("n")): new Vec3D(0f, 0f, 0f);
		velocity = map.containsKey("v")?new Vec3D(map.get("v")):new Vec3D(0f, 0f, 0f);
		force =  map.containsKey("f")?new Vec3D(map.get("f")):new Vec3D(0f, 0f, 0f);
		deltaP = map.containsKey("d")?new Vec3D(map.get("d")): new Vec3D(0f, 0f, 0f);
		neighbors = new ArrayList<Particle>();
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append("p:"+oldPos.toString());
		sb.append("\tn:"+newPos.toString());
		sb.append("\tl:"+lambda);
		sb.append("\tv:"+velocity.toString());
		sb.append("\tf:"+force.toString());
		sb.append("\td:"+deltaP.toString());
		return sb.toString();
	}
	public Particle(Vec3D pos) {
		this.oldPos = pos;
		newPos = new Vec3D(0f, 0f, 0f);
		velocity = new Vec3D(0f, 0f, 0f);
		force = new Vec3D(0f, 0f, 0f);
		deltaP = new Vec3D(0f, 0f, 0f);
		neighbors = new ArrayList<Particle>();
	}
	
	public Vec3D getOldPos() {
		return oldPos;
	}

	public Vec3D getNewPos() {
		return newPos;
	}

	public Vec3D getVelocity() {
		return velocity;
	}

	public void setVelocity(Vec3D velocity) {
		this.velocity = velocity;
	}
	
	public Vec3D getForce() {
		return force;
	}
	
	public void setForce(float x, float y, float z) {
		force.x = x;
		force.y = y;
		force.z = z;
	}

	public ArrayList<Particle> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(ArrayList<Particle> neighbors) {
		this.neighbors = neighbors;
	}

//	public float getPConstraint() {
//		return pConstraint;
//	}
//
//	public void setPConstraint(float f) {
//		pConstraint = f;
//	}

	public void setNewPos(Vec3D v) {
		newPos = v;
	}
	
	public void setOldPos(Vec3D v) {
		oldPos = v;
	}

	public Vec3D getDeltaP() {
		return deltaP;
	}

	public void setDeltaP(Vec3D deltaP) {
		this.deltaP = deltaP;
	}

	public float getLambda() {
		return lambda;
	}

	public void setLambda(float lambda) {
		this.lambda = lambda;
	}
}
