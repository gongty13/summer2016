package org.apache.hadoop.BHW.physics;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.BHW.Bottle;
import org.apache.hadoop.BHW.Vec3D;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class FrameStart {
	public static class FrameStartMapper extends Mapper<Object, Text, Object, Object>
	{
		public Bottle bottle;
		@Override
		public void setup(Context context)
				throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			FileSystem fs = FileSystem.get(conf);
			FSDataInputStream dis = fs.open(new Path(conf.get(Frame.BottlePathTag)));
			bottle = new Bottle(dis.readLine());
			bottle.run(Float.parseFloat(conf.get(Frame.BottleAccelerationTag)));
			dis.close();
		}
		private Text outKey = new Text();
		private Text outVal = new Text();
		@Override
		public void map(Object key,Text value, Context context)throws IOException, InterruptedException
		{
			Particle p = new Particle(value.toString());
			p.setForce(0, 0, 0);
			p.getForce().add(ParticleSystem.GRAVITY);
			p.setNewPos(p.getOldPos().clone());
			p.getVelocity().add(Vec3D.mul(ParticleSystem.deltaT, p.getForce()));
			p.getNewPos().add(Vec3D.mul(ParticleSystem.deltaT, p.getVelocity()));
			ParticleSystem.imposeConstraints(bottle, p);
			outKey.set("");
			outVal.set(p.toString());
			context.write(outKey, outVal);
		}
		@Override
		public void cleanup(Context context)
				throws IOException, InterruptedException {
			super.cleanup(context);
		}
	}
	public static class FrameStartReducer  extends Reducer<Text, Text, Text, Text>
	{
		@Override
		public void setup(Context context)
				throws IOException, InterruptedException {
			super.setup(context);
		}
		@Override
		protected void reduce(Text key, Iterable<Text> value, Context context)
				throws IOException, InterruptedException {
			for(Text val:value){
				context.write(key, val);
			}
		}
		@Override
		public void cleanup(Context context)
				throws IOException, InterruptedException {
			super.cleanup(context);
		}
	}

}
