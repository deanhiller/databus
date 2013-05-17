package misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class TestPattern {

	@Test
	public void testPattern() {
		Pattern pattern = Pattern.compile("^[A-Za-z0-9:/s \\.]+$");
		
		Matcher matcher = pattern.matcher("teimline");
		print(matcher, "timeline");
		
		Matcher matcher2 = pattern.matcher("teimline:%%");
		print(matcher2, "teimline:%%");

		Matcher matcher3 = pattern.matcher("http://localhost:9000/api/rawdataV1/wideTable/1367846618000/1368886618000");
		print(matcher3, "http:asdf");
	}

	private void print(Matcher matcher, String label) {
		if(matcher.matches())
			System.out.println("label matches");
		else
			System.out.println("label no match");
	}
}
