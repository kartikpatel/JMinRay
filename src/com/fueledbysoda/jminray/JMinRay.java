/**
 * 
 */
package com.fueledbysoda.jminray;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * Minimal Ray Tracer
 * Java implementation of Paul Heckbert's business card raytracer
 * http://fabiensanglard.net/rayTracing_back_of_business_card/index.php
 * @author kartikpatel
 */
public class JMinRay {

	private Random generator = new Random();
	private int[] spheres = new int[] {247570,280596,280600,249748,18578,18577,231184,16,16};
	
	private class TraceResult {
		int result = 0;
		double distance = 1e9d;
		Vector3D bounce = null;
		
		public TraceResult(int result, double distance, Vector3D bounce) {
			this.result = result;
			this.distance = distance;
			this.bounce = bounce;
		}
	}
	
	/** 
	 * The Sampler (S) is a function that returns a pixel color for a given 
	 * Point Origin (o) and Vector Direction (d). It is recursive if it hits a 
	 * sphere. If no sphere is hit by the ray (and a ray always ends up 
	 * missing) then the function either returns a sky color gradient or a 
	 * floor color based on a checkerboard texture.
	 * 
	 * Remark the calls to R when computing the light direction: This is how 
	 * the soft-shadows are generated.
	 */
	private Vector3D sample(Vector3D origin, Vector3D direction) {
	      //Search for an intersection ray versus World.
	      TraceResult m = trace(origin, direction);
	    
	      if(m.result == 0) {
	    	  // No sphere found and the ray goes upward: Generate a sky color  
	    	  return new Vector3D(0.7, 0.6, 1.0).scalarMultiply(Math.pow(1-direction.getZ(), 4));
	      }

	      // A sphere was maybe hit.
	    
	      Vector3D h = origin.add(direction.scalarMultiply(m.distance)); // h = intersection coordinate
	      Vector3D l = (new Vector3D(9+generator.nextDouble(), 9+generator.nextDouble(), 16).add(h.scalarMultiply(-1))).normalize(); // 'l' = direction to light (with random delta for soft-shadows).
	      Vector3D r = direction.add(m.bounce.scalarMultiply(m.bounce.dotProduct(direction.scalarMultiply(-2)))); // r = The half-vector
	 
	      // Calculated the lambertian factor
	      double b = l.dotProduct(m.bounce);
	    
	      // Calculate illumination factor (lambertian coefficient > 0 or in shadow)?
	      if(b < 0 || trace(h,l).result > 0) {
	         b=0;
	      }
	   
	      // Calculate the color 'p' with diffuse and specular component 
	      double p = Math.pow(l.dotProduct(r.scalarMultiply(b > 0 ? 1 : 0)), 99);
	    
	      if((m.result & 1) == 1) {
	         h = h.scalarMultiply(0.2f); //No sphere was hit and the ray was going downward: Generate a floor color
	         return(((int)(Math.ceil(h.getX()) + Math.ceil(h.getY())) & 1) == 1 ? 
	        		 new Vector3D(3, 1, 1) : new Vector3D(3, 3, 3)).scalarMultiply(b * 0.2f + 0.1f);
	      }
	   
	      // m == 2 A sphere was hit. Cast an ray bouncing from the sphere surface.
	      return new Vector3D(p, p, p).add(sample(h,r).scalarMultiply(0.5f)); //Attenuate color by 50% since it is bouncing (* .5)
	  }
	
	/** 
	 * The Tracer is in charge of casting a Ray (Origin o ,Direction d). It 
	 * return an integer that is a code for the intersection test with the 
	 * spheres in the world (0=miss toward sky,1=miss toward floor, 
	 * 2=sphere hit). If a sphere is hit, it updates the references t (the 
	 * parametric value to compute the distance of intersection) and n 
	 * (the half-vector where the ray bounce).
	 */
	private TraceResult trace(Vector3D origin, Vector3D direction) { //, f& t,v& n 
		double distance = 1e9d;
		Vector3D bounce = null;
		int m = 0;
	     
		double p = -origin.getZ() / direction.getZ();
		if(.01 < p) {
			distance = p;
			bounce = new Vector3D(0, 0, 1);
			m=1;
		}
	       
		// The world is encoded in G, with 9 lines and 19 columns
		for(int k = 18; k >= 0; k--) { //For each columns of objects
			for(int j = 8; j >= 0; j--) { //For each line on that columns
	      
				if((spheres[j] & (1 << k)) > 0) { //For this line j, is there a sphere at column i ?
	        
					// There is a sphere but does the ray hits it ?
					
					Vector3D p2 = origin.add(new Vector3D(-k,0, -j - 4));
					
					double b = p2.dotProduct(direction);
					double c = p2.dotProduct(p2) - 1;
					double q = b * b - c;
	     
					// Does the ray hit the sphere ?
					if(q > 0) {
						// It does, compute the distance camera-sphere
						double s = -b - Math.sqrt(q);
	             
						if(s < distance && s > 0.01) {
							// So far this is the minimum distance, save it. And also
							// compute the bouncing ray vector into 'n'  
							distance = s;
							bounce = (p2.add(direction.scalarMultiply(distance))).normalize();
							m = 2;
						}
					}
				}
			}
		}
	    
		return new TraceResult(m, distance, bounce);
	}
	
	public void work() {
		try (FileOutputStream fos = new FileOutputStream("java.ppm")) {
	        fos.write(new String("P6 512 512 255 ").getBytes()); // The PPM Header is issued
		     
			Vector3D g = new Vector3D(-6, -16, 0).normalize(); // Camera direction
		    Vector3D a = new Vector3D(0, 0, 1).crossProduct(g).normalize().scalarMultiply(0.002f); // Camera up vector...Seem Z is pointing up
		    Vector3D b = g.crossProduct(a).normalize().scalarMultiply(0.002f); // The right vector, obtained via traditional cross-product
		    Vector3D c = a.add(b).scalarMultiply(-256).add(g); // See https://news.ycombinator.com/item?id=6425965 for more.
	
		     for(int y = 511; y >= 0; y--) { //For each column
		    	 for(int x = 511; x >= 0; x--) { //For each pixel in a line
		       
		    		 // Reuse the vector class to store not XYZ but a RGB pixel color
		    		 Vector3D p = new Vector3D(13, 13, 13); // Default pixel color is almost pitch black
		       
		    		 // Cast 64 rays per pixel (For blur (stochastic sampling) and soft-shadows. 
		    		 for(int r = 63; r >= 0; r--) { 
		          
		    			 // The delta to apply to the origin of the view (For Depth of View blur).
		    			 Vector3D t = a.scalarMultiply(generator.nextDouble() - 0.5f).scalarMultiply(99).add(b.scalarMultiply(generator.nextDouble() - 0.5f).scalarMultiply(99)); // A little bit of delta up/down and left/right
		                                           
		    			 // Set the camera focal point v(17,16,8) and Cast the ray 
		    			 // Accumulate the color returned in the p variable
		    			 p = sample(new Vector3D(17f, 16f, 8f).add(t), // Ray Origin 
		    					 t.scalarMultiply(-1.0f).add(((a.scalarMultiply(generator.nextDouble() + x)).add((b.scalarMultiply(generator.nextDouble() + y))).add(c)).scalarMultiply(16.0f)).normalize()).scalarMultiply(3.5f).add(p); // Ray Direction with random deltas // for stochastic sampling // +p for color accumulation
		    		 }
		    		 
		    		 fos.write((int) p.getX());
		             fos.write((int) p.getY());
		             fos.write((int) p.getZ());
		    	 }  
		     }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JMinRay minray = new JMinRay();
		minray.work();
	}

}
