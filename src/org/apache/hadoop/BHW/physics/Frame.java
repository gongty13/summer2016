package org.apache.hadoop.BHW.physics;

import org.apache.hadoop.BHW.physics.FrameIterator.FrameIteratorMapper;
import org.apache.hadoop.BHW.physics.FrameIterator.FrameIteratorReducer;
import org.apache.hadoop.BHW.physics.FrameStart.FrameStartMapper;
import org.apache.hadoop.BHW.physics.FrameStart.FrameStartReducer;
import org.apache.hadoop.conf.Configuration;
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
		job.setReducerClass(FrameIteratorReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		job.waitForCompletion(true);		
	}
	public static void main(String[] args)throws Exception
	{
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: Frame <bottlePath> <bottleAcceleration> <in> <out>");
			System.exit(2);
		}
		conf.set(BottlePathTag, otherArgs[0]);
		conf.set(BottleAccelerationTag, otherArgs[1]);
		runInit(otherArgs[2], otherArgs[3]+0, conf);
		for(int i=0;i<ParticleSystem.PRESSURE_ITERATIONS;++i){
			runIterator(otherArgs[3]+i, otherArgs[3]+(i+1), conf);
		}
		//run Render
	}
}
