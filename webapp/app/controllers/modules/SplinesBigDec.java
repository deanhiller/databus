package controllers.modules;

import java.math.BigDecimal;

public interface SplinesBigDec {
	/**
	 * get the interpolated value
	 * @param x new x value at which to interpolate
	 */
	BigDecimal getValue(long x);
	
//	/**
//	 * get the interpolated value
//	 * @param x new x value at which to interpolate
//	 */
//	BigDecimal getValue(BigDecimal x);
	
//	/**
//	 * get the valid range for interpolation
//	 * @return double[2] - min and max of the range
//	 */
//	BigDecimal[] getXrange();
	
	void setRawDataPoints(long[] x, BigDecimal[]y);
	
//	void setRawDataPoints(BigDecimal[] x, BigDecimal[]y);
	
//	BigDecimal[] getDerivatives();
//	
//	BigDecimal[] getCalculatedDerivatives();
//	
//	BigDecimal[] getInputData(int index);
	
}
