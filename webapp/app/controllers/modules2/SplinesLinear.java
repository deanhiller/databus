package controllers.modules2;

import java.math.BigDecimal;
import java.math.RoundingMode;

import controllers.modules.SplinesBigDec;

public class SplinesLinear implements SplinesBigDec {

	private long[] xaxis;
	private BigDecimal[] yaxis;

	@Override
	public BigDecimal getValue(long x) {
		if(x < xaxis[0])
			throw new IllegalArgumentException("The x you provided is less than the first value in our xaxis array so we cannot interpolate(that would be extrapolating)");
		else if(x > xaxis[xaxis.length-1])
			throw new IllegalArgumentException("The x you provided is more than the last value in our xaxis array so we cannot interpolate(that is extrapolating)");
		Integer index = null;
		for(int i = 1; i < xaxis.length; i++) {
			if(x <= xaxis[i]) {
				index = i-1;
				break;
			}
		}

		if(index == null)
			throw new RuntimeException("Bug, our preconditions should have caught something....x="+x+" xaxis0="+xaxis[0]+" xaxisLast="+xaxis[xaxis.length-1]);

		//Now, lets create a line formula from x1, x2, y1, y2 
		BigDecimal y1 = yaxis[index];
		BigDecimal y2 = yaxis[index+1];
		long x1 = xaxis[index];
		long x2 = xaxis[index+1];
		
		long range = x2 - x1;
		BigDecimal slopeDivisor = new BigDecimal(range);
		BigDecimal slopeNumerator = y2.subtract(y1);
		//equation of any line is y = mx +constant
		//so to find the constant, constant = y - mx using any point on that line
		BigDecimal firstPart = slopeNumerator.multiply(new BigDecimal(x2));
		BigDecimal firstPart2 = firstPart.divide(slopeDivisor, 5, RoundingMode.HALF_UP);
		BigDecimal constant = y2.subtract(firstPart2);
		
		//now y = mx + constant
		BigDecimal mxPart1 = slopeNumerator.multiply(new BigDecimal(x));
		BigDecimal mx = mxPart1.divide(slopeDivisor, 5, RoundingMode.HALF_UP);
		BigDecimal value = mx.add(constant);
		return value;
	}

	@Override
	public void setRawDataPoints(long[] x, BigDecimal[] y) {
		if(x == null || y == null)
			throw new IllegalArgumentException("x nor y can be null");
		else if(x.length < 2 || y.length < 2)
			throw new IllegalArgumentException("the x or y array is not 2 elements or greater and must be");
		else if(x.length != y.length)
			throw new IllegalArgumentException("y length must equal x length");
		this.xaxis = x;
		this.yaxis = y;
	}

}
