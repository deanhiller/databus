package models;

import java.util.HashMap;
import java.util.Map;

import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;
import com.alvazan.orm.api.base.anno.NoSqlId;

@NoSqlEmbeddable
public class UserChart {
	public enum ChartType {
		/**
		 * ChartType:
		 * 
		 * 			EMBEDDED_OBJECT			- 	single chart that has all data stored within the URL.
		 * 														To be used inside an HTML element such as a <div>
		 * 
		 * 			EMBEDDED_OBJECT_PAGE	-	single chart that has all data stored within the URL.
		 * 														To be used as an entire HMTL page.
		 * 
		 * 			SCRIPT								-	a chart loaded from the /public/Charts directory.
		 * 														To be used inside an HTML element such as a <div>
		 * 
		 * 			SCRIPT_PAGE						-	a chart loaded from the /public/Charts directory.
		 * 														To be used as an entire HMTL page.
		 */
		EMBEDDED_OBJECT("EMBEDDED_OBJECT"), EMBEDDED_OBJECT_PAGE("EMBEDDED_OBJECT_PAGE"), SCRIPT("SCRIPT"), SCRIPT_PAGE("SCRIPT_PAGE");
		
		private String code;

		private static Map<String, ChartType> codeToEnum = new HashMap<String, ChartType>();
		static {
			for(ChartType t : ChartType.values()) {
				codeToEnum.put(t.getName(), t);
			}
		}
		private ChartType(String dbCode) {
			this.code = dbCode;
		}
		
		public String getName() {
			return code;
		}
		
		public static ChartType convertFromCode(String code) {
			return codeToEnum.get(code);
		}
	}
	
	public enum ChartLibrary {
		/**
		 * Which charting library is used.
		 * 
		 * 			Currently support:
		 * 
		 * 					HIGHCHARTS
		 * 					D3 (not yet as of 6/30/13)
		 */
		HIGHCHARTS("HIGHCHARTS"); /*, D3*/
		
		private String code;

		private static Map<String, ChartLibrary> codeToEnum = new HashMap<String, ChartLibrary>();
		static {
			for(ChartLibrary t : ChartLibrary.values()) {
				codeToEnum.put(t.getName(), t);
			}
		}
		private ChartLibrary(String dbCode) {
			this.code = dbCode;
		}
		
		public String getName() {
			return code;
		}
		
		public static ChartLibrary convertFromCode(String code) {
			return codeToEnum.get(code);
		}
	}
	
	@NoSqlId
	private String chartName = "";
	
	private String chartType = ChartType.SCRIPT.getName();
	private String chartLibrary = ChartLibrary.HIGHCHARTS.getName();
	private String chartURI = "";
	private boolean publicViewable = true;
	
	/**
	 * Chart Override variables for script charts
	 * 
	 * 		scriptChart_SingleDBOverride:
	 * 					For use w/ single DB charts.  
	 * 
	 * 					In a script chart that supplies a single URL for the data, we can replace the
	 * 					DB (or stream name) associated w/ the data on the fly.
	 * 
	 * 					Example:
	 * 						ORIGINAL URL	-	/api/firstvaluesV1/50/aggregation/RSF_PV_1MIN?reverse=true
	 * 					
	 * 						scriptChart_SingleDBOverride = "/api/firstvaluesV1/50/rawdataV1/Wind303W_Glo90sPSP?reverse=true"
	 * 
	 * 						OVERRIDE URL	-	/api/firstvaluesV1/50/rawdataV1/Wind303W_Glo90sPSP?reverse=true
	 * 
	 */
	private String scriptChart_SingleDBOverride = "";
	
	/**
	 * 		scriptChart_ChartTitleOverride:
	 * 					For use w/ single DB charts.  
	 * 
	 * 					In a script chart that supplies a single URL for the data, we can replace the
	 * 					DB (or stream name) associated w/ the data on the fly.
	 * 
	 * 					Example:
	 * 						ORIGINAL Title	-	'3phaseRealPower D73937773/D73937763 +-2000';
	 * 					
	 * 						scriptChart_ChartTitleOverride = "My Cool New Title"
	 * 
	 * 						OVERRIDE Title	-	'My Cool New Title'
	 * 
	 */
	private String scriptChart_ChartTitleOverride = "";
	
	/**
	 * Legacy (need to have this removed...)
	 */
	private String scriptChart_SingleDBOriginal = "";
	
