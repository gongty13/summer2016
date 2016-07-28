package org.apache.hadoop.BHW.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.hadoop.BHW.Bottle;
import org.apache.hadoop.BHW.Generator;
import org.apache.hadoop.BHW.Vec3D;
import org.apache.hadoop.BHW.Vec3D.Ray;
import org.apache.hadoop.BHW.physics.Particle;
import org.apache.hadoop.BHW.render.Scene.Camera;
import org.apache.hadoop.BHW.render.Scene.Light;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;



public class RenderV2 {
	public static class RenderV2mapper extends Mapper<Object, Text, Text, FloatWritable>
	{
		private Scene scene = new Scene();
		Camera camera;
		Vec3D eye;
		float eyeToScreen;
		@Override
		protected void setup(Context context)throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			FileSystem fs = FileSystem.get(conf);
			FSDataInputStream dis = fs.open(new Path(conf.get("scenePath")));
			while(true){
				String line = dis.readLine();
				if(line==null || line.isEmpty())
					break;
				scene.parse(line);
			}
			dis.close();
			camera = scene.camera;
			eye = camera.viewPoint.start;
			eyeToScreen = Vec3D.dis(camera.screenCenter, eye);
		}
		private Text outKey = new Text();
		private FloatWritable outVal = new FloatWritable();
		@Override
		protected void map(Object key, Text value, Context context)throws IOException, InterruptedException {
			Particle p = new Particle(value.toString());
			Vec3D c = p.getOldPos();
			Ray r = new Ray(eye, Vec3D.minus(c, eye), null);
			float tmp = r.direction.dot(camera.viewPoint.direction)/camera.viewPoint.direction.len();
			Vec3D vp = Vec3D.minus(Vec3D.add(eye, 
					Vec3D.mul(r.direction, eyeToScreen/tmp)), scene.camera.screenCenter);
//			System.out.println(vp.toString()+"\t"+eye+"\t"+camera.screenCenter+"\t"+eyeToScreen);
//			sc + vp on screen
			float x = vp.dot(camera.screenRight)/camera.screenRight.len()/camera.screenWidth;
			float y = vp.dot(camera.screenUp)/camera.screenUp.len()/camera.screenHeight;
//			System.out.println(p.toString()+" "+x+","+y);
			if(x<-0.5 || x>0.5)
				return;
			if(y<-0.5 || y>0.5)
				return;
			int x_pixel = (int)((x+0.5)*camera.pictureWidth);
			int y_pixel = (int)((0.5-y)*camera.pictureHeight);
			for(int i=Math.max(0, x_pixel-1);i<Math.min(x_pixel+2, camera.pictureWidth);++i)
				for(int j=Math.max(0, y_pixel-1);j<Math.min(y_pixel+2, camera.pictureHeight);++j)
			{
					outKey.set(""+i+" "+j);
					outVal.set(r.direction.len());
					context.write(outKey, outVal);
			}
		}
	}
	public static class RenderV2reducer extends Reducer<Text, FloatWritable, Text, FloatWritable> {
		private FloatWritable outVal = new FloatWritable();
		@Override
		protected void reduce(Text key, Iterable<FloatWritable> value,Context context)throws IOException, InterruptedException {
			float min = Float.MAX_VALUE;
			for(FloatWritable val:value){
				if(min>val.get())
					min = val.get();
			}
//			System.out.println(min);
			outVal.set(min);
			context.write(key, outVal);
		}
	}
	public static class View{
		public Vec3D n;
		public float dis;
		public View() {
		}
	}
	public static final float particleR= 0.6f;
	public static final Vec3D particleColor = new Vec3D(0,0,1);
	public static final Vec3D bottleColor = new Vec3D(0.2f, 0.2f, 0.2f);
	public static float point2line(Ray ray, Vec3D center){
		float A = ray.direction.len2();
		float B = 2*Vec3D.dot(ray.direction,Vec3D.minus(ray.start, center));
//		C == B*B/4/A
		float ret2 = Vec3D.minus(ray.start, center).len2()-B*B/4/A;
		return ret2>0?(float)Math.sqrt(ret2):0;
	}
	public static Vec3D getIntersecection(Ray ray, Vec3D center){
		float A = ray.direction.len2();
		float B = 2*Vec3D.dot(ray.direction,Vec3D.minus(ray.start, center));
		float C = Vec3D.minus(ray.start, center).len2() - particleR*particleR;
		float delta = B*B - 4*A*C;
//		dis(k*d+s,c)=r
//		(k*d+s-c)^2=r
		if(delta<=0)
			return null;
		float x1 = (float)(-Math.sqrt(delta)-B)/(2*A);
		float x2 = (float)(Math.sqrt(delta)-B)/(2*A);
		return Vec3D.add(center, 
				Vec3D.mul(ray.direction, x1));
	}
	public static int calcRGB(Vec3D rgb){
		int r = (int)(rgb.x*255);
		if(r>255)r = 255;
		int g = (int)(rgb.y*255);
		if(g>255)g = 255;
		int b = (int)(rgb.z*255);
		if(b>255)b = 255;
//		System.out.println(""+r+","+g+","+b);
		return (r<<16)|(g<<8)|b;
	}
	public static Vec3D getNormal(Vec3D center, ArrayList<Vec3D> list)
	{
		float tmp = 0;
		Vec3D ret = null;
		for(int i=0;i<list.size();++i)
			for(int j=i+1;j<list.size();++j)
				for(int k=j+1;k<list.size();++k){
				Vec3D r = Vec3D.cross(Vec3D.minus(list.get(i), list.get(k)), Vec3D.minus(list.get(j), list.get(k)));
				if(r.len()>tmp){
					tmp  = r.len();
					ret = r;
				}
			}
		return ret;
	}
	public static void getPic(Configuration conf, String reduceResult, String out) throws IOException{
		Scene scene = new Scene();
		FileSystem fs = FileSystem.get(conf);
		FSDataInputStream dis = fs.open(new Path(conf.get("scenePath")));
		while(true){
			String line = dis.readLine();
			if(line==null || line.isEmpty())
				break;
			scene.parse(line);
		}
		dis.close();
		dis = fs.open(new Path(Generator.PREFIX+"/bottle.txt"));
		Bottle bottle = new Bottle(dis.readLine());
		dis.close();
		Camera camera = scene.camera;
		dis = fs.open(new Path(reduceResult));
		View[][] view = new View[camera.pictureHeight][camera.pictureWidth];
		while(true){
			String line = dis.readLine();
			if(line==null || line.isEmpty())
				break;
			String[] strs = line.split("\\s+");
			if(strs.length!=3)
				continue;
			int x = Integer.parseInt(strs[0]);
			int y = Integer.parseInt(strs[1]);
			float d = Float.parseFloat(strs[2]);
			view[y][x] = new View();
			view[y][x].dis = d;
			view[y][x].n = camera.emitRay(x, y).direction.mul(-1).normalize();
		}
		BufferedImage img = new BufferedImage(camera.pictureWidth, camera.pictureHeight, BufferedImage.TYPE_3BYTE_BGR);
		BufferedImage img2 = new BufferedImage(camera.pictureWidth, camera.pictureHeight, BufferedImage.TYPE_3BYTE_BGR);
		
		for(int i=0;i<camera.pictureHeight;++i)
			for(int j=0;j<camera.pictureWidth;++j)
			{
				Vec3D color = Vec3D.black.clone();//scene.environment.clone();
				Ray ray = camera.emitRay(j, i);
				{
					float A = ray.direction.x*ray.direction.x + ray.direction.y*ray.direction.y;
					float B = 2*(ray.direction.x*(ray.start.x-bottle.center.x)+ray.direction.y*(ray.start.y-bottle.center.y));
					float C = (ray.start.x-bottle.center.x)* (ray.start.x-bottle.center.x)+
							(ray.start.y-bottle.center.y)* (ray.start.y-bottle.center.y)+
							- bottle.radius*bottle.radius;
					float delta = B*B - 4*A*C;
					if(delta>0)
					{
						delta = (float)Math.sqrt(delta);
						Vec3D i1 = new Vec3D(ray.start);
						Vec3D i2 = new Vec3D(ray.start);
						i1.add(Vec3D.mul(ray.direction, (-B+delta)/(2*A)));
						i2.add(Vec3D.mul(ray.direction, (-B-delta)/(2*A)));
						if (i1.z<75 && i1.z>0)
							color.add(bottleColor);
						if (i2.z<75 && i2.z>0)
							color.add(bottleColor);
					}
				}
				if(view[i][j]==null){
					img.setRGB(j, i, calcRGB(color));
					continue;
				}
				int num=0;
				int range = 1;
				Vec3D mid = Vec3D.mul(camera.emitRay(j, i).direction,view[i][j].dis);
				ArrayList<Vec3D> list = new ArrayList<Vec3D>();
				for(int k=-range;k<=range;++k)
					if(0<=i+k && i+k<camera.pictureHeight)
						for(int l=-range;l<=range;++l)
							if(0<=j+l && j+l<camera.pictureWidth)
								if(view[i+k][j+l]!=null){
									list.add(Vec3D.mul(camera.emitRay(j+l, i+k).direction,view[i+k][j+l].dis));
							}
				Vec3D normal = getNormal(mid, list);
				if(normal!=null){
					Light light = scene.lights.get(0);
					Vec3D d = Vec3D.mul(light.getRay(mid).direction,-1).normalize();
					float cosVal = normal.normalize().dot(d);
					cosVal = Math.abs(cosVal);
					if (cosVal>0)
						color.add(Vec3D.colorMul(particleColor, Vec3D.mul(light.color, cosVal)));
					img.setRGB(j, i, calcRGB(color));
				}
				else{
//					img.setRGB(j, i, calcRGB(scene.environment));
					img.setRGB(j, i, calcRGB(color));
				}
//				System.out.println(calcRGB(scene.environment)+" ??? "+scene.environment.toString());
//				System.out.println(""+j+","+i+":"+img.getRGB(i, j));
				
			}
		ImageIO.write(img, "JPEG", new File(out));
	}
}
