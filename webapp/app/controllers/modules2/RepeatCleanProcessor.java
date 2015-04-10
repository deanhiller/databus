package controllers.modules2;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PullProcessorAbstract;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.message.ChartVarMeta;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strips results from dupplicate results, used for exemple to detect changes in a stream of data.
 * 
 * Allows specification of columns to compare and a double tolerance for each column
 * 
 * examples : 
 * 
 * /api/json/repeatcleanV1(column0=value1,column1=value2,tolerance0=5.5,discardNull=true)/rawdataV1/data1
 * Discards row if value1 for row n+1 is between value1 for row n +- 5.5 and if value2 for row n+1 is equal to value2 for row n and discards any null value1 or value2
 * 
 * /api/json/repeatcleanV1/rawdataV1/data1
 * Discards row if value for row n+1 is equal value for row n
 * 
 * @author dbinder
 */
public class RepeatCleanProcessor extends PullProcessorAbstract {

    private static final Logger log = LoggerFactory.getLogger(SplinesPullProcessor.class);
    private static final String COL_PREFIX = "column";
    private static final String VAR_PREFIX = "tolerance";
    private static final String VAR_VALUE = "tolerance";
    private static final String DISCARD_NULL_OR_EMPTY = "discardNull";
    private ReadResult currentValue;
    private ReadResult previousValue;
//    private ReadResult nextValue;

    private CircularFifoBuffer master;
    private long end;

    private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
    private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.ONE, true, "Spline(version 3)", "Interpolation/Time alignment");

    static {
        ChartVarMeta meta1 = new ChartVarMeta();
        meta1.setLabel("Tolerance used for comparing values from \"value\" column to more or less its predecessor (no other column specified)");
        meta1.setNameInJavascript(VAR_VALUE);
        meta1.setDefaultValue("0");
        meta1.setRequired(true);
        meta1.setClazzType(Double.class);
        ChartVarMeta meta2 = new ChartVarMeta();
        meta2.setLabel("Column selection to work on different colum than \"value\" column which won't be used unless specified column0, column1, columnN can be used");
        meta2.setRequired(false);
        meta2.setNameInJavascript(COL_PREFIX + "X");
        meta2.setClazzType(String.class);
        ChartVarMeta meta3 = new ChartVarMeta();
        meta3.setLabel("Tolerance used for comparing values from columnX to more or less its predecessor (if no toleranceX is specified, 0 will be used for columnX)");
        meta3.setNameInJavascript(VAR_PREFIX + "X");
        meta3.setClazzType(Double.class);
        ChartVarMeta meta = new ChartVarMeta();
        meta.setLabel("if true, rows with null value will be discarded instead of being compared");
        meta.setNameInJavascript(DISCARD_NULL_OR_EMPTY);
        meta.setDefaultValue("20");
        meta.setClazzType(Integer.class);
        parameterMeta.put(meta1.getNameInJavascript(), meta1);
        parameterMeta.put(meta2.getNameInJavascript(), meta2);
        parameterMeta.put(meta3.getNameInJavascript(), meta3);
        parameterMeta.put(meta.getNameInJavascript(), meta);

        metaInfo.setDescription("This module takes data from the source module remove any repeated value");
    }
    private double variation;
    private List<String> columns = new ArrayList<String>();
    private List<Double> variations = new ArrayList<Double>();
    private boolean nullValue;

    @Override
    public MetaInformation getGuiMeta() {
        return metaInfo;
    }

    @Override
    protected int getNumParams() {
        return 0;
    }

