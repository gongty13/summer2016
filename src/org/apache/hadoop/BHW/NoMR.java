package org.apache.hadoop.BHW;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.hadoop.BHW.Vec3D.Ray;
import org.apache.hadoop.BHW.physics.Particle;
import org.apache.hadoop.BHW.physics.ParticleSystem;
import org.apache.hadoop.BHW.render.RenderV2;
import org.apache.hadoop.BHW.render.Scene;
import org.apache.hadoop.BHW.render.Scene.Camera;
import org.apache.hadoop.BHW.render.Scene.ParallelLight;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.P;

public class NoMR {
	public static ArrayList<Particle> particles;
	public static Scene scene;
	public static Camera camera;
	public static Vec3D eye;
	public static float eyeToScreen;

	public static void initScene(){
		scene = new Scene();
		StringBuilder sb = new StringBuilder("");
		scene.parse("Camera cameraPoint:0,-400,120\t"+
				"cameraDirection:0,1,0\t"+
				"screenCenter:0,-200,120\t"+
				"screenUp:0,0,1\t"+
				"screenWidth:320\t"+
				"screenHeight:200\n");
		scene.parse("environment 0.5,0.5,0.5\n");
		scene.parse("ParallelLight color:1,1,1\t"+
				"direction:0,1,-1");
		camera = scene.camera;
		eye = camera.viewPoint.start;
		eyeToScreen = Vec3D.dis(eye, camera.screenCenter);
	}
	public static void initParticle(float radius, float height){
		particles = new ArrayList<Particle>();
		for(int x=0;x<=(int)radius;++x)
			for(int y=-(int)Math.sqrt(radius*radius-x*x);y<=(int)Math.sqrt(radius*radius-x*x);++y)
				for(int z=0;z<(int)height;++z){
					Particle p = new Particle(new Vec3D(x,y,z));
					particles.add(p);
				}
	}
	public static void setNeighbour(){
		
		Map<String, ArrayList<Particle> > cells =new HashMap<String, ArrayList<Particle> >();
		System.out.println(particles.size());
		int num=0;
		for(Particle p:particles) p.getNeighbors().clear();
		for(Particle p:particles){
			++num;
			if(num%100==0)
				System.out.println(num);
			int x=(int)p.getNewPos().x;
			int y=(int)p.getNewPos().y;
			int z=(int)p.getNewPos().z;
			for(int i= -1;i<2;++i)
				for(int j= -1;j<2;++j)
					for(int k= -1;k<2;++k){
//						System.out.println(""+i+"\t"+j+"\t"+k);
						String key = ""+(x+i)+"\t"+(y+j)+"\t"+(z+k);
						ArrayList<Particle> cell = cells.get(key);
						if(cell==null){
							cell = new ArrayList<Particle>();
							cell.add(p);
							cells.put(key, cell);
						}
						else{
							for(Particle n:cell){
								n.getNeighbors().add(p);
								p.getNeighbors().add(n);
							}
							cell.add(p);
						}
					}
		}
		cells.clear();
	}
	public static void update(Bottle bottle) {
		 for (Particle p : particles) {
				p.setForce(0, 0, 0);
				p.getForce().add(ParticleSystem.GRAVITY);
	                        p.setNewPos(p.getOldPos().clone());

	                        // update velocity vi = vi + delta T * fext
	                        p.getVelocity().add(p.getForce().clone().mul(ParticleSystem.deltaT));

	                        // predict position x* = xi + delta T * vi
	                        p.getNewPos().add(p.getVelocity().clone().mul(ParticleSystem.deltaT));

	                        ParticleSystem.imposeConstraints(bottle, p);
	                }
		 System.out.println("setting Neighbour");
		 setNeighbour();
		 for (int i = 0; i < ParticleSystem.PRESSURE_ITERATIONS; i++) {
			 System.out.println("iteration "+i);
			 // Set lambda
			 for (Particle p : particles) {
				 ArrayList<Particle> neighbors = p.getNeighbors();
				 p.setLambda(ParticleSystem.lambda(p, neighbors));
			 }
			 System.out.println("Calculate deltaP");
                        // Calculate deltaP
			 for (Particle p : particles) {
				 Vec3D deltaP = new Vec3D();
				 ArrayList<Particle> neighbors = p.getNeighbors();
				 for (Particle n : neighbors) {
					 float lambdaSum = p.getLambda() + n.getLambda();
					 float sCorr = ParticleSystem.sCorr(p, n);
					 // float sCorr = 0;
					 deltaP.add((ParticleSystem.WSpiky(p.getNewPos(), n.getNewPos())).mul(lambdaSum + sCorr));
				 }	
				 p.setDeltaP(deltaP.div(ParticleSystem.REST_DENSITY));
                        }
			 // Update position x*i = x*i + delta Pi
			 System.out.println("Update position x*i = x*i + delta Pi");
			 for (Particle p : particles) {
				 p.getNewPos().add(p.getDeltaP());
			 }
		 }
		 for (Particle p : particles) {
			 ParticleSystem.imposeConstraints(bottle, p);
			 // set new velocity vi = (x*i - xi) /(deltaT) 
			 p.setVelocity(Vec3D.minus(p.getNewPos(), p.getOldPos()).div(ParticleSystem.deltaT));
			 // apply vorticity confinement
			 p.getVelocity().add(ParticleSystem.vorticityForce(p).mul(ParticleSystem.deltaT));
			 // apply XSPH viscosity
			 p.getVelocity().add(ParticleSystem.xsphViscosity(p));
			 // update position xi = x*i
		 }
	}
	public static void render(String picPath) throws IOException{
		Particle[][] view = new Particle[camera.pictureHeight][camera.pictureWidth];
		float[][] vdis = new float[camera.pictureHeight][camera.pictureWidth];
		for(Particle p:particles){
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
			for(int i=Math.max(0, x_pixel-3);i<Math.min(x_pixel+4, camera.pictureWidth);++i)
				for(int j=Math.max(0, y_pixel-3);j<Math.min(y_pixel+4, camera.pictureHeight);++j){
					Ray ray = camera.emitRay(i, j);
					Vec3D intesection = RenderV2.getIntersecection(ray, p.getOldPos());
					if(intesection!=null)
					if(view[j][i]==null || vdis[j][i]>Vec3D.dis(intesection, eye)){
							view[j][i] = p;
							vdis[j][i] = Vec3D.dis(intesection, eye);					
					}
				}
		}
		BufferedImage img = new BufferedImage(camera.pictureWidth, camera.pictureHeight, BufferedImage.TYPE_3BYTE_BGR);
		ParallelLight light = (ParallelLight)scene.lights.get(0);
		for(int i=0;i<camera.pictureHeight;++i)
			for(int j=0;j<camera.pictureWidth;++j)
			{
				if(view[i][j]==null)
					img.setRGB(j, i, RenderV2.calcRGB(Vec3D.black));
				else{
					Vec3D intersection = RenderV2.getIntersecection(camera.emitRay(j, i), view[i][j].getOldPos());
					Vec3D normal = Vec3D.minus(intersection, view[i][j].getOldPos());
					float cosVal = Math.abs(Vec3D.dot(normal.normalize(),light.direction.normalize()));
					img.setRGB(j, i, RenderV2.calcRGB(Vec3D.mul(light.color, cosVal)));					
				}
			}
		ImageIO.write(img, "JPEG", new File(picPath));
	}
	public static void saveParticle(String path) throws IOException{
		File f = new File(path);
		if(f.exists())
			f.delete();
		FileWriter fw = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(fw);
		for(Particle p:particles){
			bw.write(p.toString()+'\n');
		}
		bw.close();
	}
	public static void main(String []args) throws IOException{
		float radius = 25;
		float height = 50;
		float time = 2;
		float acceleration = 50;
		float deceleration = 50;
		float distance = 100;

		float x0 = deceleration*distance/(acceleration+deceleration);
		float x1 = acceleration*distance/(acceleration+deceleration);
		float t0 = (float)Math.sqrt(2*x0/acceleration);
		float t1 = t0+(float)Math.sqrt(2*x1/deceleration);
		
		Bottle bottle = new Bottle("");
		bottle.radius = radius;
		initScene();
		initParticle(radius, height);
		
//		/*
		for(int i=0;i<time/ParticleSystem.deltaT;++i){
			System.out.println(i);
			float t = i*ParticleSystem.deltaT;
			float nowAcceleration = 0;
			if(t<t0)
				nowAcceleration = acceleration;
			else if (t<t1)
				nowAcceleration = -deceleration;
			bottle.run(nowAcceleration);
			update(bottle);
			saveParticle("./_"+t+".txt");
			render("./_"+t+".JPEG");
//			render(conf, PREFIX+"/"+((i+1)*ParticleSystem.deltaT), PREFIX+"/"+((i+1)*ParticleSystem.deltaT)+"_render");
////			RenderV2.getPic(conf, PREFIX+"/"+((i+1)*ParticleSystem.deltaT)+"_render/"+SUFFIX,
////					"./"+((i+1)*ParticleSystem.deltaT)+".JPEG");
//			updateBottle(fs, bottle);
//			break;
		}//*/
		
		
	}
	
}
