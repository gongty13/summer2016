package org.apache.hadoop.BHW.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.BHW.Vec3D;
import org.apache.hadoop.BHW.Vec3D.Ray;

public class Scene {
	public Camera camera;
	public Vec3D environment = new Vec3D(0,0,0);
	public ArrayList<Light> lights =  new ArrayList<Light>();
	public ArrayList<ObjectInScene> objs =  new ArrayList<ObjectInScene>();
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
		}
		else if(strs[0].equals("environment"))
			environment = new Vec3D(strs[1]);
//		else if (strs[0].equals("Ball")){
//			objs.add(new Ball(map));
//		}else if (strs[0].equals("Plane")){
//			objs.add(new Plane(map));
//		}
	}
	public static class Camera{
		public Ray viewPoint;
		public Vec3D screenCenter;
		public Vec3D screenUp;
		public Vec3D screenRight;
		public float screenWidth;
		public float screenHeight;
		public int pictureWidth = 1280;
		public int pictureHeight = 800;
		public Camera(Map<String, String> map){
			viewPoint = new Ray(new Vec3D(map.get("cameraPoint")),
					new Vec3D(map.get("cameraDirection")),
					null);
			screenCenter = new Vec3D(map.get("screenCenter"));
			screenUp = new Vec3D(map.get("screenUp")).normalize();
			screenWidth = Float.parseFloat(map.get("screenWidth"));
			screenHeight = Float.parseFloat(map.get("screenHeight"));
			screenRight = Vec3D.cross(viewPoint.direction, screenUp).normalize();
		}
		public Ray emitRay(int x,int y) {
			float _x = ((float)x)/pictureWidth-0.5f;
			float _y = 0.5f-((float)y)/pictureHeight;
			Vec3D tmp = screenCenter;
			tmp = Vec3D.add(tmp, 
					Vec3D.mul(_y*screenHeight/screenUp.len(), screenUp)					
					);
			tmp = Vec3D.add(tmp, 
					Vec3D.mul(_x*screenWidth/screenRight.len(), screenRight)					
					);
			return new Ray(viewPoint.start, Vec3D.minus(tmp, viewPoint.start), null);
		}
	}
	public abstract class Light{
		public Vec3D color;
		public abstract Ray getRay(Vec3D toPoint);
	}
	public class ParallelLight extends Light{
		static final float inf = 1e5f;
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
		public float n;
		public float refractive;//折射系数
		public float reflect;//反射系数
		public float diffuse;//漫反射系数
		public Vec3D color;
		public void init(Map<String, String> map){
			n = Float.parseFloat(map.get("n"));
			refractive = Float.parseFloat(map.get("refractive"));
			reflect = Float.parseFloat(map.get("reflect"));
			diffuse = Float.parseFloat(map.get("diffuse"));
			color = new Vec3D(map.get("color"));
		}
		public abstract float intersect(Ray ray);
		public abstract Vec3D getNormal(Vec3D interPoint);
	}
	public static class ObjPacked{
		public ObjectInScene obj = null;
	}
	public class Ball extends ObjectInScene{
		public Vec3D center;
		public float radius;
		@Override
		public float intersect(Ray ray) {
			float A = ray.direction.len2();
			float B = 2*Vec3D.dot(ray.direction,Vec3D.minus(ray.start, center));
			float C = Vec3D.minus(ray.start, center).len2() - radius*radius;
			float delta = B*B - 4*A*C;
			if(delta<=0)
				return Float.MAX_VALUE;
			float x1 = (float)(-Math.sqrt(delta)-B)/(2*A);
			float x2 = (float)(Math.sqrt(delta)-B)/(2*A);
			if(ray.nowIn == this)
			{
				if(x1>0)
					return x1;
				if(x2>0)
					return x2;
				return Float.MAX_VALUE;
			}else{
				if(x1>0)
					return x1;
				return Float.MAX_VALUE;
			}
		}
		public Ball(Map<String, String> map)
		{
			super.init(map);
			center = new Vec3D(map.get("center"));
			radius = Float.parseFloat(map.get("radius"));
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
		public float intersect(Ray ray) {
			if(Vec3D.dot(ray.direction, normal)==0)
				return Float.MAX_VALUE;
			float k = Vec3D.dot(p, normal)/Vec3D.dot(ray.direction, normal);
			if (k>0)
				return k;
			return Float.MAX_VALUE;
		}
		@Override
		public Vec3D getNormal(Vec3D interPoint) {
			return normal;
		}
	}
	public float findObj(Ray ray, ObjPacked op)
	{
		float dis = Float.MAX_VALUE;
		for(ObjectInScene obj:objs)
		{
			float tmp = obj.intersect(ray);
			if(tmp<dis)
			{
				dis = tmp;
				op.obj = obj;
			}
		}
		return dis;
	}
//	public Vec3D calcLight(Ray ray, Vec3D nStart, ObjectInScene obj) {
//		Vec3D ret = Vec3D.black;
//		Vec3D normal = obj.getNormal(nStart);
//		for(Light light:lights)
//		{
//			Ray lightRay = light.getRay(nStart);
//			ObjPacked op = new ObjPacked();
//			float dis = findObj(lightRay,op);
//			if(op.obj==obj && Vec3D.dis(lightRay.online(dis), nStart)<1e-5)
//			{
////				ret = Vec3D.add(ret, 
////						Vec3D.mul(k, light.color)
////						
////						);
//			}
//				
//		}
//		return null;
//	}
}
