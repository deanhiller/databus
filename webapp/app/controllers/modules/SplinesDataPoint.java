package controllers.modules;

public class SplinesDataPoint implements Comparable<SplinesDataPoint>{
	final double x;
	final double y;
	
	public SplinesDataPoint(double x, double y){
		this.x = x;
		this.y = y;
	}
	public double getX(){
		return this.x;
	}
	
	public double getY(){
		return this.y;
	}
	
	public double[] getPoint(){
		return new double[] {this.x, this.y};
	}
	
	@Override
	public int compareTo(SplinesDataPoint o) {
		if(this.x < o.getX()){
			return -1;
		} else if( this.x > o.getX()){
			return 1;
		}
		return 0;
	}
	
	public static void main(String[] args) {
		SplinesDataPoint test = new SplinesDataPoint(4.4, 5.5);
		System.out.println(test.getX());
	}
	
}
