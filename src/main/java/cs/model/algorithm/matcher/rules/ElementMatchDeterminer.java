package cs.model.algorithm.matcher.rules;

import cs.model.algorithm.matcher.mappings.ElementMapping;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.measures.ElementSimMeasures;
import cs.model.algorithm.matcher.rules.innerstmt.InnerStmtEleSandwichRule;
import cs.model.algorithm.matcher.rules.innerstmt.InnerStmtEleTokenDiceRule;
import cs.model.algorithm.matcher.rules.stmt.*;
import cs.model.algorithm.matcher.rules.stmt.specialstmts.BlockMatchRule;
import cs.model.algorithm.matcher.rules.stmt.specialstmts.ReturnOrThrowStmtRule;
import cs.model.algorithm.matcher.rules.token.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Determine if two elements can be mapped with multiple rules
 */
public class ElementMatchDeterminer {
    private ElementMappings eleMappings;

    // Cache of legal element mappings
    private Set<ElementMapping> legalMappings;

    // Cache of illegal element mappings
    private Set<ElementMapping> illegalMappings;
    public ElementMatchDeterminer(ElementMappings eleMappings) {
        this.eleMappings = eleMappings;
        this.legalMappings = new HashSet<>();
        this.illegalMappings = new HashSet<>();
    }

    /**
     * Determine if the mapping in the given similarity measures is reasonable
     * @param simMeasures the similarity measures containing value of different similarity measures.
     * @return whether the mapping is reasonable
     */
    //主要是根据第一套规则进行过滤，determineCanBeMapped返回布尔值，只要当前的匹配对满足第一套的任一规则，则返回true，并且将当前匹配对加入合法映射的集合中
    public boolean determine(ElementSimMeasures simMeasures) {
        if (legalMappings.contains(simMeasures.getElementMapping()))
            return true;
        if (illegalMappings.contains(simMeasures.getElementMapping()))
            return false;
        //这里用的第一套
        String[] ruleNames = MatchRulesConfiguration.getRuleConfiguration(simMeasures.getSrcEle());
        for (String ruleName: ruleNames) {
            //getRuleConfiguration和getElementMatchRule统一起来
            ElementMatchRule rule = getElementMatchRule(ruleName);
            //determineCanBeMapped是第一套
            if (rule.determineCanBeMapped(simMeasures, eleMappings)) {
                legalMappings.add(simMeasures.getElementMapping());
                return true;
            }
        }
        illegalMappings.add(simMeasures.getElementMapping());
        return false;
    }

    public ElementMatchRule getElementMatchRule(String ruleName) {  // 修改public-ZN
        ElementMatchRule rule;
        switch (ruleName) {
            case MatchRuleNames.IDEN:
                rule = new IdenticalStmtMatchRule();
                break;
            case MatchRuleNames.STMT_NAME:
                rule = new StmtNameMatchRule();
                break;
            case MatchRuleNames.BLOCK:
                rule = new BlockMatchRule();
                break;
            case MatchRuleNames.RETURN_STMT:
                rule = new ReturnOrThrowStmtRule();
//                System.out.println("=====RETURN=====");
                break;
            case MatchRuleNames.STMT_SANDWICH:
                rule = new StmtSandwichRule();
                break;
            case MatchRuleNames.IMSR:
                rule = new DescendantStmtMatchRule();
                break;
            case MatchRuleNames.IMTR:
                rule = new StmtTokenDiceRule();
                break;
            case MatchRuleNames.TOKEN_SAME_STMT:
                rule = new TokenSameStmtRule();
                break;
            case MatchRuleNames.TOKEN_SANDWICH:
                rule = new TokenSandwichRule();
                break;
            case MatchRuleNames.I_IMTR:
                rule = new InnerStmtEleTokenDiceRule();
                break;
            case MatchRuleNames.I_ABS:
                rule = new InnerStmtEleSandwichRule();
                break;
            default:
                rule = null;
        }
        return rule;
    }
}
