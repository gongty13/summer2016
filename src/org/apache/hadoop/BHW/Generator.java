package org.apache.hadoop.BHW;

import java.io.IOException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.hadoop.BHW.physics.Frame;
import org.apache.hadoop.BHW.physics.Particle;
import org.apache.hadoop.BHW.physics.ParticleSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class Generator {
	public static final String PREFIX = "/user/hadoop/BHW";
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
	public static void main(String[] args) throws Exception
	{
		CommandLineParser parser = new BasicParser();
		Options opt = new Options();
		opt.addOption("r", true, "radius");
		opt.addOption("h", true, "height");
		opt.addOption("t", true, "time(s)");
		opt.addOption("a", true, "acceleration(s^-2)");
		opt.addOption("d", true, "deceleration(s^-2)(>0)");
		opt.addOption("x", true, "distance");
		CommandLine cmd  = parser.parse(opt, args);
		float radius =  cmd.hasOption('r')?Float.parseFloat(cmd.getOptionValue('r')):50;
		float height = cmd.hasOption('h')?Float.parseFloat(cmd.getOptionValue('h')):100;
		float time = cmd.hasOption('t')?Float.parseFloat(cmd.getOptionValue('t')):5;
		float acceleration = cmd.hasOption('a')?Float.parseFloat(cmd.getOptionValue('a')):5;
		float deceleration = cmd.hasOption('d')?Float.parseFloat(cmd.getOptionValue('d')):5;
		float distance = cmd.hasOption('x')?Float.parseFloat(cmd.getOptionValue('x')):100;

		Bottle bottle = new Bottle("");bottle.radius = radius;
		Configuration conf = new Configuration();
		FileSystem fs = init(conf);

		updateBottle(fs, bottle);

		fs.mkdirs(new Path(PREFIX+"/"+0f));
		FSDataOutputStream particleFile = fs.create(new Path(PREFIX+"/"+0f+"/particle.txt"));
		for(int x=-(int)radius;x<=(int)radius;++x)
			for(int y=-(int)Math.sqrt(radius*radius-x*x);y<=(int)Math.sqrt(radius*radius-x*x);++y)
				for(int z=0;z<(int)height;++z){
					Particle p = new Particle(new Vec3D(x,y,z));
					particleFile.writeBytes(p.toString()+'\n');
				}
		particleFile.close();
		float x0 = deceleration*distance/(acceleration+deceleration);
		float x1 = acceleration*distance/(acceleration+deceleration);
		float t0 = (float)Math.sqrt(2*x0/acceleration);
		float t1 = t0+(float)Math.sqrt(2*x1/deceleration);
		
		for(int i=0;i<time/ParticleSystem.deltaT;++i){
			float t = i*ParticleSystem.deltaT;
//			System.out.println(PREFIX+"/"+t);
//			System.out.println(PREFIX+"/"+(i+1)*ParticleSystem.deltaT);
			float nowAcceleration = 0;
			if(t<t0)
				nowAcceleration = acceleration;
			else if (t<t1)
				nowAcceleration = -deceleration;
			Frame.run(PREFIX+"/bottle.txt", nowAcceleration, PREFIX+"/"+t, PREFIX+"/"+((i+1)*ParticleSystem.deltaT));
			//update bottle
			bottle.run(nowAcceleration);
			updateBottle(fs, bottle);
		}
	}
}