//	@Override
//	public void start(VisitorInfo visitor) {
//		super.start(visitor);
//		
//		RowMeta rowMeta = getSingleChild().getRowMeta();
//		if(rowMeta != null) {
//			//very special case very bad hack but need to complete story and revisit for relational time series later
//			for(ColumnState state : columns) {
//				state.setColumnName(rowMeta.getValueColumn());
//				state.setTimeColumn(rowMeta.getTimeColumn());
//			}
//		}
//	}
    @Override
    public void initModule(Map<String, String> options, long start, long end) {
        super.initModule(options, start, end);
    }

    @Override
    public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
        if (log.isInfoEnabled()) {
            log.info("initialization of splines pull processor");
        }
        String newPath = super.init(path, nextInChain, visitor, options);

        Long startTime = null;
        if (params.getStart() != null) {
            startTime = params.getStart();
        }
        long end = Long.MAX_VALUE;
        if (params.getEnd() != null) {
            end = params.getEnd();
        }

        if (startTime == null) {
            startTime = Long.MIN_VALUE + 1;
        }

        String stringVariation = fetchProperty(VAR_VALUE, "0", options);
        this.variation = Double.parseDouble(stringVariation);

        String stringNullValue = fetchProperty(DISCARD_NULL_OR_EMPTY, "false", options);
        this.nullValue = Boolean.parseBoolean(stringNullValue);

        int i = 0;
        String col = fetchProperty(COL_PREFIX + (i), null, options);
        String var = fetchProperty(VAR_PREFIX + (i++), null, options);
        while (col != null) {
            columns.add(col);
            if (var != null) {
                variations.add(Double.parseDouble(var));
            } else {
                variations.add(0d);
            }
            col = fetchProperty(COL_PREFIX + (i), null, options);
            var = fetchProperty(VAR_PREFIX + (i++), null, options);
        }

        return newPath;
    }

    @Override
    public ReadResult read() {
        while (true) {
            pull();

            if (currentValue != null) {
                if(currentValue.isEndOfStream())
                    return currentValue;
                if (previousValue == null) {
                    if (!nullValue || containsAllColumn()) {
                        previousValue = currentValue;
                        return currentValue;
                    }
                } else if (!checkSimilarity(currentValue, previousValue)) {
                    previousValue = currentValue;
                    return currentValue;
                }
            } else {
                return null;
            }
        }
    }

    private boolean checkSimilarity(ReadResult firstValue, ReadResult sndValue) {
        if (firstValue.getRow() == null || sndValue.getRow() == null) {
            return false;
        }

        if (columns.isEmpty()) {
            final Object v1 = grabValue(firstValue);
            final Object v2 = grabValue(sndValue);
            if (nullValue && (v1 == null || v2 == null)) {
                return true;
            } else if (v1 == null && v2 == null) {
                return true;
            } else if (v1 == null || v2 == null) {
                return false;
            }
            if (variation != 0) {
                Number n1 = (Number) v1;
                Number n2 = (Number) v2;
                return n1.doubleValue() - variation <= n2.doubleValue() && n2.doubleValue() <= n1.doubleValue() + variation;
            }

            return v1.equals(v2);
        } else {
            int i = 0;
            for (String column : columns) {
                final Object v1 = grabColumnValue(firstValue, column);
                final Object v2 = grabColumnValue(sndValue, column);
                if (nullValue && (v1 == null || v2 == null)) {
                    continue;
                } else if (v1 == null && v2 == null) {
                    continue;
                } else if (v1 == null || v2 == null) {
                    return false;
                }
                Double var = variations.get(i++);
                if (var != 0) {
                    Number n1 = (Number) v1;
                    Number n2 = (Number) v2;
                    if (n1.doubleValue() - var > n2.doubleValue() || n2.doubleValue() > n1.doubleValue() + var) {
                        return false;
                    }
                } else if (!v1.equals(v2)) {
                    return false;
                }
            }
            return true;
        }
    }

    private Object grabValue(ReadResult r) {

        return grabColumnValue(r, r.getRow().getValCol());
    }

    private Object grabColumnValue(ReadResult r, String columnName) {
        return r.getRow().get(columnName);
    }

    private void pull() {
        PullProcessor ch = getChild();
        currentValue = ch.read();

    }

    private boolean containsAllColumn() {
        if (this.currentValue.getRow() == null) {
            return false;
        }
        if (columns.isEmpty()) {
            return currentValue.getRow().containsKey(currentValue.getRow().getValCol());
        }
        for (String column : columns) {
            if (!currentValue.getRow().containsKey(column)) {
                return false;
            }
        }
        return true;
    }

}
