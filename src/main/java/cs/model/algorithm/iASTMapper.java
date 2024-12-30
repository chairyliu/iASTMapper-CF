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
import cs.model.algorithm.matcher.matchers.searchers.*;
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

    // mappings of program elements
    private final ElementMappings eleMappings;

    // mappings of tree nodes
    private MappingStore ms;

    protected Map<String, String> pathMap;
    private List<TreeEditAction> actions;

    private long treeBuildTime;
    private long ttMapBuildTime;
    private long mappingTime;
    private long actionGenerationTime;

    // We use gumtree's AST in SE-Mapping algorithm
    public static String used_ASTtype = "gt";

    public static Set<String> used_rules = new HashSet<>();
    public List<ProgramElement> allDstStmts;
    public Map<String, Set<ProgramElement>> allDstPathToStmtsMap = new HashMap<>();
    public Map<String, Set<ProgramElement>> allSrcPathToStmtsMap = new HashMap<>();
    public List<ProgramElement> allDstStmtsList = new ArrayList<>();
    public List<ProgramElement> allDstTokensList = new ArrayList<>();
    public List<ProgramElement> allDstinnerStmtsList = new ArrayList<>();
    public Map<String, Set<ProgramElement>> allDstValTokenMap;
    public Map<String, List<ProgramElement>> dstPathToStmtsMap;
    public Map<String, ProgramElement> dstPathToRoot;
    public Map<String, ProgramElement> srcPathToRoot;
    public Map<Map<String, String>, String> crossFileMap = new HashMap<>();
    public Map<String, List<String>> oneToMultiMap = new HashMap<>();
    public CandidateSetsAndMaps candidateSetsAndMaps;
    public FilterDstCandidates filterDstCandidates;


    /**
     * Constructor for initializing and mapping source and destination ASTs using the SE-Mapping algorithm
     */
    public iASTMapper(String srcFileContent, String dstFileContent, ITree src, ITree dst, String srcPath, String dstPath,
                      Map<String,String> pathMap, Map<String, List<ProgramElement>> srcPathToStmtsMap,
                      Map<String, List<ProgramElement>> dstPathToStmtsMap, Map<String, ProgramElement> dstPathToRoot,
                      Map<String, ProgramElement> srcPathToRoot, List<ProgramElement> allDstStmts) throws IOException {
        this.dstPathToRoot = dstPathToRoot;
        this.allDstStmts = allDstStmts;
        this.srcPathToRoot = srcPathToRoot;
        this.dstPathToStmtsMap = dstPathToStmtsMap;
        this.pathMap = pathMap;
        used_rules.clear();

        this.srcFileContent = srcFileContent;
        this.dstFileContent = dstFileContent;
        this.src = src;
        this.dst = dst;

        long time1 = System.currentTimeMillis();
        this.srcRc = new RangeCalculator(srcFileContent);
        this.dstRc = new RangeCalculator(dstFileContent);
        this.srcTtMap = new TreeTokensMap(srcRc, src, src, new HashSet<>());
        this.dstTtMap = new TreeTokensMap(dstRc, dst, dst, new HashSet<>());
        long time2 = System.currentTimeMillis();
        ttMapBuildTime = time2 - time1;

        this.src.setParent(null);
        this.dst.setParent(null);
        this.srcTokenTypeMap = new TokenRangeTypeMap(srcTtMap);
        this.dstTokenTypeMap = new TokenRangeTypeMap(dstTtMap);

        this.srcRootEle = ElementTreeBuilder.buildElementTree(src, srcTtMap, srcTokenTypeMap, true);
        this.dstRootEle = ElementTreeBuilder.buildElementTree(dst, dstTtMap, dstTokenTypeMap, false);
        this.srcStmts = ElementTreeUtils.getAllStmtsPreOrder(srcRootEle);
        this.dstStmts = ElementTreeUtils.getAllStmtsPreOrder(dstRootEle);

        srcPathToRoot.put(srcPath,this.srcRootEle);
        dstPathToRoot.put(dstPath,this.dstRootEle);

        srcPathToStmtsMap.put(srcPath, this.srcStmts);
        dstPathToStmtsMap.put(dstPath, this.dstStmts);

        allDstStmts.addAll(this.dstStmts);

        this.eleMappings = new ElementMappings();
        this.eleMappings.addMapping(srcRootEle, dstRootEle);
    }

    public void identicalMapping(){
        BaseFastMatcher fastMatcher = new MultiFastMatcher(srcStmts, dstStmts, eleMappings);
        fastMatcher.setTreeTokenMaps(srcTtMap, dstTtMap);
        fastMatcher.buildMappings();
    }

    /**
     * Aggregate all modified target statements in a commit and store them with their respective paths.
     */
    public void preStoreAllDstCandidates(String srcPath, String dstPath, List<ProgramElement> AllDstStmtsList,
                                         List<ProgramElement> AllDstTokensList, List<ProgramElement> AllDstinnerStmtsList,
                                         Map<String, Set<ProgramElement>> AllDstPathToStmtsMap,
                                         Map<String, Set<ProgramElement>> AllDstValTokenMap,
                                         Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap){

        FilterDstCandidates filterDstCandidates = new FilterDstCandidates(eleMappings, AllDstStmtsList, AllDstTokensList,
                AllDstinnerStmtsList, AllDstPathToStmtsMap, AllDstValTokenMap, AllSrcPathToStmtsMap);
        filterDstCandidates.initDstElements(srcStmts, dstStmts, srcPath, dstPath,AllDstStmtsList, AllDstTokensList,
                AllDstinnerStmtsList,AllDstPathToStmtsMap, AllDstValTokenMap,AllSrcPathToStmtsMap);

        allDstStmtsList = filterDstCandidates.getAllDstStmtsList();
        allDstTokensList = filterDstCandidates.getAllDstTokensList();
        allDstinnerStmtsList = filterDstCandidates.getAllDstinnerStmtsList();

        allDstPathToStmtsMap = filterDstCandidates.getAllDstPathToStmtsMap();

        allDstValTokenMap = filterDstCandidates.getAllDstValTokenMap();
        allSrcPathToStmtsMap = filterDstCandidates.getAllSrcPathToStmtsMap();
    }

    /**
     * Build element mappings and tree mappings.
     */
    public void buildMappingsOuterLoop(List<ProgramElement> srcStmts,String srcPath,Map<String, String> pathMap,
                                       Map<String, Set<ProgramElement>> AllSrcPathToStmtsMap, boolean isSingleFile,
                                       boolean doCrossFileMapping) throws IOException {

        long time1 = System.currentTimeMillis();
        String dstPath = pathMap.get(srcPath);
        // Build candidate searcher and best mapping searcher
        if (!doCrossFileMapping) {
            List<ProgramElement> dstStmts = dstPathToStmtsMap.get(dstPath);
            this.candidateSetsAndMaps = new CandidateSetsAndMaps(eleMappings, srcStmts, dstStmts, doCrossFileMapping);
        } else {
            this.candidateSetsAndMaps = new CandidateSetsAndMaps(eleMappings, srcStmts, allDstStmts,
                    allDstStmtsList, allDstTokensList, allDstinnerStmtsList, allDstValTokenMap, AllSrcPathToStmtsMap, doCrossFileMapping);
            FilterDstCandidates filterDstCandidates = new FilterDstCandidates(eleMappings, allDstStmtsList, allDstTokensList, allDstinnerStmtsList,
                    allDstPathToStmtsMap, allDstValTokenMap,allSrcPathToStmtsMap);
        }

        CandidateSearcher candidateSearcher = new CandidateSearcher(filterDstCandidates,candidateSetsAndMaps, eleMappings);
        BestMappingSearcher bestMappingSearcher = new BestMappingSearcher(candidateSearcher);
        // iterative statement matcher
        BaseMatcher matcher1 = new StmtMatcher(eleMappings, bestMappingSearcher);
        matcher1.setProcessToken(false);
        matcher1.calSrcElementsToMap();

        // iterative token matcher
        BaseMatcher matcher2 = new TokenMatcher(eleMappings, bestMappingSearcher);
        matcher2.setProcessToken(true);
        matcher2.calSrcElementsToMap();

        // iterative inner-stmt element matcher
        BaseMatcher matcher3 = new InnerStmtEleMatcher(eleMappings, bestMappingSearcher);
        matcher3.setProcessToken(false);
        matcher3.calSrcElementsToMap();

        matcher1.setMatcher_type("STMT");
        matcher2.setMatcher_type("TOKEN");
        matcher3.setMatcher_type("INNER");

        // create an inner loop, and if new mappings are generated, establish an outer loop.
        while (true) {
            boolean findMappings1 = matcher1.buildMappingsInnerLoop();
            boolean findMappings2 = matcher2.buildMappingsInnerLoop();
            boolean findMappings3 = matcher3.buildMappingsInnerLoop();
            if (!findMappings2)
                break;
        }

        // build cross-file and one-to-many mapping
        if (!isSingleFile && doCrossFileMapping){
            SelectCrossFileMapping selectCrossFileMapping = new SelectCrossFileMapping(eleMappings, srcPath, pathMap,
                    allDstPathToStmtsMap, allSrcPathToStmtsMap);
            crossFileMap = selectCrossFileMapping.getCrossFileMap();
            oneToMultiMap = selectCrossFileMapping.getOneToMultiMap();
        }

        // map all inner-stmt elements
        ProgramElement srcRoot = srcPathToRoot.get(srcPath);
        ProgramElement dstRoot = dstPathToRoot.get(dstPath);
        if (dstRoot != null)
            ms = MappingTransformer.elementMappingsToTreeMappings(eleMappings, srcRoot, dstRoot);
        long time2 = System.currentTimeMillis();
        mappingTime = time2 - time1;

        time1 = System.currentTimeMillis();
        List<Action> actionList = GumTreeUtil.getEditActions(ms);
        time2 = System.currentTimeMillis();
        actionGenerationTime = time2 - time1;
        if (actionList != null) {
            actions = new ArrayList<>();
            for (Action a : actionList) {
                TreeEditAction ea = new TreeEditAction(a, ms, srcRc, dstRc);
                if (ea.isJavadocRelated())
                    continue;
                actions.add(ea);
            }
        }
    }

    public Map<Map<String, String>, String> getCrossFileMap(){ return crossFileMap;}

    public Map<String, List<String>> getOneToMultiMap() { return oneToMultiMap;}

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
    public List<StmtTokenAction> generateStmtTokenEditActions(String srcPath){
        String dstFilePath = pathMap.get(srcPath);
        List<ProgramElement> dstStmts = dstPathToStmtsMap.get(dstFilePath);
        StmtTokenActionGenerator generator = new StmtTokenActionGenerator(srcStmts, dstStmts, eleMappings);
        List<StmtTokenAction> actionList = generator.generateActions(false, srcPath, dstPathToStmtsMap);
        return generator.reorderActions(actionList);
    }
}
