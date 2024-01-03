package cs.model.algorithm.ttmap;
import java.io.IOException;
import java.util.*;

/**
 * Map between token and type.
 */
public class TokenRangeTypeMap {
    private Map<TokenRange, String> tokenTypeMap;

    // all tokens in a file
    private List<TokenRange> tokens;
    public TokenRangeTypeMap(TreeTokensMap ttMap) throws IOException {
        Map<String, Set<String>> nameTypeMap = new HashMap<>();
        tokenTypeMap = new HashMap<>();
        tokens = ttMap.getTokenRanges();//获取标记范围列表

        // First phase: get all types except method receiver

        for (TokenRange token: tokens){
            TokenTypeCalculator calculator = new TokenTypeCalculator(token, ttMap);
            String type = calculator.getTypeOfNode();
            String tokenStr = ttMap.getTokenByRange(token);//获取标记范围对应的具体标记字符串
            tokenTypeMap.put(token, type);
            if (!nameTypeMap.containsKey(tokenStr))
                nameTypeMap.put(tokenStr, new HashSet<>());
            nameTypeMap.get(tokenStr).add(type);//更新类型名称映射，将类型与具体字符串对应起来
        }
        // Second phase: leverage the string map to get the type of remaining tokens

        for (TokenRange token: tokens){
            boolean condition1 = tokenTypeMap.get(token) != null &&
                    tokenTypeMap.get(token).equals(TokenTypeCalculator.QUALIFIED_PATH_NAME);
            boolean condition2 = tokenTypeMap.get(token) == null;
            if (condition1 || condition2) {
                TokenTypeCalculator calculator = new TokenTypeCalculator(token, ttMap);
                String type = calculator.getTypeOfNodeFromNameTypeMap(nameTypeMap);
                if (type != null)
                    tokenTypeMap.put(token, type);
            }
        }
    }

    public String getTokenType(TokenRange token){
        if (tokenTypeMap.get(token) != null)
            if (tokenTypeMap.get(token).equals(TokenTypeCalculator.QUALIFIED_PATH_NAME))
                return TokenTypeCalculator.NULL_TOKEN;
        return tokenTypeMap.get(token);
    }


    public String toString(TreeTokensMap ttMap) {
        String ret = "";
        for (TokenRange token: tokens){
            ret += token.toString(ttMap) + ": " + tokenTypeMap.get(token) + "\n";
        }
        return ret;
    }
}
