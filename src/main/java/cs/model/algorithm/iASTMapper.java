package cs.model.algorithm;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.actions.StmtTokenAction;
import cs.model.algorithm.actions.StmtTokenActionGenerator;
import cs.model.algorithm.actions.TreeEditAction;
import cs.model.algorithm.element.ElementTreeBuilder;
import cs.model.algorithm.element.ElementTreeUtils;
import cs.model.algorithm.element.ProgramElement;
import cs.model.algorithm.matcher.fastmatchers.BaseFastMatcher;
import cs.model.algorithm.matcher.fastmatchers.MultiFastMatcher;
import cs.model.algorithm.matcher.mappings.ElementMappings;
import cs.model.algorithm.matcher.mappings.MappingTransformer;
import cs.model.algorithm.matcher.mappings.SelectCrossFileMapping;
import cs.model.algorithm.matcher.matchers.BaseMatcher;
import cs.model.algorithm.matcher.matchers.InnerStmtEleMatcher;
import cs.model.algorithm.matcher.matchers.StmtMatcher;
import cs.model.algorithm.matcher.matchers.TokenMatcher;
import cs.model.algorithm.matcher.matchers.searchers.BestMappingSearcher;
import cs.model.algorithm.matcher.matchers.searchers.CandidateSearcher;
import cs.model.algorithm.matcher.matchers.searchers.CandidateSetsAndMaps;
import cs.model.algorithm.matcher.matchers.searchers.FilterDstCandidates;
import cs.model.algorithm.ttmap.TokenRangeTypeMap;
import cs.model.algorithm.ttmap.TreeTokensMap;
import cs.model.algorithm.utils.GumTreeUtil;
import cs.model.algorithm.utils.RangeCalculator;

import java.io.IOException;
import java.util.*;

/**
 * APIs of SE-Mapping
 */
public class iASTMapper {
    // root ITree node of source file and target file
    private final ITree src;
    private final ITree dst;

    // content of source file and target file
    private final String srcFileContent;
    private final String dstFileContent;

    // range calculator for source file and target file
    private final RangeCalculator srcRc;
    private final RangeCalculator dstRc;

    // tree token map for source file and target file
    private final TreeTokensMap srcTtMap;
    private final TreeTokensMap dstTtMap;

    // root element of source file and target file
    private final ProgramElement srcRootEle;
    private final ProgramElement dstRootEle;

    // statements of source file and target file
    private final List<ProgramElement> srcStmts;
    private final List<ProgramElement> dstStmts;

    // token type map for source and target file
    private final TokenRangeTypeMap srcTokenTypeMap;
    private final TokenRangeTypeMap dstTokenTypeMap;

    private final ElementMappings eleMappings;   // mappings of program elements
    private MappingStore ms;    // mappings of tree nodes

    private List<TreeEditAction> actions;

    private long treeBuildTime;
    private long ttMapBuildTime;
    private long mappingTime;
    private long actionGenerationTime;

    public static String used_ASTtype = "gt";  // 添加-ZN

    public static Set<String> used_rules = new HashSet<>();  // 添加-ZN
    public Set<ProgramElement> AllSrcStmts;
    public Set<ProgramElement> allDstStmts;
    public Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap;
    public Map<String, Set<ProgramElement>> AllSrcPathToTokensMap;
    public Map<String, Set<ProgramElement>> AllSrcPathToinnerStmtsMap;
    public Map<Set<ProgramElement>, String> allDstPathToStmtsMap = new HashMap<>();
    public Map<Set<ProgramElement>, String> allDstPathToTokensMap = new HashMap<>();
    public Map<Set<ProgramElement>, String> allDstPathToinnerStmtsMap = new HashMap<>();
    public List<ProgramElement> AllSrcStmtsToMap;
    public List<ProgramElement> AllSrcTokensToMap;
    public List<ProgramElement> AllSrcinnerStmtsToMap;
    public List<ProgramElement> allDstStmtsToMap = new ArrayList<>();
    public List<ProgramElement> allDstTokensToMap = new ArrayList<>();
    public List<ProgramElement> allDstinnerStmtsToMap = new ArrayList<>();
    public Map<String, Set<ProgramElement>> allDstValTokenMap;
//    public Map<String, List<ProgramElement>> srcStmtsToMap;
    public Map<String, ProgramElement> dstPathToRoot;
    public Map<String, MappingStore> dstPathToMs = new HashMap<>();

