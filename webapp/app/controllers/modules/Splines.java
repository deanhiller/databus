package controllers.modules;

public interface Splines {
	
	
	
	/**
	 * get the interpolated value
	 * @param x new x value at which to interpolate
	 */
	double getValue(double x);
	
	/**
	 * get the valid range for interpolation
	 * @return double[2] - min and max of the range
	 */
	double[] getXrange();
	
	void setRawDataPoints(double[] x, double[]y);
	
	double[] getDerivatives();
	
	double[] getCalculatedDerivatives();
	
	double[] getInputData(int index);
	
}
