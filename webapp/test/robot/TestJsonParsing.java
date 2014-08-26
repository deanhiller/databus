package robot;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.deser.ValueInstantiator;
import org.codehaus.jackson.map.deser.std.MapDeserializer;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.junit.Test;

import controllers.modules2.framework.TSRelational;

public class TestJsonParsing {

	@Test
	public void testMarshalling() throws JsonParseException, JsonMappingException, IOException {
		//{"string":"something", "integer":552323, "float":5.4}
		String json = "{\"string\":\"something\", \"integer\":552323, \"float\":5.4}"; 
		Feature useBigDecimalForFloats = DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS;
		Feature useBigIntegerForInts = DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS;
		
		ObjectMapper m = new ObjectMapper();
		m.configure(useBigDecimalForFloats, true);
		m.configure(useBigIntegerForInts, true);
		m.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
		
		TypeFactory factory = m.getTypeFactory();
		MapType type = factory.constructMapType(TSRelational.class, String.class, Object.class);
		
		TSRelational result = m.readValue(json, type);
		System.out.println("data ="+result);
		
	}
}
