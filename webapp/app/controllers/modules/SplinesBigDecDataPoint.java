package controllers.modules;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SplinesBigDecDataPoint implements Comparable<SplinesBigDecDataPoint>{
	final BigDecimal x;
	final BigDecimal y;
	
	public SplinesBigDecDataPoint(BigDecimal x, BigDecimal y){
		this.x = x;
		this.y = y;
	}
	public BigDecimal getX(){
		return this.x;
	}
	
	public BigDecimal getY(){
		return this.y;
	}
	
	public BigDecimal[] getPoint(){
		return new BigDecimal[] {this.x, this.y};
	}
	
	@Override
	public int compareTo(SplinesBigDecDataPoint o) {
		return x.compareTo(o.getX());
	}
	
	public static void main(String[] args) {
		SplinesBigDecDataPoint test = new SplinesBigDecDataPoint(BigDecimal.valueOf(4.4), BigDecimal.valueOf(5.5));
		System.out.println(test.getX());
		System.out.println(BigDecimal.valueOf(Long.MIN_VALUE).precision());
		System.out.println(BigDecimal.valueOf(Long.MIN_VALUE).scale());
		//hence it should have at least a scale of 19 (probably 20) to represent a long
		System.out.println(BigDecimal.valueOf(Long.MIN_VALUE).setScale(19));
		System.out.println(BigDecimal.valueOf(1D/3D).precision());
		System.out.println(BigDecimal.valueOf(1D/3D).scale());
		System.out.println(BigDecimal.valueOf(1D/3D*1000D).precision());
		System.out.println(BigDecimal.valueOf(1D/3D*1000D).scale());
		System.out.println(BigDecimal.valueOf(1D/3D).setScale(19));
		//need to set the scale to get what we expect...
		System.out.println(BigDecimal.valueOf(1).setScale(19,BigDecimal.ROUND_HALF_UP).divide(BigDecimal.valueOf(3).setScale(19,BigDecimal.ROUND_HALF_UP),RoundingMode.HALF_UP));
		System.out.println(BigDecimal.valueOf(1).divide(BigDecimal.valueOf(3),RoundingMode.HALF_UP));
	}

}
