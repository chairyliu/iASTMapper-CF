package cs.model.algorithm.matcher.matchers.utils;

import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.mappings.ElementMappings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MatcherUtil {

    /**
     * Get unmapped elements from a set of candidate elements
     * @param elements token elements to consider
     * @param eleMappings current element mappings
     * @return a set of unmapped elements
     */
    public static Set<ProgramElement> getUnmappedProgramElements(Collection<? extends ProgramElement> elements,
                                                                 ElementMappings eleMappings){
        Set<ProgramElement> ret = new HashSet<>();
        if (elements != null) {//elements是指传入的候选集
            for (ProgramElement element : elements) {
                if (!eleMappings.isMapped(element))
                    ret.add(element);//如果候选集中的元素还没有被映射，则加入ret，并返回，作为局部最优
            }
        }
        return ret;
    }
}
