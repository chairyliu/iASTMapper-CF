package cs.model.algorithm.matcher.measures;

import cs.model.algorithm.element.ProgramElement;

/**
 * Configure the used similarity measures and their comparison order
 */
public class SimMeasureConfiguration {

    // comparison order for stmt
    public static final String[] STMT_MEASURE_CONFIGURATION = {
            SimMeasureNames.IDEN,
            SimMeasureNames.ANCE,
            SimMeasureNames.IMSR,
            SimMeasureNames.IMTR,
            SimMeasureNames.S_ABS,
    };

    // comparison order for token
    public static final String[] TOKEN_MEASURE_CONFIGURATION = {
            SimMeasureNames.T_MSIS,
            SimMeasureNames.T_ABS,
    };

    // comparison order for inner-stmt element
    public static final String[] INNER_STMT_ELE_MEASURE_CONFIGURATION = {
            SimMeasureNames.I_MSIS,
            SimMeasureNames.I_IMTR,
            SimMeasureNames.I_ABS,
    };

    public static String[] getSimilarityMeasureConfiguration(ProgramElement element) {
        if (element.isStmt())
            return SimMeasureConfiguration.STMT_MEASURE_CONFIGURATION;
        if (element.isToken())
            return SimMeasureConfiguration.TOKEN_MEASURE_CONFIGURATION;
        if (element.isInnerStmtElement())
            return SimMeasureConfiguration.INNER_STMT_ELE_MEASURE_CONFIGURATION;
        throw new RuntimeException("Unknown element type");
    }
}
