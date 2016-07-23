package org.apache.hadoop.BHW.physics;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.BHW.Bottle;
import org.apache.hadoop.BHW.Vec3D;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class ParticleSystem {
	public static final Vec3D GRAVITY = new Vec3D(0f, -9.8f, 0f);
	//Number of iterations to update pressure constraints (Macklin et al. used 4)
	public static final int PRESSURE_ITERATIONS = 6;
	// deltaT is the timestep
	public static float deltaT = 0.005f;
	// H is radius of influence
	// KPOLY and SPIKY are constant coefficients used in Density Estimation
	// Kernels
	// See Macklin slides or Muller 2003
	public static final float H = 1.25f;
	public static final float KPOLY = (float) (315f / (64f * Math.PI * Math.pow(H, 9)));
	public static final float SPIKY = (float) (45f / (Math.PI * Math.pow(H, 6)));
	public static final float VISC = (float) (15f / (2 * Math.PI * (H * H * H)));
	public static final float REST_DENSITY = 1f;
	// Epsilon used in lambda calculation
	// See Macklin part 3
	public static final float EPSILON_LAMBDA = 150f;
	public static final float C = 0.01f;
	// K and deltaQMag used in sCorr Calculation
	// See Macklin part 4
	public static final float EPSILON_VORTICITY = 10f;
	public static final float K = 0.001f;
	public static final float deltaQMag = .3f * H;
	public static final float wQH = KPOLY * (H * H - deltaQMag * deltaQMag) * (H * H - deltaQMag * deltaQMag) * (H * H - deltaQMag * deltaQMag);
	// Used for bounds of the box
		
	public static float lambda(Particle p, ArrayList<Particle> neighbors) {
		float densityConstraint = calcDensityConstraint(p, neighbors);
		Vec3D gradientI = new Vec3D();
		float sumGradients = 0;
		for (Particle n : neighbors) {
			// Calculate gradient with respect to j
			Vec3D gradientJ = Vec3D.div(
							WSpiky(p.getNewPos(), n.getNewPos())	,
							REST_DENSITY
							);			
			// Add magnitude squared to sum
			sumGradients += gradientJ.len2();
			// Continue calculating particle i gradient
			gradientI.add(gradientJ);
		}
		// Add the particle i gradient magnitude squared to sum
		sumGradients += gradientI.len2();
		return ((-1f) * densityConstraint) / (sumGradients + EPSILON_LAMBDA);
	}
	public static float calcDensityConstraint(Particle p, ArrayList<Particle> neighbors) {
		float sum = 0f;
		for (Particle n : neighbors) {
			sum +=  WPoly6(p.getNewPos(), n.getNewPos());
		}

		return (sum / REST_DENSITY) - 1;
	}
	public static  float WPoly6(Vec3D pi, Vec3D pj) {
		Vec3D r =  Vec3D.minus(pi, pj);
		float rLen = r.len();
		if (rLen > H || rLen == 0) {
			return 0;
		}
		return (float) (KPOLY * Math.pow((H * H - r.len2()), 3));
	}
	public static Vec3D WSpiky(Vec3D pi, Vec3D pj) {
		Vec3D r = Vec3D.minus(pi, pj);
		float rLen = r.len();
		if (rLen > H || rLen == 0) {
			return new Vec3D();
		}

		float coeff = (H - rLen) * (H - rLen);
		coeff *= SPIKY;
		coeff /= rLen;
		return Vec3D.mul(-1 * coeff, r);
	}
	public static float sCorr(Particle pi, Particle pj) {
		// Get Density from WPoly6 and divide by constant from paper
		float corr = WPoly6(pi.getNewPos(), pj.getNewPos()) / wQH;
		// take to power of 4
		corr *= corr * corr * corr;
		return -K * corr;
	}
	public static Vec3D vorticity(Particle p) {
		Vec3D vorticity = new Vec3D(0, 0, 0);
		Vec3D velocityDiff;
		Vec3D gradient;

		ArrayList<Particle> neighbors = p.getNeighbors();
		for (Particle n : neighbors) {
			velocityDiff = Vec3D.minus(n.getVelocity(), p.getVelocity());
			gradient = WViscosity(p.getNewPos(), n.getNewPos());
			vorticity.add(Vec3D.cross(velocityDiff, gradient));
		}

		return vorticity;
	}
	public static Vec3D WViscosity(Vec3D pi, Vec3D pj) {
		Vec3D r = Vec3D.minus(pi, pj);
		float rLen = r.len();
		if (rLen > H || rLen == 0) return new Vec3D();
		
		float coeff = (-1 * (rLen * rLen * rLen)) / (2 * (H * H * H));
		coeff += (r.len2() / (H * H));
		coeff += (H / (2 * rLen)) - 1;
		return Vec3D.mul(r,coeff);
	}
//	public static Vec3D xsphViscosity(Particle p) {
//		Vec3D visc = new Vec3D();
//		ArrayList<Particle> neighbors = p.getNeighbors();
//		for (Particle n : neighbors) {
//			Vec3D velocityDiff = Vec3D(n.getVelocity().clone().sub(p.getVelocity().clone()));
//			velocityDiff.mul(WPoly6(p.getNewPos(), n.getNewPos()));
//		}
//		return visc.mul(C);
//	}
	public static Vec3D vorticityForce(Particle p) {
		Vec3D vorticity = vorticity(p);
		if (vorticity.len() == 0) {
			// No direction for eta
			return new Vec3D ();
		}
		Vec3D eta = eta(p, vorticity.len());
		Vec3D n = eta.normalize();
		return Vec3D.mul(Vec3D.cross(n, vorticity), EPSILON_VORTICITY);
	}
	public static Vec3D eta(Particle p, float vorticityMag) {
		ArrayList<Particle> neighbors = p.getNeighbors();
		Vec3D eta = new Vec3D(0, 0, 0);
		for (Particle n : neighbors) {
			eta.add(Vec3D.mul(WViscosity(p.getNewPos(), n.getNewPos()), vorticityMag));
		}
		return eta;
	}
	public static void imposeConstraints(Bottle bottle, Particle p) {
		Vec3D pos = new Vec3D(p.getNewPos().x, p.getNewPos().y, bottle.center.z);
		float dis = Vec3D.dis(pos, bottle.center);
		if(dis>bottle.radius)
		{
			pos = Vec3D.add(bottle.center, 
					Vec3D.mul(Vec3D.minus(pos, bottle.center), bottle.radius/dis)
					);
			pos.z = p.getNewPos().z;
			p.setNewPos(pos);
			p.setVelocity(Vec3D.div(Vec3D.minus(pos, p.getOldPos()), deltaT));
		}		
	}

}
