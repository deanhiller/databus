package controllers.modules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplinesBigDecBasic implements SplinesBigDec {
	protected static final Logger log = LoggerFactory.getLogger(SplinesBigDecBasic.class);
	protected BigDecimal[] x;
	protected BigDecimal[] y;
	protected BigDecimal[] m;
	
	//extra stuff that is not really needed to make the method work
	protected List<SplinesBigDecDataPoint> inputData;
	protected BigDecimal[] mCalculated;
	
	// scale stuff
	int scaleX;
	int scaleY;
	
	public SplinesBigDecBasic(){
		
	}

	@Override
	public BigDecimal getValue(long x) {
		return(getValue(BigDecimal.valueOf(x).setScale(20)));
	}

	@Override
	public BigDecimal getValue(BigDecimal x) {
		BigDecimal ynew = new BigDecimal(0);
		ynew.setScale(scaleY*2);

		if( (x.compareTo(this.x[0])==-1) || (x.compareTo(this.x[1])==1))
			throw new IllegalArgumentException("xnew not in the proper range, new = " + x +"-- should be ["+this.x[0]+","+this.x[1]+"]");

		BigDecimal deltaX = this.x[1].subtract(this.x[0]);
		if(log.isDebugEnabled())
			log.debug("deltaX {}",deltaX);
		BigDecimal t = (x.subtract(this.x[0])).divide(this.x[1].subtract(this.x[0]),RoundingMode.HALF_UP);
		if(log.isDebugEnabled())
			log.debug("t {}",t);

		//double h00 = (1.0 - t * t * (3.0 - 2.0*t)); // 1 - 3*t^2 + 2*t^3
		//double h10 = t * (1.0 - t * ( 2.0 - t ) ); // t - 2*t^2 + t^3
		//double h01 = t * t * ( 3.0 - 2.0 * t ); // 3t^2 - 2t^3
		//double h11 = t * t * ( t - 1.0 ); // t^3 - t^2
		BigDecimal n1 = new BigDecimal(1.0);
		BigDecimal n2 = new BigDecimal(2.0);
		BigDecimal n3 = new BigDecimal(3.0);

		BigDecimal h00 = n1.subtract(t.multiply(t.multiply(n3.subtract(n2.multiply(t)))));
		BigDecimal h10 = t.multiply(n1.subtract(t.multiply(n2.subtract(t))));
		BigDecimal h01 = t.multiply(t.multiply(n3.subtract(n2.multiply(t))));
		BigDecimal h11 = t.multiply(t.multiply(t.subtract(n1)));
		

		if(log.isDebugEnabled()) {
			log.debug("h00 {}",h00);
			log.debug("h10 {}",h10);
			log.debug("h01 {}",h01);
			log.debug("h11 {}",h11);
		}

		//		log.debug("y[0] {}",y[0]);
		//		log.debug("y[1] {}",y[1]);

//		ynew += h00*y[0];
//		ynew += h10*deltaX*m[0];
//		ynew += h01*y[1];
//		ynew += h11*deltaX*m[1];
		
		ynew = ynew.add(h00.multiply(y[0])).setScale(scaleY*2, RoundingMode.HALF_UP);
		ynew = ynew.add(h10.multiply(deltaX).multiply(m[0])).setScale(scaleY*2, RoundingMode.HALF_UP);
		ynew = ynew.add(h01.multiply(y[1])).setScale(scaleY*2, RoundingMode.HALF_UP);
		ynew = ynew.add(h11.multiply(deltaX).multiply(m[1])).setScale(scaleY*2, RoundingMode.HALF_UP);
		
		ynew = ynew.setScale(scaleY, RoundingMode.HALF_UP);
		
		return ynew;
	}

	@Override
	public BigDecimal[] getXrange() {
		return this.x;
	}

	@Override
	public void setRawDataPoints(long[] x, BigDecimal[] y) {
		BigDecimal[] xcon = new BigDecimal[4];
		for(int i =0; i<4; i++){
			xcon[i] = BigDecimal.valueOf(x[i]).setScale(20);
		}
		setRawDataPoints(xcon, y);
	}

	@Override
	public void setRawDataPoints(BigDecimal[] x, BigDecimal[] y) {
		if(x.length != 4){
			throw new IllegalArgumentException("Must have 4 data points, instead has: " + x.length);
		}
		if(x.length != y.length){
			throw new IllegalArgumentException("x and y must be the same length, instead x: " + x.length + " and y: " + y.length);
		}
		
		//find the max scale for x's and y's
		scaleX = 16; //Integer.MIN_VALUE;
		scaleY = 16; //Integer.MIN_VALUE;
		for(int i = 0; i<4;i++){
			scaleX = Math.max(scaleX, x[i].scale());
			scaleY = Math.max(scaleY, y[i].scale());
		}
		
		inputData = new ArrayList<SplinesBigDecDataPoint>(4);
		boolean sorted = true;
		// copy locally
		for(int i = 0; i<4;i++){
			inputData.add(new SplinesBigDecDataPoint(x[i].setScale(scaleX),y[i].setScale(scaleY)));
			//if (i >= 1 && x[i] < x[i - 1])
			if (i >= 1 && x[i].compareTo(x[i - 1])==-1)
				sorted = false;
		}
		// sort if necessary
		if (!sorted) {
			if(log.isDebugEnabled())
				log.debug("sorting");
			Collections.sort(inputData);
		}
		//pull out the information we need
		this.x = new BigDecimal[] {inputData.get(1).getX(),inputData.get(2).getX()};
		this.y = new BigDecimal[] {inputData.get(1).getY(),inputData.get(2).getY()};
		
		mCalculated = new BigDecimal[2];
		calculateSlopes();
		
		//limit the slope for the correct half
		// if not set to 0
		m = new BigDecimal[2];
		setSlopes();
	}
	
	protected void calculateSlopes(){
		//use Catmull-Rom calculation for the slope
		// the only way to divide by 0 is if 3 values have exactly the same x
		BigDecimal temp1;
		BigDecimal temp2;
		for(int i=1; i<=2;i++){
			temp1 = (inputData.get(i+1).getY().subtract(inputData.get(i-1).getY()));
			temp2 = (inputData.get(i+1).getX().subtract(inputData.get(i-1).getX()));
			mCalculated[i-1] = temp1.divide(temp2, RoundingMode.HALF_UP);
		}
	}
	
	protected void setSlopes(){
		m[0] = mCalculated[0];
		m[1] = mCalculated[1];
	}

	@Override
	public BigDecimal[] getDerivatives() {
		return this.m;
	}

	@Override
	public BigDecimal[] getCalculatedDerivatives() {
		return this.mCalculated;
	}

	@Override
	public BigDecimal[] getInputData(int index) {
		return this.inputData.get(index).getPoint();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
