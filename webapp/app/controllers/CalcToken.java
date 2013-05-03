package controllers;

import java.math.BigInteger;

public class CalcToken {

	public static void main(String[] args) {
		int numNodes = 6;
		
		for(int i = 0; i < numNodes; i++) {
			String answer = calculate(i, numNodes);
			System.out.println("answer="+answer);
		}
	}

	private static String calculate(int i, int numNodes) {
		BigInteger mine = new BigInteger(""+2);
		BigInteger pow = mine.pow(127);
		BigInteger div = pow.divide(new BigInteger(numNodes+""));
		return ""+div.multiply(new BigInteger(i+""));
	}
}
