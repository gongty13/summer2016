package org.apache.hadoop.BHW.render;

import java.io.IOException;

import org.apache.hadoop.BHW.Vec3D;
import org.apache.hadoop.BHW.Vec3D.Ray;
import org.apache.hadoop.BHW.render.Scene.ObjPacked;
import org.apache.hadoop.BHW.render.Scene.ObjectInScene;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;


public class Rendering {
	static final String objsPathTag = "objsPath";
	public class RenderingMapper extends Mapper<Object, Text, Text, Text>{
		private Scene scene = new Scene();
		@Override
		public void setup(Context context) throws IOException, InterruptedException
		{
			super.setup(context);
			Configuration conf = context.getConfiguration();
			String objsPath = conf.get(objsPathTag);
			FileSystem fs = FileSystem.get(conf);
			FSDataInputStream dis = fs.open(new Path(objsPath));
			while(true)
			{
				String line = dis.readLine();
				if(line == null || line.isEmpty())
					break;
				scene.parse(line);
			}
		}
		public Vec3D rayTrace(Ray ray, float weight)
		{
			if(weight < 1e-5)
				return Vec3D.black;
			Vec3D ret = Vec3D.black;
			ObjPacked op = new ObjPacked();
			float dis = scene.findObj(ray,op);
			ObjectInScene obj = op.obj;
			if(obj == null)
				return ret;
			Vec3D nStart = ray.online(dis);
			Ray reflect = ray.reflect(nStart, obj.getNormal(nStart));
			Ray refractive = ray.refractive(nStart, obj.getNormal(nStart), obj);
			if(refractive!=null)
			{
				ret = Vec3D.add(ret,Vec3D.mul(obj.refractive, rayTrace(reflect, weight*obj.refractive)));
			}
			if(ray.nowIn==null)
				ret = Vec3D.add(ret, scene.calcLight(ray,nStart,obj));
			float k = refractive==null?obj.reflect+obj.refractive:obj.reflect;
			ret = Vec3D.add(ret, Vec3D.mul(k, rayTrace(reflect, weight*k)));
 			return ret;
		}
		private Text retVal = new Text("");
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException
		{
			String[] strs = value.toString().split(",");
			int x = Integer.parseInt(strs[0]);
			int y = Integer.parseInt(strs[1]);
			Ray start = scene.camera.emitRay(x, y);
			Vec3D color = rayTrace(start, 1);
			int R = Math.round((float)color.x * 255);
			int G = Math.round((float)color.y * 255);
			int B = Math.round((float)color.z * 255);
			retVal.set(""+R+","+G+","+B);
			context.write(value, retVal);
		}
	}
	public class RenderingReducer extends Reducer<Text, Text, Text, Text>
	{
		public void reduce(Text key, Iterable<Text> value, Context context)throws IOException, InterruptedException
		{
			for(Text val:value)
			{
				context.write(key, val);
			}
		}
	}
	public static void main(String args[]) throws Exception
	{
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: Rendering <objsPath> <in> <out>");
			System.exit(2);
		}
		conf.set(objsPathTag, otherArgs[0]);
		Job job = new Job(conf, "Rendering");
		job.setJarByClass(Rendering.class);
		job.setMapperClass(RenderingMapper.class);
		job.setReducerClass(RenderingReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[1]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
