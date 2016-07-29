package org.apache.hadoop.BHW;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.hadoop.BHW.physics.Frame;
import org.apache.hadoop.BHW.physics.Particle;
import org.apache.hadoop.BHW.physics.ParticleSystem;
import org.apache.hadoop.BHW.render.RenderV2;
import org.apache.hadoop.BHW.render.RenderV2.RenderV2mapper;
import org.apache.hadoop.BHW.render.RenderV2.RenderV2reducer;
import org.apache.hadoop.BHW.render.Scene;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Generator extends Thread implements Runnable{
	public static final String PREFIX = "/user/hadoop/BHW";
	public static final String SUFFIX = "part-r-00000";
	public static FileSystem init(Configuration conf) throws IOException{
		FileSystem fs = FileSystem.get(conf);
		Path path = new Path(PREFIX);
		if(fs.exists(path)){
			fs.delete(path);
		}
		fs.mkdirs(path);
		return fs;
	}
	public static void updateBottle(FileSystem fs, Bottle bottle) throws IOException{
		FSDataOutputStream bottleFile = fs.create(new Path(PREFIX+"/bottle.txt"));
		bottleFile.writeBytes(bottle.toString());
		bottleFile.close();
	}
	public static void initScene(FileSystem fs) throws IllegalArgumentException, IOException{
		File f = new File("./scene.txt");
		if(f.exists())
			f.delete();
		FileWriter fw = new FileWriter("./scene.txt");
		BufferedWriter screenFile = new BufferedWriter(fw);
		FSDataOutputStream dos = fs.create(new Path(PREFIX+"/scene.txt"));
		StringBuilder sb = new StringBuilder("");
		sb.append("Camera cameraPoint:0,-400,120\t");
		sb.append("cameraDirection:0,1,0\t");
		sb.append("screenCenter:0,-200,120\t");
		sb.append("screenUp:0,0,1\t");
		sb.append("screenWidth:320\t");
		sb.append("screenHeight:200\n");
		sb.append("environment 0.5,0.5,0.5\n");
		sb.append("ParallelLight color:1,1,1\t");
		sb.append("direction:0,1,-1");
		dos.writeBytes(sb.toString());
		dos.close();
		screenFile.write(sb.toString());
		screenFile.close();
	}
	public static void initParticle(float radius, float height, FileSystem fs) throws IllegalArgumentException, IOException{
		fs.mkdirs(new Path(PREFIX+"/"+0f));
		FSDataOutputStream particleFile = fs.create(new Path(PREFIX+"/"+0f+"/particle.txt"));
		for(int x=0;x<=(int)radius;++x)
			for(int y=-(int)Math.sqrt(radius*radius-x*x);y<=(int)Math.sqrt(radius*radius-x*x);++y)
				for(int z=0;z<(int)height;++z){
					Particle p = new Particle(new Vec3D(x,y,z));
					particleFile.writeBytes(p.toString()+'\n');
				}
		particleFile.close();
	}
	public static void render(Configuration conf, String in, String out) throws IOException, ClassNotFoundException, InterruptedException
	{
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(new Path(out)))
			fs.delete(new Path(out));
				
		Job job = new Job(conf);
		job.setJarByClass(Generator.class);
		job.setMapperClass(RenderV2mapper.class);
		job.setReducerClass(RenderV2reducer.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FloatWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(FloatWritable.class);
		FileInputFormat.addInputPath(job, new Path(in));
		FileOutputFormat.setOutputPath(job, new Path(out));
		job.waitForCompletion(true);
	}
	public static void run(float radius, float height, float acceleration, float deceleration, float distance, float time) throws Exception{

		Configuration conf = new Configuration();
		conf.set("scenePath", PREFIX+"/scene.txt");
		float x0 = deceleration*distance/(acceleration+deceleration);
		float x1 = acceleration*distance/(acceleration+deceleration);
		float t0 = (float)Math.sqrt(2*x0/acceleration);
		float t1 = t0+(float)Math.sqrt(2*x1/deceleration);
		
		Bottle bottle = new Bottle("");
		bottle.radius = radius;
//		FileSystem fs = FileSystem.get(conf);
		FileSystem fs = init(conf);
		
		initScene(fs);
		updateBottle(fs, bottle);
		initParticle(radius, height, fs);

		render(conf, PREFIX+"/"+0f, PREFIX+"/"+0f+"_render");
		RenderV2.getPic(conf, PREFIX+"/"+0f+"_render/"+SUFFIX, "./"+0f+".JPEG");
		
		
		JProgressBar progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum((int)(time/ParticleSystem.deltaT)-1);
		progressBar.setValue(0);
		java.awt.Frame dialog = new java.awt.Frame("running");
		dialog.add(progressBar);
		dialog.pack();
		dialog.setVisible(true);
//		
		for(int i=0;i<time/ParticleSystem.deltaT;++i){
			float t = i*ParticleSystem.deltaT;
			float nowAcceleration = 0;
			if(t<t0)
				nowAcceleration = acceleration;
			else if (t<t1)
				nowAcceleration = -deceleration;
			Frame.run(conf, PREFIX+"/bottle.txt", nowAcceleration, PREFIX+"/"+t, PREFIX+"/"+((i+1)*ParticleSystem.deltaT));
			bottle.run(nowAcceleration);
			updateBottle(fs, bottle);
			render(conf, PREFIX+"/"+((i+1)*ParticleSystem.deltaT), PREFIX+"/"+((i+1)*ParticleSystem.deltaT)+"_render");
			RenderV2.getPic(
					conf, 
					PREFIX+"/"+((i+1)*ParticleSystem.deltaT)+"_render/"+SUFFIX,
					"./"+((i+1)*ParticleSystem.deltaT)+".JPEG");
//			
			//gui.setVisible(false);
			progressBar.setValue(i);
		}
//			dialog.setVisible(true);
		
		dialog.dispose();
		GIFGenerator.run(time, "./final.gif");
	}
	public static void main(String[] args) throws Exception{
		
		CommandLineParser parser = new BasicParser();
		Options opt = new Options();
		opt.addOption("r", true, "radius");
		opt.addOption("h", true, "height");
		opt.addOption("t", true, "time(s)");
		opt.addOption("a", true, "acceleration(s^-2)");
		opt.addOption("d", true, "deceleration(s^-2)(>0)");
		opt.addOption("x", true, "distance");
		CommandLine cmd  = parser.parse(opt, args);
		float radius =  cmd.hasOption('r')?Float.parseFloat(cmd.getOptionValue('r')):25;
		float height = cmd.hasOption('h')?Float.parseFloat(cmd.getOptionValue('h')):50;
		float time = cmd.hasOption('t')?Float.parseFloat(cmd.getOptionValue('t')):2;
		float acceleration = cmd.hasOption('a')?Float.parseFloat(cmd.getOptionValue('a')):50;
		float deceleration = cmd.hasOption('d')?Float.parseFloat(cmd.getOptionValue('d')):50;
		float distance = cmd.hasOption('x')?Float.parseFloat(cmd.getOptionValue('x')):100;
		run(radius, height, acceleration, deceleration, distance, time);	
	}
}
