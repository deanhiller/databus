package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;

import play.data.validation.Validation;

import models.message.ChartVarMeta;

public class MetaInformation {

	private String moduleId;
	private Map<String, ChartVarMeta> parameterMeta;
	private NumChildren numChildren;
	private boolean isTimeAligning;
	private String guiLabel;
	private String description;
	private boolean needsAlignment;
	
	public MetaInformation(Map<String, ChartVarMeta> paramMeta, NumChildren num, boolean isTimeAligning, String guiLabel) {
		this.parameterMeta = paramMeta;
		this.numChildren = num;
		this.isTimeAligning = isTimeAligning;
		this.guiLabel = guiLabel;
		this.needsAlignment = true;
	}
	
	public MetaInformation(Map<String, ChartVarMeta> paramMeta, NumChildren num, boolean isTimeAligning, String guiLabel, boolean needsAlignment) {
		this(paramMeta, num, isTimeAligning, guiLabel);
		this.needsAlignment = needsAlignment;
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

	public NumChildren getNumChildren() {
		return numChildren;
	}

	public boolean isTimeAligning() {
		return isTimeAligning;
	}

	public boolean isAggregation() {
		if(numChildren == NumChildren.MANY)
			return true;
		return false;
	}

	public boolean isNeedsAlignment() {
		return needsAlignment;
	}

	public String getGuiLabel() {
		return guiLabel;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public boolean isTerminal() {
		if(numChildren == NumChildren.NONE)
			return true;
		return false;
	}

}