	/**
	 * 
	 * @param type				- 	ChartType
	 * @param library			- 	ChartLibrary
	 * @param name			- 	Chart name
	 * @param location		- 	For EMBEDDED: 	chart url
	 * 										For SCRIPT:			filename ONLY
	 */
	public UserChart(ChartType type, ChartLibrary library, String name, String location) {
		this.chartType = type.getName();
		this.chartLibrary = library.getName();
		this.chartName = name;
		this.chartURI = location;
		this.publicViewable = true;
		
		this.scriptChart_SingleDBOverride = "";
		this.scriptChart_ChartTitleOverride = "";
	}
	
	public boolean validate() {
		switch(getChartType()) {
			case EMBEDDED_OBJECT: {
				if(!this.chartURI.contains("charts/drawchart")) {
					return false;
				}
				break;
			}
			case EMBEDDED_OBJECT_PAGE: {
				if(!this.chartURI.contains("charts/chart")) {
					return false;
				}
				break;
			}
			case SCRIPT: {
				switch(getChartLibrary()) {
					case HIGHCHARTS: {
						/**
						 * See if this chart actually exists in the /public/Charts/HighCharts directory
						 */
						String chartName = "/public/Charts/HighCharts/" + chartURI;
						if(!play.vfs.VirtualFile.fromRelativePath(chartName).exists()) {
							return false;
						}
						break;
					}
					default: {
						return false;
					}
				}
				break;
			}
			case SCRIPT_PAGE: {
				switch(getChartLibrary()) {
					case HIGHCHARTS: {
						/**
						 * See if this chart actually exists in the /public/Charts/HighCharts directory
						 */
						String chartName = "/public/Charts/HighCharts/" + chartURI;
						if(!play.vfs.VirtualFile.fromRelativePath(chartName).exists()) {
							return false;
						}
						break;
					}
					default: {
						return false;
					}
				}
				break;
			}
			default: {
				return false;
			}
		}
		
		return true;
	}

	public ChartType getChartType() {
		return ChartType.convertFromCode(chartType);
	}

	public void setChartType(ChartType chartType) {
		this.chartType = chartType.getName();
	}

	public ChartLibrary getChartLibrary() {
		return ChartLibrary.convertFromCode(chartLibrary);
	}

	public void setChartLibrary(ChartLibrary chartLibrary) {
		this.chartLibrary = chartLibrary.getName();
	}

	public String getChartName() {
		return chartName;
	}

	public void setChartName(String chartName) {
		this.chartName = chartName;
	}

	public String getChartURI() {
		return chartURI;
	}
	
	public String getTrueChartURI() {
		StringBuffer trueChartURI = new StringBuffer(chartURI);
		
		if((getChartType() == ChartType.SCRIPT) || (getChartType() == ChartType.SCRIPT_PAGE)) {
			 
			 if((this.scriptChart_SingleDBOverride != null) && (!this.scriptChart_SingleDBOverride.equals(""))) {
				 trueChartURI.append("&override_db=" + this.scriptChart_SingleDBOverride);
			 }
			 
			 if((this.scriptChart_ChartTitleOverride != null) && (!this.scriptChart_ChartTitleOverride.equals(""))) {
				 trueChartURI.append("&override_title=" + this.scriptChart_ChartTitleOverride);
			 }
		 }
		return trueChartURI.toString();
	}
	
	public String getTruncatedChartURI(int count) {
		String extension = "...";
		if(count > chartURI.length()) {
			count = chartURI.length();
			extension = "";
		}
		return (chartURI.substring(0, count) + extension);
	}

	public void setChartURI(String chartURI) {
		this.chartURI = chartURI;
	}
	
	public boolean isPublicViewable() {
		return publicViewable;
	}

	public void setPublicViewable(boolean publicViewable) {
		this.publicViewable = publicViewable;
	}

	public String getScriptChart_SingleDBOverride() {
		return scriptChart_SingleDBOverride;
	}

	public void setScriptChart_SingleDBOverride(String scriptChart_SingleDBOverride) {
		this.scriptChart_SingleDBOverride = scriptChart_SingleDBOverride;
	}

	public String getScriptChart_ChartTitleOverride() {
		return scriptChart_ChartTitleOverride;
	}

	public void setScriptChart_ChartTitleOverride(
			String scriptChart_ChartTitleOverride) {
		this.scriptChart_ChartTitleOverride = scriptChart_ChartTitleOverride;
	}

	public String getScriptChart_SingleDBOriginal() {
		return scriptChart_SingleDBOriginal;
	}

	public void setScriptChart_SingleDBOriginal(String scriptChart_SingleDBOriginal) {
		this.scriptChart_SingleDBOriginal = scriptChart_SingleDBOriginal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((chartName == null) ? 0 : chartName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserChart other = (UserChart) obj;
		if (chartName == null) {
			if (other.chartName != null)
				return false;
		} else if (!chartName.equals(other.chartName))
			return false;
		return true;
	}
}
