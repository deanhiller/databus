package controllers.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SplineLimitDerivative.java
 * Function for using a spline to interpolate values where the derivatives are
 * restricted to keep interpolated values between actual data points.
 * 
 * Reference: http://en.wikipedia.org/wiki/Cubic_Hermite_spline
 * 
 * @author Eric Kalendra
 *
 */
public class SplinesLimitDerivative extends SplinesBasic{
	private static final Logger log = LoggerFactory.getLogger(SplinesLimitDerivative.class);
	
	public SplinesLimitDerivative(){
		super();
	}
	
	@Override
	protected void setSlopes(){
		double deltaY = this.y[1]-this.y[0];
		if( deltaY > 0 ) {
			for(int i =0; i <= 1; i++){
				if(mCalculated[i]<0) {
					m[i] = 0;
				} else {
					m[i] = mCalculated[i];
				}
			}
		} else if(deltaY < 0) {
			for(int i =0; i <= 1; i++){
				if(mCalculated[i]>0) {
					m[i] = 0;
				} else {
					m[i] = mCalculated[i];
				}
			}
		} else {
			m[0] = 0D;
			m[1] = 0D;
		}
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
		testCase = new SplinesLimitDerivative();
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



