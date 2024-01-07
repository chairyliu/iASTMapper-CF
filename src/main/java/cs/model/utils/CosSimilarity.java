package cs.model.utils;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CosSimilarity {
    static final CosSimilarity INSTANCE = new CosSimilarity();

    public CosSimilarity() {
    }

    public Double cosineSimilarity(Map<CharSequence, Integer> leftVector, Map<CharSequence, Integer> rightVector) {
        if (leftVector != null && rightVector != null) {
            Set<CharSequence> intersection = this.getIntersection(leftVector, rightVector);
            double dotProduct = this.dot(leftVector, rightVector, intersection);
            double d1 = 0.0;
            BigDecimal d1B = new BigDecimal(Double.toString(d1));

            Integer value;
            for(Iterator var8 = leftVector.values().iterator(); var8.hasNext(); d1B.add(new BigDecimal(Double.toString(Math.pow((double)value, 2.0))))) {
                value = (Integer)var8.next();
            }

            double d2 = 0.0;
            BigDecimal d2B = new BigDecimal(Double.toString(d2));

//            Integer value;
            for(Iterator var10 = rightVector.values().iterator(); var10.hasNext(); d2B.add(new BigDecimal(Double.toString(Math.pow((double)value, 2.0))))) {
                value = (Integer)var10.next();
            }

            double cosineSimilarity = 0.0;
            BigDecimal cosineSimilarityB = new BigDecimal(Double.toString(cosineSimilarity));
            BigDecimal dotProductB = new BigDecimal(Double.toString(dotProduct));
            if (!(d1B.doubleValue() <= 0.0) && !(d2B.doubleValue() <= 0.0)) {
                cosineSimilarityB = dotProductB.divide((d1B.sqrt(new MathContext(5, RoundingMode.HALF_UP)).multiply(d2B.sqrt(new MathContext(5, RoundingMode.HALF_UP)))));
            } else {
                cosineSimilarityB = new BigDecimal(Double.toString(0.0));
            }

            return cosineSimilarityB.doubleValue();
        } else {
            throw new IllegalArgumentException("Vectors must not be null");
        }
    }

    private double dot(Map<CharSequence, Integer> leftVector, Map<CharSequence, Integer> rightVector, Set<CharSequence> intersection) {
        long dotProduct = 0L;

        CharSequence key;
        for(Iterator var6 = intersection.iterator(); var6.hasNext(); dotProduct += (long)(Integer)leftVector.get(key) * (long)(Integer)rightVector.get(key)) {
            key = (CharSequence)var6.next();
        }

        return (double)dotProduct;
    }

    private Set<CharSequence> getIntersection(Map<CharSequence, Integer> leftVector, Map<CharSequence, Integer> rightVector) {
        Set<CharSequence> intersection = new HashSet(leftVector.keySet());
        intersection.retainAll(rightVector.keySet());
        return intersection;
    }
}