    //这里可以加一个if判断是否是对应的文件路径（与前面遍历pathMap合并），如果是，则执行，如果不是，则跳过，后面调用存好的快速映射阶段后的语句进行内外层循环
    public iASTMapper(String srcFileContent, String dstFileContent, String srcPath, String dstPath,
                      Map<String, List<ProgramElement>> srcStmtsToMap,Map<String, ProgramElement> dstPathToRoot,
                      Set<ProgramElement> allDstStmts) throws IOException {//这些src的步骤都可以提前计算，这样在for遍历的时候就不用重复算好几次
        this.dstPathToRoot = dstPathToRoot;
        this.allDstStmts = allDstStmts;
        used_rules.clear(); // 添加-ZN
        // We use gumtree's AST in SE-Mapping algorithm
        long time1 = System.currentTimeMillis();
//        this.src = GumTreeUtil.getITreeRoot(srcFileContent, "gt");
//        this.dst = GumTreeUtil.getITreeRoot(dstFileContent, "gt");
        this.src = GumTreeUtil.getITreeRoot(srcFileContent, used_ASTtype);//获取源AST
        this.dst = GumTreeUtil.getITreeRoot(dstFileContent, used_ASTtype);//目标AST
        long time2 = System.currentTimeMillis();
        treeBuildTime = time2 - time1;

        this.srcFileContent = srcFileContent;
        this.dstFileContent = dstFileContent;

        time1 = System.currentTimeMillis();
        this.srcRc = new RangeCalculator(srcFileContent);//建立行列表及行末索引
        this.dstRc = new RangeCalculator(dstFileContent);
        this.srcTtMap = new TreeTokensMap(srcRc, src, src, new HashSet<>());//将AST中的节点和token中的字符一一对应起来，建立位置索引
        this.dstTtMap = new TreeTokensMap(dstRc, dst, dst, new HashSet<>());//dst传了两次，考虑是否删除
        time2 = System.currentTimeMillis();
        ttMapBuildTime = time2 - time1;

        this.src.setParent(null);
        this.dst.setParent(null);
        this.srcTokenTypeMap = new TokenRangeTypeMap(srcTtMap);//建立AST节点与其类型的关系
        this.dstTokenTypeMap = new TokenRangeTypeMap(dstTtMap);

        this.srcRootEle = ElementTreeBuilder.buildElementTree(src, srcTtMap, srcTokenTypeMap, true);//建立程序元素树
        this.dstRootEle = ElementTreeBuilder.buildElementTree(dst, dstTtMap, dstTokenTypeMap, false);
        this.srcStmts = ElementTreeUtils.getAllStmtsPreOrder(srcRootEle);
        this.dstStmts = ElementTreeUtils.getAllStmtsPreOrder(dstRootEle);

        dstPathToRoot.put(dstPath,this.dstRootEle);

        srcStmtsToMap.put(srcPath, this.srcStmts);

        allDstStmts.addAll(this.dstStmts);

        this.eleMappings = new ElementMappings();
        this.eleMappings.addMapping(srcRootEle, dstRootEle);//建立了根节点之间的映射,这里先把一对一的根元素匹配在一起了，是否有问题
    }

    public void multiFastMapped(){
        BaseFastMatcher fastMatcher = new MultiFastMatcher(srcStmts, dstStmts, eleMappings);//快速映射（3种）
        fastMatcher.setTreeTokenMaps(srcTtMap, dstTtMap);
        fastMatcher.buildMappings();
    }

