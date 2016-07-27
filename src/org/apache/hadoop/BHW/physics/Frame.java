package org.apache.hadoop.BHW.physics;

import org.apache.hadoop.BHW.Bottle;
import org.apache.hadoop.BHW.physics.FrameIterator.FrameIteratorMapper;
import org.apache.hadoop.BHW.physics.FrameIterator.FrameIteratorReducer;
import org.apache.hadoop.BHW.physics.FrameStart.FrameStartMapper;
import org.apache.hadoop.BHW.physics.FrameStart.FrameStartReducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;


public class Frame {
	public static final String BottlePathTag = "bottlePath";
	public static final String BottleAccelerationTag = "bottleAcceleration";
	public static void runInit(String inputPath, String outputPath, Configuration conf) throws Exception
	{
		Job job = new Job(conf, "init");
		job.setJarByClass(Frame.class);
		job.setMapperClass(FrameStartMapper.class);
		job.setReducerClass(FrameStartReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		job.waitForCompletion(true);		
	}
	public static void runIterator(String inputPath, String outputPath, Configuration conf) throws Exception
	{
		Job job = new Job(conf, "iter");
		job.setJarByClass(Frame.class);
		job.setMapperClass(FrameIteratorMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setReducerClass(FrameIteratorReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		job.waitForCompletion(true);
	}
	public static void run(Configuration conf, String bottlePath, float bottleAcceleration, String in, String out)throws Exception
	{
		conf.set(BottlePathTag, bottlePath);
		conf.setFloat(BottleAccelerationTag, bottleAcceleration);
		runInit(in, out+"_"+0, conf);
		for(int i=0;i<ParticleSystem.PRESSURE_ITERATIONS;++i){
			conf.setBoolean("final", i+1==ParticleSystem.PRESSURE_ITERATIONS);
			runIterator(out+"_"+i, out+"_"+(i+1), conf);
		}
		FileSystem fs = FileSystem.get(conf);
		fs.rename(new Path(out+"_"+ParticleSystem.PRESSURE_ITERATIONS), new Path(out));
		//run Render TODO
	}
}
