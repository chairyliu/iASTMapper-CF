package cs.model.algorithm.matcher.measures;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.ml.AttrMetaInfo;
import weka.core.Attribute;

import java.util.ArrayList;
import java.util.List;

/**
 * Configure the used similarity measures and their comparison order
 */
public class SimMeasureConfiguration {

    // comparison order for stmt
    public static final String[] STMT_MEASURE_CONFIGURATION = {
            SimMeasureNames.IDEN,
//            SimMeasureNames.SAME_METHOD_BODY,
            SimMeasureNames.ANCE,
//            SimMeasureNames.NAME,
            SimMeasureNames.IMTR,
            SimMeasureNames.IMSR,
            SimMeasureNames.S_ABS,
    };

    // comparison order for token
    public static final String[] TOKEN_MEASURE_CONFIGURATION = {
            SimMeasureNames.T_MSIS,
//            SimMeasureNames.INNERSTMT, // 添加-ZN
//            SimMeasureNames.STRUCT,
//            SimMeasureNames.TOKEN_SANDWICH,
//            SimMeasureNames.TOKEN_NEIGHBOR,
            SimMeasureNames.T_ABS,
//            SimMeasureNames.INNERSTMT, // 注释-ZN
//            SimMeasureNames.ANCESTOR,
//            SimMeasureNames.SAME_VALUE_RENAME,
    };

    // comparison order for inner-stmt element
    public static final String[] INNER_STMT_ELE_MEASURE_CONFIGURATION = {
//            SimMeasureNames.INNER_STMT_ELE_NAME,
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

    private static List<AttrMetaInfo> getAttrMetaInfoWithClass(){
        List<AttrMetaInfo> infos = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        categories.add("accurate");
        categories.add("inaccurate");
        AttrMetaInfo classAttr = new AttrMetaInfo("accuracy", Attribute.NOMINAL, categories);
        infos.add(classAttr);
        return infos;
    }

    /**
     * Generate attribute meta info list in order to build a dataset for training classifier
     */
    public static List<AttrMetaInfo> getAttrMetaInfosForStmtMapping(){
        List<AttrMetaInfo> infos = getAttrMetaInfoWithClass();
        AttrMetaInfo srcTokenAttr = new AttrMetaInfo("srcToken", Attribute.NUMERIC);
        AttrMetaInfo dstTokenAttr = new AttrMetaInfo("dstToken", Attribute.NUMERIC);
        infos.add(srcTokenAttr);
        infos.add(dstTokenAttr);
        for (String measureName: STMT_MEASURE_CONFIGURATION) {
            AttrMetaInfo attr = new AttrMetaInfo(measureName, Attribute.NUMERIC);
            infos.add(attr);
        }
        return infos;
    }

    public static List<AttrMetaInfo> getAttrMetaInfosForTokenMapping() {
        List<AttrMetaInfo> infos = getAttrMetaInfoWithClass();
        for (String measureName: TOKEN_MEASURE_CONFIGURATION) {
            AttrMetaInfo attr = new AttrMetaInfo(measureName, Attribute.NUMERIC);
            infos.add(attr);
        }
        return infos;
    }
}
