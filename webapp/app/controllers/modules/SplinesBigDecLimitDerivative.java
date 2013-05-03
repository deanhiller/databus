package controllers.modules;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplinesBigDecLimitDerivative extends SplinesBigDecBasic{
	private static final Logger log = LoggerFactory.getLogger(SplinesBigDecLimitDerivative.class);
	
	public SplinesBigDecLimitDerivative(){
		super();
	}
	
	@Override
	protected void setSlopes(){
		BigDecimal deltaY = this.y[1].subtract(this.y[0]);
		if( deltaY.compareTo(new BigDecimal(0D))==1 ) {
			for(int i =0; i <= 1; i++){
//				if(mCalculated[i]<0) {
				if(mCalculated[i].compareTo(new BigDecimal(0D))==-1){
					m[i] = new BigDecimal(0D);
				} else {
					m[i] = mCalculated[i];
				}
			}
		} else if(deltaY.compareTo(new BigDecimal(0D))==-1) {
			for(int i =0; i <= 1; i++){
//				if(mCalculated[i]>0) {
				if(mCalculated[i].compareTo(new BigDecimal(0D))==1){
					m[i] = new BigDecimal(0D);
				} else {
					m[i] = mCalculated[i];
				}
			}
		} else {
			m[0] = new BigDecimal(0D);
			m[1] = new BigDecimal(0D);
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