    //另外写一个类调用modifiele的dstPathToStmtMap存入All中，然后revison中写一个传all参数的方法，并用this和commit绑定
    public void preStoreAllDstCandidates(String srcPath, String dstPath, boolean isLastPath, List<ProgramElement> AllDstStmtsToMap,
                                         List<ProgramElement> AllDstTokensToMap, List<ProgramElement> AllDstinnerStmtsToMap,
                                         Map<Set<ProgramElement>, String> AllDstPathToStmtsMap,
                                         Map<Set<ProgramElement>, String> AllDstPathToTokensMap,
                                         Map<Set<ProgramElement>, String> AllDstPathToinnerStmtsMap,
                                         Map<String, Set<ProgramElement>> AllDstValTokenMap){
        FilterDstCandidates filterDstCandidates = new FilterDstCandidates(eleMappings, srcStmts, dstStmts, srcPath, dstPath,AllDstStmtsToMap, AllDstTokensToMap,
                AllDstinnerStmtsToMap,AllDstPathToStmtsMap, AllDstPathToTokensMap, AllDstPathToinnerStmtsMap,AllDstValTokenMap);
        filterDstCandidates.initStmtsAndTokens(srcStmts, dstStmts, srcPath, dstPath,AllDstStmtsToMap, AllDstTokensToMap,
                AllDstinnerStmtsToMap,AllDstPathToStmtsMap, AllDstPathToTokensMap, AllDstPathToinnerStmtsMap,AllDstValTokenMap);

        if (isLastPath == true){
            allDstStmtsToMap = filterDstCandidates.getAllDstStmtsToMap();
//            System.out.println(allDstStmtsToMap);
            allDstTokensToMap = filterDstCandidates.getAllDstTokensToMap();
            allDstinnerStmtsToMap = filterDstCandidates.getAllDstinnerStmtsToMap();
            allDstPathToStmtsMap = filterDstCandidates.getAllDstPathToStmtsMap();
//            System.out.println(allDstPathToStmtsMap);
            allDstPathToTokensMap = filterDstCandidates.getAllDstPathToTokensMap();
            allDstPathToinnerStmtsMap = filterDstCandidates.getAllDstPathToinnerStmtsMap();
            allDstValTokenMap = filterDstCandidates.getAllDstValTokenMap();
        }
    }
    /**
     * Build element mappings and tree mappings.
     */
    public void buildMappingsOuterLoop(List<ProgramElement> srcStmts,String srcPath,String dstPath) throws IOException {

        long time1 = System.currentTimeMillis();

        // Run fast matchers
//        BaseFastMatcher fastMatcher = new MultiFastMatcher(srcStmts, dstStmts, eleMappings);//快速映射（3种）
//        fastMatcher.setTreeTokenMaps(srcTtMap, dstTtMap);
//        fastMatcher.buildMappings();

        // Build candidate searcher and best mapping searcher
        CandidateSetsAndMaps candidateSetsAndMaps = new CandidateSetsAndMaps(eleMappings, srcStmts, allDstStmts,
                allDstStmtsToMap, allDstTokensToMap, allDstinnerStmtsToMap);
        FilterDstCandidates filterDstCandidates = new FilterDstCandidates(eleMappings, srcStmts, dstStmts, srcPath, dstPath,allDstStmtsToMap, allDstTokensToMap,
                allDstinnerStmtsToMap,allDstPathToStmtsMap, allDstPathToTokensMap, allDstPathToinnerStmtsMap, allDstValTokenMap);
//        System.out.println("Before: " + candidateSetsAndMaps);
        CandidateSearcher candidateSearcher = new CandidateSearcher(filterDstCandidates,candidateSetsAndMaps, eleMappings);//候选元素搜索器
//        System.out.println("After: " + candidateSearcher);
        BestMappingSearcher bestMappingSearcher = new BestMappingSearcher(candidateSearcher);//最佳元素搜索器
//        System.out.println("After: " + bestMappingSearcher);
        // iterative statement matcher
        BaseMatcher matcher1 = new StmtMatcher(eleMappings, bestMappingSearcher);//迭代的语句映射对象
        matcher1.setProcessToken(false);//是否处理token
        matcher1.calSrcElementsToMap();

        // iterative token matcher
        BaseMatcher matcher2 = new TokenMatcher(eleMappings, bestMappingSearcher);//迭代的词元素映射对象
        matcher2.setProcessToken(true);
        matcher2.calSrcElementsToMap();

        // iterative inner-stmt element matcher
        BaseMatcher matcher3 = new InnerStmtEleMatcher(eleMappings, bestMappingSearcher);//迭代的内部语句映射对象
        matcher3.setProcessToken(false);
        matcher3.calSrcElementsToMap();

        matcher1.setMatcher_type("STMT");
        matcher2.setMatcher_type("TOKEN");
        matcher3.setMatcher_type("INNER");

        while (true) {
            boolean findMappings1 = matcher1.buildMappingsInnerLoop();
            boolean findMappings2 = matcher2.buildMappingsInnerLoop();
            boolean findMappings3 = matcher3.buildMappingsInnerLoop();
            if (!findMappings2)
                break;
        }
//        System.out.println(eleMappings);

        //筛选跨文件的映射
        SelectCrossFileMapping selectCrossFileMapping = new SelectCrossFileMapping(eleMappings, srcPath,
                allDstPathToStmtsMap, allDstPathToTokensMap, allDstPathToinnerStmtsMap);

        //ms和matcher和srcpath一起绑定
        // map all inner-stmt elements
        //元素映射转换为AST节点映射（AST code mapping）
//        for (String dstToPath : dstPathToRoot.keySet()){
//            ProgramElement dstRoot = dstPathToRoot.get(dstToPath);
//            ms = MappingTransformer.elementMappingsToTreeMappings(eleMappings, srcRootEle, dstRoot);
//            dstPathToMs.put(dstToPath, ms);//这个集合里面存的dstRoot是对的，但是srcToDst这些内容没变
////            System.out.println(dstPathToMs);
//        }//这里想拿到一条srcPath对应所有dstPath的ms，但是ms不能累加

        ProgramElement dstRoot = dstPathToRoot.get(dstPath);
        ms = MappingTransformer.elementMappingsToTreeMappings(eleMappings, srcRootEle, dstRoot);
        long time2 = System.currentTimeMillis();
        mappingTime = time2 - time1;

        time1 = System.currentTimeMillis();
        List<Action> actionList = GumTreeUtil.getEditActions(ms);//生成AST编辑操作
        time2 = System.currentTimeMillis();
        actionGenerationTime = time2 - time1;
        if (actionList != null) {
            actions = new ArrayList<>();
            for (Action a : actionList) {
                TreeEditAction ea = new TreeEditAction(a, ms, srcRc, dstRc);//为AST节点添加信息
                if (ea.isJavadocRelated())
                    continue;
                actions.add(ea);
            }
        }
    }
//    public Map<String, List<ProgramElement>> getSrcStmtsToMap(){
//        return srcStmtsToMap;
//    }
    public MappingStore getMs(){return ms;};
    /**
     * Get the generated element mappings
     */
    public ElementMappings getEleMappings(){
        return eleMappings;
    }

