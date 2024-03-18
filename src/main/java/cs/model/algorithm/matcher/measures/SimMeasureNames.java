package cs.model.algorithm.matcher.measures;

/**
 * To use a SimMeasure, it must have a measure name.
 * Then, add measure instance creation in ElementSimMeasures.java
 */
public class SimMeasureNames {
    // element similarity measure
    public final static String E_TYPE = "E_TYPE";
    public final static String ANCE = "E_ANCESTOR";

    // statement similarity measure
    public final static String IDEN = "IDEN";
    public final static String SAME_METHOD_BODY = "SAME_METHOD_BODY";

    public final static String PM = "PM";
    public final static String STMT_NAME = "STMT_NAME";
    public final static String NIT = "NIT";
    public final static String S_ABS = "S-ABS";
    public final static String STMT_SANDWICH = "STMT_SANDWICH";
    public final static String MS = "MS";
    public final static String IMTR = "IMTR";
    public final static String IMSR = "IMSR";
    public final static String NGRAM = "NGRAM";
    public final static String RETURN_STMT = "RETURN";
    public final static String T_MSIS = "T-MSIS";
    public final static String SAME_VALUE_RENAME = "SAME_VALUE_OR_RENAME";
    public final static String TOKEN_NEIGHBOR = "TOKEN_NEIGHBOR";
    public final static String TOKEN_SAME_STRUCT = "TOKEN_SAME_STRUCT";
    public final static String TOKEN_SANDWICH = "TOKEN_SANDWICH";
    public final static String T_ABS = "T-ABS";
    public final static String INNERSTMT = "INNERSTMT";

    // similarity measures for inner-stmt element
    public final static String I_IMTR = "I-IMTR";
    public final static String I_ABS = "I-ABS";
    public final static String I_MSIS = "I-MSIS";
}
