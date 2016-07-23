package org.apache.hadoop.BHW.physics;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.transform.OutputKeys;

import org.apache.hadoop.BHW.Bottle;
import org.apache.hadoop.BHW.Vec3D;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class FrameIterator {
	public static class FrameIteratorMapper extends Mapper<Object, Text, Text, Text>
	{
		private Text outKey = new Text();
		@Override
		protected void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			Particle p = new Particle(value.toString());
			int x = (int) p.getOldPos().x;
			int y = (int) p.getOldPos().y;
			int z = (int) p.getOldPos().z;
			for(int i=-1;i<2;++i)
				for(int j=-1;j<2;++j)
					for(int k=-1;k<2;++k)
					{
						outKey.set(""+(x+i)+","+(y+j)+","+(z+k));
						context.write(outKey, value);
					}
		}
	}
	public static class FrameIteratorReducer extends Reducer<Text, Text, Text, Text>
	{
		public Bottle bottle;
		@Override
		protected void setup(Context context)
				throws IOException, InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			bottle = new Bottle(conf.get(Frame.BottlePathTag));
			bottle.run(Float.parseFloat(conf.get(Frame.BottleAccelerationTag)));			
		}
		@Override
		protected void cleanup(Context context)
				throws IOException, InterruptedException {
			super.cleanup(context);
		}
		private Text outKey = new Text("");
		private Text outVal = new Text("");
		@Override
		protected void reduce(Text key, Iterable<Text> value, Context context)
				throws IOException, InterruptedException {
			ArrayList<Particle> particles = new ArrayList<Particle>();
			ArrayList<Particle> centers = new ArrayList<Particle>();
			for(Text val:value){
				particles.add(new Particle(val.toString()));
			}
			String[] strs = key.toString().split(",");
			int x = Integer.parseInt(strs[0]);
			int y = Integer.parseInt(strs[1]);
			int z = Integer.parseInt(strs[2]);
			for(Particle p:particles){
				ArrayList<Particle> neighbours = new ArrayList<Particle>();
				Vec3D pos = p.getOldPos();
				if(x==(int)pos.x && y==(int)pos.y && z==(int)pos.z){
					centers.add(p);
					for(Particle n:particles)
						if(Vec3D.dis(p.getNewPos(),n.getNewPos())<ParticleSystem.H)
							neighbours.add(n);
						neighbours.remove(p);
						p.setNeighbors(neighbours);
					}
			}
			for(Particle p:centers){
				p.setLambda(ParticleSystem.lambda(p,p.getNeighbors()));
			}
			for (Particle p:centers) {
				Vec3D deltaP = new Vec3D();
				ArrayList<Particle> neighbors = p.getNeighbors();
				for (Particle n : neighbors) {
					float lambdaSum = p.getLambda() + n.getLambda();
					float sCorr = ParticleSystem.sCorr(p, n);
					// float sCorr = 0;
					deltaP.add(Vec3D.mul(ParticleSystem.WSpiky(p.getNewPos(), n.getNewPos()),lambdaSum + sCorr));
				}
				p.setDeltaP(Vec3D.div(deltaP, ParticleSystem.REST_DENSITY));
			}
			// Update position x*i = x*i + delta Pi
			for (Particle p : centers) {
				p.getNewPos().add(p.getDeltaP());
			}
			if(context.getConfiguration().getBoolean("final", false))
			{
				for(Particle p:centers){
					ParticleSystem.imposeConstraints(bottle, p);
					// set new velocity vi = (x*i - xi) /(deltaT) 
					p.setVelocity(Vec3D.div(Vec3D.minus(p.getNewPos(), p.getOldPos()), ParticleSystem.deltaT));
					// apply vorticity confinement
					p.getVelocity().add( Vec3D.mul(ParticleSystem.vorticityForce(p), ParticleSystem.deltaT));
					// apply XSPH viscosity
					//p.getVelocity().add(xsphViscosity(p));TODO
					// update position xi = x*i
					p.setOldPos(p.getNewPos());
				}
			}
			outKey.set("");
			for(Particle p:centers){
				outVal.set(p.toString());
				context.write(outKey, outVal);	
			}
		}
	}
}
