package cs.model.algorithm.matcher.rules;

import cs.model.algorithm.element.ProgramElement;

/**
 * Configuration of mapping rules for statements, tokens and inner-stmt elements.
 */
public class MatchRulesConfiguration {
    // match rule configuration for statement mapping
    private static final String[] STMT_RULE_CONFIGURATION = {
            MatchRuleNames.IDEN,          // same statement
            MatchRuleNames.STMT_NAME,    // same name and same scope
            MatchRuleNames.BLOCK,              // block with parent element mapped
            MatchRuleNames.RETURN_STMT,               // return statement in the same method
            MatchRuleNames.STMT_SANDWICH,      // statements with surround statements mapped
            MatchRuleNames.IMSR,    // statements with enough descendant statements mapped
            MatchRuleNames.IMTR,    // statements with similar content
    };

    // match rule configuration for token mapping
    private static final String[] TOKEN_RULE_CONFIGURATION = {
            MatchRuleNames.TOKEN_SAME_STMT,     // tokens in the same statement with same or renamed value
            MatchRuleNames.TOKEN_SANDWICH,      // tokens with the sandwich measure
    };

    // configuration for inner-stmt element mapping
    private static final String[] INNER_STMT_ELE_RULE_CONFIGURATION = {
            MatchRuleNames.I_IMTR,                        // inner-stmt elements with enough common tokens.
            MatchRuleNames.I_ABS,                         // inner-stmt elements with left and right tokens mapped.
    };

    /**
     * Find the rule configuration for a given element
     * @param element the given statement, token or inner-stmt element.
     * @return the rule set.
     */
    public static String[] getRuleConfiguration(ProgramElement element) {
        if (element.isStmt())
            return MatchRulesConfiguration.STMT_RULE_CONFIGURATION;
        if (element.isToken())
            return MatchRulesConfiguration.TOKEN_RULE_CONFIGURATION;
        if (element.isInnerStmtElement())
            return MatchRulesConfiguration.INNER_STMT_ELE_RULE_CONFIGURATION;
        throw new RuntimeException("Unknown element type");
    }
}
