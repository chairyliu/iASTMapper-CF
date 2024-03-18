package cs.model.algorithm.matcher.rules;

/**
 * Names of mapping rules.
 *
 * Adding a new rule must add the rule name to this class.
 */
public class MatchRuleNames {
    // Stmt Rules
    public static final String IDEN = "Iden";
    public static final String STMT_NAME = "StmtSameName";
    public static final String BLOCK = "Block";
    public static final String RETURN_STMT = "ReturnStmt";
    public static final String STMT_SANDWICH = "StmtSandwich";
    public static final String IMSR = "IMSR";
    public static final String IMTR = "StmtTokenDice";

    // Token Rules
    public static final String TOKEN_SAME_STRUCT = "TokenSameStructure";
    public static final String TOKEN_SAME_STMT = "TokenSameStmt";
    public static final String TOKEN_SANDWICH = "TokenSandwich";
    public static final String TOKEN_MOVE = "TokenMove";
    public static final String STMT_NAME_TOKEN = "StmtNameToken";

    // Rules for inner-stmt elements
    public static final String I_IMTR = "I-IMTR";
    public static final String I_ABS = "I-ABS";
}
