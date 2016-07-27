package org.apache.hadoop.BHW;

import org.apache.hadoop.BHW.render.Scene;
import org.apache.hadoop.BHW.render.Scene.ObjectInScene;

public class Vec3D {
	public static final Vec3D black = new Vec3D(0,0,0);
	public static final Vec3D white = new Vec3D(1,1,1);	
	public float x,y,z;
	public float len2(){
		return x*x+y*y+z*z;
	}
	public float len(){
		return (float)Math.sqrt(x*x+y*y+z*z);
	}
	public Vec3D normalize() {
		return Vec3D.div(this, this.len());
	}
	public Vec3D() {
		x=y=z=0;
	}
	public Vec3D(String str)
	{
		String[] strs = str.split(",");
		x = Float.parseFloat(strs[0]);
		y = Float.parseFloat(strs[1]);
		z = Float.parseFloat(strs[2]);
	}
	@Override
	public String toString() {
		return ""+x+','+y+','+z;
	}
	public Vec3D(float x,float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vec3D(Vec3D v){
		x = v.x;
		y = v.y;
		z = v.z;
	}
	public Vec3D clone(){
		return new Vec3D(this);
	}
	public static Vec3D add(Vec3D a, Vec3D b){
		return new Vec3D(a.x+b.x,a.y+b.y,a.z+b.z);
	}
	public Vec3D add(Vec3D a){
		x += a.x;
		y += a.y;
		z += a.z;
		return this;
	}
	public static Vec3D minus(Vec3D a, Vec3D b){
		return new Vec3D(a.x-b.x,a.y-b.y,a.z-b.z);
	}
	public static float dis(Vec3D a, Vec3D b){
		return Vec3D.minus(a, b).len();
	}
	public static Vec3D mul(float k, Vec3D a){
		return new Vec3D(k*a.x,k*a.y,k*a.z);
	}
	public  Vec3D mul(float k){
		x *= k;
		y *= k;
		z *= k;
		return this;
	}
	public static Vec3D mul(Vec3D a, float k){
		return new Vec3D(k*a.x,k*a.y,k*a.z);
	}
	public static Vec3D div( Vec3D a, float k){
		return new Vec3D(a.x/k, a.y/k, a.z/k);
	}
	public Vec3D div( float k){
		x /= k;
		y /= k;
		z /= k;
		return this;
	}
	public static float dot(Vec3D a, Vec3D b){
		return a.x*b.x+a.y*b.y+a.z*b.z;
	}
	public float dot(Vec3D a){
		return x*a.x+y*a.y+z*a.z;
	}
	public static Vec3D cross(Vec3D a, Vec3D b){
		return new Vec3D(a.y*b.z-a.z*b.y, 
				a.z*b.x-a.x*b.z,
				a.x*b.y-a.y*b.x);
	}
	public static Vec3D colorMul(Vec3D a, Vec3D b){
		return new Vec3D(a.x*b.x, a.y*b.y, a.z*b.z);
	}
	public static class Ray{
		public Vec3D start;
		public Vec3D direction;
		public Scene.ObjectInScene nowIn;
		public Ray(Vec3D start, Vec3D direction, Scene.ObjectInScene nowIn){
			this.start = start;
			this.direction = direction;
			this.nowIn = nowIn;
		}
		@Override
		public String toString() {
			return "s:"+start.toString()+"\td:"+direction.toString();
		}
		public Vec3D online(float k){
			return Vec3D.add(start, Vec3D.mul(k, direction));
		}
		public Ray reflect(Vec3D nStart, Vec3D normal) {
			Vec3D nDirection = Vec3D.minus(direction,
					Vec3D.mul(
							Vec3D.dot(direction,
									normal)/normal.len2()*2,
							normal)
					);
			return new Ray(nStart, nDirection, nowIn);
		}
		public Ray refractive(Vec3D nStart, Vec3D normal, ObjectInScene obj) {
			float c2t1 = Vec3D.dot(direction, normal)/normal.len()/direction.len();
			float n1 = 1;float n2 = 1;
			if(nowIn == obj )
			{
				if(obj != null)
					n1 = obj.n; 
			}
			else if(obj != null){
				n2 = obj.n;
			}
			float c2t2 = 1-(1-c2t1)*n1*n1/(n2*n2);
//			n1^2(1-c2t1)=n2^2(1-c2t2)
			if(c2t2<=0)
				return null;
			float ct2 = (float)Math.sqrt(c2t2);		
			Vec3D _n =Vec3D.mul(n1/n2, 
					Vec3D.minus(direction,
							Vec3D.mul(
									Vec3D.dot(normal, direction)/normal.len2()
									, normal)
					));
			Vec3D _t = Vec3D.mul((float)Math.sqrt(direction.len2()/normal.len2())*ct2,
					Vec3D.dot(normal, direction)>0?normal:Vec3D.mul(-1, normal)
							);
//				               -(N*dir<0?N:N*(-1))*sqrt(dir.len2()/N.len2())*cosThetaT);
			return new Ray(nStart, Vec3D.minus(_n, _t), obj == nowIn?null:obj);
		}
	}
}
