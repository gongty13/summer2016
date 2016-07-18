package org.apache.hadoop.BHW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.BHW.Scene.ObjectInScene;
import org.apache.hadoop.BHW.Vec3D.Ray;



public class Scene {
	public Camera camera;
	public Vec3D environment = new Vec3D(0,0,0);
	public Collection<Light> lights =  new ArrayList<Light>();
	public Collection<ObjectInScene> objs =  new ArrayList<ObjectInScene>();
	public void parse(String line)
	{
		String[] strs = line.split("\\s+");
		Map<String, String> map = new HashMap<String, String>();
		for(String str:strs)
		{
			String[] l = str.split(":");
			if(l.length == 2)
				map.put(l[0], l[1]);			
		}
		if (strs[0].equals("Camera")){
			camera = new Camera(map);
		}else if (strs[0].equals("ParallelLight")){
			lights.add(new ParallelLight(map));
		}else if (strs[0].equals("Ball")){
			objs.add(new Ball(map));
		}else if (strs[0].equals("Plane")){
			objs.add(new Plane(map));
		}
	}
	public class Camera{
		public Ray viewPoint;
		public Vec3D screenCenter;
		public Vec3D screenUp;
		public Vec3D screenRight;
		public double screenWidth;
		public double screenHeight;
		public int pictureWidth;
		public int pictureHeight;
		public Camera(Map<String, String> map){
			viewPoint = new Ray(new Vec3D(map.get("cameraPoint")),
					new Vec3D(map.get("cameraDirection")),
					null);
			screenCenter = new Vec3D(map.get("screenCenter"));
			screenUp = new Vec3D(map.get("screenUp")).normalize();
			screenWidth = Double.parseDouble(map.get("screenWidth"));
			screenHeight = Double.parseDouble(map.get("screenHeight"));
			screenRight = Vec3D.cross(viewPoint.direction, screenUp).normalize();
		}
		public Ray emitRay(int x,int y) {
			double _x = (double)x/pictureWidth-0.5;
			double _y = 0.5-(double)y/pictureHeight;
			Vec3D tmp = screenCenter;
			tmp = Vec3D.add(tmp, 
					Vec3D.mul(_y/screenUp.len(), screenUp)					
					);
			tmp = Vec3D.add(tmp, 
					Vec3D.mul(_x/screenRight.len(), screenRight)					
					);
			return new Ray(viewPoint.start, Vec3D.minus(tmp, viewPoint.start), null);
		}
	}
	public abstract class Light{
		public Vec3D color;
		public abstract Ray getRay(Vec3D toPoint);
	}
	public class ParallelLight extends Light{
		static final double inf = 1e10;
		public Vec3D direction;
		public ParallelLight(Map<String, String> map) {
			color = new Vec3D(map.get("color"));
			direction = new Vec3D(map.get("direction"));			
		}
		@Override
		public Ray getRay(Vec3D toPoint) {
			return new Ray(Vec3D.add(toPoint, Vec3D.mul(-inf, direction))	, direction, null);
		}
	}
	public abstract class ObjectInScene
	{
		public double n;
		public double refractive;//折射系数
		public double reflect;//反射系数
		public double diffuse;//漫反射系数
		public Vec3D color;
		public void init(Map<String, String> map){
			n = Double.parseDouble(map.get("n"));
			refractive = Double.parseDouble(map.get("refractive"));
			reflect = Double.parseDouble(map.get("reflect"));
			diffuse = Double.parseDouble(map.get("diffuse"));
			color = new Vec3D(map.get("color"));
		}
		public abstract double intersect(Ray ray);
		public abstract Vec3D getNormal(Vec3D interPoint);
	}
	public class Ball extends ObjectInScene{
		public Vec3D center;
		public double radius;
		@Override
		public double intersect(Ray ray) {
			double A = ray.direction.len2();
			double B = Vec3D.dot(ray.direction,Vec3D.minus(ray.start, center));
			double C = Vec3D.minus(ray.start, center).len2() - radius*radius;
			double delta = B*B - 4*A*C;
			if(delta<=0)
				return Double.MAX_VALUE;
			double x1 = (-Math.sqrt(delta)-B)/(2*A);
			double x2 = (Math.sqrt(delta)-B)/(2*A);
			if(ray.nowIn == this)
			{
				if(x1>0)
					return x1;
				if(x2>0)
					return x2;
				return Double.MAX_VALUE;
			}else{
				if(x1>0)
					return x1;
				return Double.MAX_VALUE;
			}
		}
		public Ball(Map<String, String> map)
		{
			super.init(map);
			center = new Vec3D(map.get("center"));
			radius = Double.parseDouble(map.get("radius"));
		}
		@Override
		public Vec3D getNormal(Vec3D interPoint) {
			return Vec3D.minus(interPoint, center);
		}
		
	}
	public class Plane extends ObjectInScene{
		public Vec3D normal;//法向量
		public Vec3D p;
		public Plane(Map<String, String> map)
		{
			super.init(map);
			normal = new Vec3D(map.get("normal"));
			p = new Vec3D(map.get("p"));
		}
		@Override
		public double intersect(Ray ray) {
			if(Vec3D.dot(ray.direction, normal)==0)
				return Double.MAX_VALUE;
			double k = Vec3D.dot(p, normal)/Vec3D.dot(ray.direction, normal);
			if (k>0)
				return k;
			return Double.MAX_VALUE;
		}
		@Override
		public Vec3D getNormal(Vec3D interPoint) {
			return normal;
		}
	}
	public ObjectInScene findObj(Ray ray)
	{
		double dis = Double.MAX_VALUE;
		ObjectInScene ret = null;
		for(ObjectInScene obj:objs)
		{
			double tmp = obj.intersect(ray);
			if(tmp<dis)
			{
				dis = tmp;
				ret = obj;
			}
		}
		return ret;
	}
	public Vec3D calcLight(Ray ray, Vec3D nStart, ObjectInScene obj) {
		Vec3D ret = Vec3D.black;
		Vec3D normal = obj.getNormal(nStart);
		for(Light light:lights)
		{
			
		}
		return null;
	}
}
