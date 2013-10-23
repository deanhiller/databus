package controllers.modules2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.mortbay.log.Log;

import play.mvc.results.BadRequest;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;

public class VariabilityCleanProcessor extends PushOrPullProcessor {

	private Float allowedVariation;
	private BigDecimal previousPoint = null;
	private CorrectionMethod correctionMethod = CorrectionMethod.REMOVE;
	private ComparisonType comparisonType = ComparisonType.PERCENT;
	
	@Override
	protected int getNumParams() {
		return 1;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
		String allowedVarStr = params.getParams().get(0);
		String correctionMethodStr = options.get("correctionMethod");
		String comparisonTypeStr = options.get("comparisonType");
		String msg = "module url /variabilityclean(correctionMethod=aCorrectionMethod,comparison=aComparisonType)/<allowedVariation>/ must be passed a long for allowed variation (ie. .2 for 20%) " +
				"and optionally a correctionMethod as an option describing what to do with a value outside that allowedVariation.  " +
				"Supported correctionMethods are PREVIOUS to use the previous value, ZERO to replace with 0 or REMOVE to remove the row entirely; if not specified REMOVE is assumed." +
				"Supported comparisonTypes are PERCENT to compare by percent difference (.2 = anything more then 20% different will have correction applied), SCALAR to compare by a scalar " +
				"(100 = any value more than 100 different from previous will have correction applied)";
		if (StringUtils.isNotBlank(correctionMethodStr)) {
			correctionMethod = CorrectionMethod.translate(StringUtils.lowerCase(correctionMethodStr));
			if (correctionMethod == null)
				throw new BadRequest(msg);
		}
		if (StringUtils.isNotBlank(comparisonTypeStr)) {
			comparisonType = ComparisonType.translate(StringUtils.lowerCase(comparisonTypeStr));
			if (comparisonType == null)
				throw new BadRequest(msg);
		}
		this.allowedVariation = parseFloat(allowedVarStr, msg);
		return newPath;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational tv) {
		
		if (previousPoint == null) {
			previousPoint = getValueEvenIfNull(tv);
			return tv;
		}
		
		if (comparisonType == ComparisonType.PERCENT) 
			return compareByPercent(tv);
		else if (comparisonType == ComparisonType.SCALAR)
			return compareByScalar(tv);
		else //coding error?
			throw new BadRequest("bad comparison type, this is a bug!");
			
		
	}
	
	private TSRelational compareByScalar(TSRelational tv) {
		if ((previousPoint.abs().subtract(getValueEvenIfNull(tv).abs())).abs().floatValue() > allowedVariation) 
			tv=applyCorrection(tv);

		if (tv != null)
			previousPoint = getValueEvenIfNull(tv);	
		return tv;
	}

	private TSRelational compareByPercent(TSRelational tv) {
		
		//TODO:JSC special case, any value is infinite percent difference from 0, what to do?
		if (previousPoint.compareTo(new BigDecimal("0.0"))==0) {
			previousPoint = getValueEvenIfNull(tv);
			return tv;
		}
		
		if (getValueEvenIfNull(tv)==null) {
			//jsc: what the hell?
			previousPoint = getValueEvenIfNull(tv);
			return tv;
		}
		else if (getValueEvenIfNull(tv).subtract(previousPoint.abs()).divide(previousPoint.abs(), 12, RoundingMode.HALF_DOWN).floatValue() > allowedVariation) {
			tv = applyCorrection(tv);
			if (tv != null)
				previousPoint = getValueEvenIfNull(tv);
			return tv;
		}
		else {
			previousPoint = getValueEvenIfNull(tv);
			return tv;
		}
	}

	private TSRelational applyCorrection(TSRelational tv) {
		if (correctionMethod == CorrectionMethod.REMOVE)
			return null;
		else if (correctionMethod == CorrectionMethod.PREVIOUS) {
			setValue(tv, previousPoint);
			return tv;
		}
		else if (correctionMethod == CorrectionMethod.ZERO) {
			setValue(tv, new BigDecimal("0.0"));
			return tv;
		}
		throw new BadRequest("No valid correctionMethod set");
	}

	private enum CorrectionMethod {
		PREVIOUS("previous"),
		ZERO("zero"),
		REMOVE("remove");
		
		private String code;
		private static final Map<String, CorrectionMethod> types;
		
		static {
			Map<String, CorrectionMethod> typesMap = new HashMap<String, CorrectionMethod>();
			for(CorrectionMethod type : CorrectionMethod.values()) {
				typesMap.put(type.getCode(), type);
			}
			types = Collections.unmodifiableMap(typesMap);
		}
		
		private CorrectionMethod(String code) {
			this.setCode(code);
		}
		
		public static CorrectionMethod translate(String code) {
			if(code == null) {
				return null;
			}
			return types.get(code);
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
	
	private enum ComparisonType {
		PERCENT("percent"),
		SCALAR("scalar");

		
		private String code;
		private static final Map<String, ComparisonType> types;
		
		static {
			Map<String, ComparisonType> typesMap = new HashMap<String, ComparisonType>();
			for(ComparisonType type : ComparisonType.values()) {
				typesMap.put(type.getCode(), type);
			} 
			types = Collections.unmodifiableMap(typesMap);
		}
		
		private ComparisonType(String code) {
			this.setCode(code);
		}
		
		public static ComparisonType translate(String code) {
			if(code == null) {
				return null;
			}
			return types.get(code);
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

}
