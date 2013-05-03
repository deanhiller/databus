package controllers.modules2;

import java.math.BigInteger;

public class TokenCalculator
{
    public static void main(String[] args)
    {
    	int count = 4;
    	

    	for(int i = 0; i < count; i++) {
    		run(i, count);
    	}
    }

	private static void run(int nodeNum, int count) {
        try
        {
            Integer node  = Integer.valueOf(nodeNum);
            Integer total = Integer.valueOf(count);
            
            if(node < 0 || total <= 0 || node >= total)
                throw new RuntimeException("Invalid input: "+node+" "+total);
            
            BigInteger token = BigInteger.valueOf(node);
            BigInteger pow   = BigInteger.valueOf(2).pow(127).subtract(BigInteger.ONE);
            token = token.multiply(pow).divide(BigInteger.valueOf(total));

            System.out.println("Token "+node+" of "+total+": "+token.abs().toString());

	    return;
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            System.err.println("Usage: calculate_token.sh [node] [total]");
        }		
	}   
}
