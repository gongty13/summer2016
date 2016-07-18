package org.apache.hadoop.BHW;

import org.apache.hadoop.BHW.Scene.ObjectInScene;

public class Vec3D {
	public static final Vec3D black = new Vec3D(0,0,0);	
	public double x,y,z;
	public double len2(){
		return x*x+y*y+z*z;
	}
	public double len(){
		return Math.sqrt(x*x+y*y+z*z);
	}
	public Vec3D normalize() {
		return Vec3D.mul(this.len(), this);
	}
	public Vec3D() {
		x=y=z=0;
	}
	public Vec3D(String str)
	{
		String[] strs = str.split(",");
		x = Double.parseDouble(strs[0]);
		y = Double.parseDouble(strs[1]);
		z = Double.parseDouble(strs[2]);
	}
	public Vec3D(double x,double y, double z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vec3D(Vec3D v){
		x = v.x;
		y = v.y;
		z = v.z;
	}
	public static Vec3D add(Vec3D a, Vec3D b){
		return new Vec3D(a.x+b.x,a.y+b.y,a.z+b.z);
	}	
	public static Vec3D minus(Vec3D a, Vec3D b){
		return new Vec3D(a.x-b.x,a.y-b.y,a.z-b.z);
	}
	public static Vec3D mul(double k, Vec3D a){
		return new Vec3D(k*a.x,k*a.y,k*a.z);
	}
	public static double dot(Vec3D a, Vec3D b){
		return a.x*b.x+a.y*b.y+a.z*b.z;
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
			double c2t1 = Vec3D.dot(direction, normal)/normal.len()/direction.len();
			double n1 = 1;double n2 = 1;
			if(nowIn == obj )
			{
				if(obj != null)
					n1 = obj.n; 
			}
			else if(obj != null){
				n2 = obj.n;
			}
			double c2t2 = 1-(1-c2t1)*n1*n1/(n2*n2);
//			n1^2(1-c2t1)=n2^2(1-c2t2)
			if(c2t2<=0)
				return null;
			double ct2 = Math.sqrt(c2t2);
			Vec3D _n =Vec3D.mul(n1/n2, 
					Vec3D.minus(direction,
							Vec3D.mul(
									Vec3D.dot(normal, direction)/normal.len2()
									, normal)
					));
			Vec3D _t = Vec3D.mul(Math.sqrt(direction.len2()/normal.len2())*ct2,
					Vec3D.dot(normal, direction)>0?normal:Vec3D.mul(-1, normal)
							);
//				               -(N*dir<0?N:N*(-1))*sqrt(dir.len2()/N.len2())*cosThetaT);
			return new Ray(nStart, Vec3D.minus(_n, _t), obj == nowIn?null:obj);
		}
	}
}
