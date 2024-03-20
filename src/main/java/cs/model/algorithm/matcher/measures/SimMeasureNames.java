package cs.model.algorithm.matcher.measures;

/**
 * To use a SimMeasure, it must have a measure name.
 * Then, add measure instance creation in ElementSimMeasures.java
 */
public class SimMeasureNames {

    // similarity measures for statement
    public final static String IDEN = "IDEN";
    public final static String ANCE = "E_ANCESTOR";
    public final static String IMTR = "IMTR";
    public final static String IMSR = "IMSR";
    public final static String S_ABS = "S-ABS";


    // similarity measures for token
    public final static String T_MSIS = "T-MSIS";
    public final static String T_ABS = "T-ABS";


    // similarity measures for inner-stmt element
    public final static String I_MSIS = "I-MSIS";
    public final static String I_IMTR = "I-IMTR";
    public final static String I_ABS = "I-ABS";
}
