package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;

import play.data.validation.Validation;

import models.message.ChartVarMeta;

public class MetaInformation {

	private Map<String, ChartVarMeta> parameterMeta;
	private boolean isStreamTerminator;
	private boolean isTimeAligning;
	
	public MetaInformation(Map<String, ChartVarMeta> paramMeta, boolean isStreamTerminator, boolean isTimeAligning) {
		this.parameterMeta = paramMeta;
		this.isStreamTerminator = isStreamTerminator;
		this.isTimeAligning = isTimeAligning;
	}
	
	public final void validate(Validation validation, Map<String, String> variableValues) {
		//for each field that is wrong, you should add validation
		//validation.addError("variables."+variableName, message);
		//and we will display the error with the field on the GUI

		//we do all basic validation here based on the ChartVarMeta of the each variable that came from the module
		for(Entry<String, ChartVarMeta> entry : parameterMeta.entrySet()) {
			ChartVarMeta meta = entry.getValue();
			Class<?> type = meta.getClazzType();
			if(type == null)
				type = String.class;
			String value = variableValues.get(entry.getKey());
			if(value == null && meta.isRequired()) {
				validation.addError(meta.getNameInJavascript(), "This field is required");
			} else {
				try {
					//test for conversion and if fail, then this is not correct type
					StandardConverters.convertFromString(type, value);
				} catch(RuntimeException e) {
					validation.addError(meta.getNameInJavascript(), "This field needs to be of type="+meta.getClazzType());
				}
			}
		}
		
		//here, the subclass can add further validation to ensure value is greater than 0, etc. etc.
		validateMore(validation, variableValues);
	}

	//you can override this validate function for specific validation logic
	protected void validateMore(Validation validation,
			Map<String, String> variableValues) {
	}

	public Map<String, ChartVarMeta> getParameterMeta() {
		return parameterMeta;
	}

	public boolean isStreamTerminator() {
		return isStreamTerminator;
	}

	public boolean isTimeAligning() {
		return isTimeAligning;
	}

}