    public ITree getSrc() {
        return src;
    }

    public ITree getDst() {
        return dst;
    }

    public ProgramElement getSrcRootEle() {
        return srcRootEle;
    }

    public ProgramElement getDstRootEle() {
        return dstRootEle;
    }

    public List<ProgramElement> getSrcStmts() {
        return srcStmts;
    }

    public List<ProgramElement> getDstStmts() {
        return dstStmts;
    }

    public String getSrcFileContent() {
        return srcFileContent;
    }

    public String getDstFileContent() {
        return dstFileContent;
    }

    public TreeTokensMap getSrcTtMap() {
        return srcTtMap;
    }

    public TreeTokensMap getDstTtMap() {
        return dstTtMap;
    }

    public TokenRangeTypeMap getSrcTokenTypeMap() {
        return srcTokenTypeMap;
    }

    public TokenRangeTypeMap getDstTokenTypeMap() {
        return dstTokenTypeMap;
    }

    public RangeCalculator getSrcRc() {
        return srcRc;
    }

    public RangeCalculator getDstRc() {
        return dstRc;
    }

    public long getTreeBuildTime() {
        return treeBuildTime;
    }

    public long getTtMapBuildTime() {
        return ttMapBuildTime;
    }

    public long getMappingTime() {
        return mappingTime;
    }

    public long getActionGenerationTime() {
        return actionGenerationTime;
    }

    /**
     * Get the generated tree edit actions that are implemented in GumTree
     */
    public List<TreeEditAction> getTreeEditActions() {
        return actions;
    }

    /**
     * Generate Stmt-Token edit actions.
     * Such actions are grouped along each pair of mapped statements.
     * It is more convenient to visualize the mappings of statements and tokens.
     */
    public List<StmtTokenAction> generateStmtTokenEditActions(){
        StmtTokenActionGenerator generator = new StmtTokenActionGenerator(srcStmts, dstStmts, eleMappings);
        List<StmtTokenAction> actionList = generator.generateActions(false);
        return generator.reorderActions(actionList);
    }
}
