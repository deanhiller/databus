package controllers.api;

import java.math.BigInteger;

import junit.framework.Assert;

import org.junit.Test;

import controllers.modules2.SplinesV3PullProcessor;

public class TestOffsetCode {

	@Test
	public void testOne() {
		long start = SplinesV3PullProcessor.calculateStartTime(-20L, 10L, 3L);
		Assert.assertEquals(-17, start);
		
		long start1 = SplinesV3PullProcessor.calculateStartTime(10L, 10L, 3L);
		Assert.assertEquals(13, start1);

		long start2 = SplinesV3PullProcessor.calculateStartTime(50L, 10L, 3L);
		Assert.assertEquals(53, start2);
		
		long start3 = SplinesV3PullProcessor.calculateStartTime(Long.MIN_VALUE, 10L, 3L);
		Assert.assertEquals(-9223372036854775807L, start3);
	}
	

}
