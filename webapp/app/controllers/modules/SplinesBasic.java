package controllers.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplinesBasic implements Splines {
	protected static final Logger log = LoggerFactory.getLogger(SplinesBasic.class);
	protected double[] x;
	protected double[] y;
	protected double[] m;
	
	//extra stuff that is not really needed to make the method work
	protected List<SplinesDataPoint> inputData;
	protected double[] mCalculated;
	
	public SplinesBasic(){
		
	}
	
	public void setRawDataPoints(double[] x, double[] y){
		if(x.length != 4){
			throw new IllegalArgumentException("Must have 4 data points, instead has: " + x.length);
		}
		if(x.length != y.length){
			throw new IllegalArgumentException("x and y must be the same length, instead x: " + x.length + " and y: " + y.length);
		}
		inputData = new ArrayList<SplinesDataPoint>(4);
		boolean sorted = true;
		// copy locally
		for(int i = 0; i<4;i++){
			inputData.add(new SplinesDataPoint(x[i],y[i]));
			if (i >= 1 && x[i] < x[i - 1])
				sorted = false;
		}
		// sort if necessary
		if (!sorted) {
			if(log.isDebugEnabled())
				log.debug("sorting");
			Collections.sort(inputData);
		}
		//pull out the information we need
		this.x = new double[] {inputData.get(1).getX(),inputData.get(2).getX()};
		this.y = new double[] {inputData.get(1).getY(),inputData.get(2).getY()};
		
		mCalculated = new double[2];
		
		calculateSlopes();
		
		//limit the slope for the correct half
		// if not set to 0
		m = new double[2];
		setSlopes();
	}
	
	protected void setSlopes(){
		m[0] = mCalculated[0];
		m[1] = mCalculated[1];
	}
	
	protected void calculateSlopes(){
		//use Catmull-Rom calculation for the slope
		// the only way to divide by 0 is if 3 values have exactly the same x
		for(int i=1; i<=2;i++){
			mCalculated[i-1] = (inputData.get(i+1).getY() - inputData.get(i-1).getY())/
					(inputData.get(i+1).getX() - inputData.get(i-1).getX());
		}
	}

	@Override
	public double getValue(double x) {
		double ynew = 0.0;

		if( x < this.x[0] || x > this.x[1])
			throw new IllegalArgumentException("xnew not in the proper range, new = " + x +"-- should be ["+this.x[0]+","+this.x[1]+"]");

		double deltaX = this.x[1]-this.x[0];
		if(log.isDebugEnabled())
			log.debug("deltaX {}",deltaX);
		double t = (x - this.x[0])/(this.x[1]-this.x[0]);
		if(log.isDebugEnabled())
			log.debug("t {}",t);

		double h00 = (1.0 - t * t * (3.0 - 2.0*t)); // 1 - 3*t^2 + 2*t^3
		double h10 = t * (1.0 - t * ( 2.0 - t ) ); // t - 2*t^2 + t^3
		double h01 = t * t * ( 3.0 - 2.0 * t ); // 3t^2 - 2t^3
		double h11 = t * t * ( t - 1.0 ); // t^3 - t^2

		if(log.isDebugEnabled()) {
			log.debug("h00 {}",h00);
			log.debug("h10 {}",h10);
			log.debug("h01 {}",h01);
			log.debug("h11 {}",h11);
		}

		//		log.debug("y[0] {}",y[0]);
		//		log.debug("y[1] {}",y[1]);

		ynew += h00*y[0];
		ynew += h10*deltaX*m[0];
		ynew += h01*y[1];
		ynew += h11*deltaX*m[1];

		return ynew;
	}

	@Override
	public double[] getXrange() {
		return this.x;
	}
	
	@Override
	public double[] getDerivatives(){
		return this.m;
	}
	
	@Override
	public double[] getCalculatedDerivatives(){
		return this.mCalculated;
	}
	
	@Override
	public double[] getInputData(int index){
		return this.inputData.get(index).getPoint();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double[] x;
		double[] y;
		Splines testCase;
		
		x = new double[] { 1.0,2.0,4.0,3.0};
		y = new double[] { 5.0,2.0,-2.0,3.0};
		testCase = new SplinesBasic();
		testCase.setRawDataPoints(x, y);
		for( int i = 0; i<4 ; i ++){
			System.out.print("i : " + i + " -- " +testCase.getInputData(i)[0]);
			System.out.print(" : ");
			System.out.println(testCase.getInputData(i)[1]);
		}
		System.out.println("mOrig[0] : " + testCase.getCalculatedDerivatives()[0]);
		System.out.println("mOrig[1] : " + testCase.getCalculatedDerivatives()[1]);
		System.out.println("m[0] : " + testCase.getDerivatives()[0]);
		System.out.println("m[1] : " + testCase.getDerivatives()[1]);
		double xnew = 2.1;
		System.out.println("xnew: " + xnew + "  value: " + testCase.getValue(xnew));
		
		System.out.println("---");
		
		x = new double[] { 1.0,2.0,4.0,3.0};
		y = new double[] { 1.0,2.0,4.0,3.0};
		testCase.setRawDataPoints(x, y);
		for( int i = 0; i<4 ; i ++){
			System.out.print("i : " + i + " -- " +testCase.getInputData(i)[0]);
			System.out.print(" : ");
			System.out.println(testCase.getInputData(i)[1]);
		}
		System.out.println("mOrig[0] : " + testCase.getCalculatedDerivatives()[0]);
		System.out.println("mOrig[1] : " + testCase.getCalculatedDerivatives()[1]);
		System.out.println("m[0] : " + testCase.getDerivatives()[0]);
		System.out.println("m[1] : " + testCase.getDerivatives()[1]);
		
		xnew = 2.1;
		System.out.println("xnew: " + xnew + "  value: " + testCase.getValue(xnew));
	}

}


