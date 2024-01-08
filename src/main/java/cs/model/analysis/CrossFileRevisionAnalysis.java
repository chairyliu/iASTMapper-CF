package cs.model.analysis;

import com.github.gumtreediff.tree.ITree;
import cs.model.algorithm.utils.DeclarationUtil;
import cs.model.algorithm.utils.GumTreeUtil;
import cs.model.gitops.GitInfoRetrieval;
import cs.model.gitops.GitUtils;
import cs.model.utils.CosSimilarity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.util.*;

//import static com.sun.tools.javac.util.Constants.formatDouble;


public class CrossFileRevisionAnalysis {
    protected String project;
    protected String commitId;
    protected String baseCommitId;
    protected Map<String, String> srcFilePathMap;
    protected Map<String, String> dstFilePathMap;
    protected String srcFileContent;
    protected String dstFileContent;
    public Set<ITree> srcMethodDeclarations;

    public Set<ITree> dstMethodDeclarations;

    public static String used_ASTType = "gt";

    public Integer methodDeclarationNum;

    public Integer crossTransferMethodDeclarationNum;

//    public static double comparisonSimilarity;

//    public double mp_similarity;

//    public double cross_similarity;

    public CrossFileRevisionAnalysis(String project, String commitId) throws Exception {
        this.project = project;
        this.commitId = commitId;
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);
        this.baseCommitId = baseCommitId;
        // old path 等价于
        this.srcFilePathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);
        // new path 等价于
        this.dstFilePathMap = new HashMap<String, String>();
        for (String key : srcFilePathMap.keySet()) {
            this.dstFilePathMap.put(srcFilePathMap.get(key), key);
        }

        // initialization
        this.srcMethodDeclarations = new HashSet<ITree>();
        this.dstMethodDeclarations = new HashSet<ITree>();

        // count
        this.methodDeclarationNum = 0;
        this.crossTransferMethodDeclarationNum = 0;

//        System.out.println("testpoint");
        for (String srcPath : srcFilePathMap.keySet()) {
            Map<ITree, Double> mpSimMap = new HashMap<>();
            // 对于每一个src，过滤出一个不含其本身的dst map
            Map<String, String> dstFilePathMapFiltered = new HashMap<String, String>();
            dstFilePathMapFiltered.putAll(dstFilePathMap);
//            dstFilePathMapFiltered.remove(srcPath);
//            System.out.println("checkPoint");
            // 获取其srcFileContent
            ByteArrayOutputStream srcFileStream = GitUtils
                    .getFileContentOfCommitFile(project, this.baseCommitId, srcPath);
            srcFileContent = srcFileStream.toString("UTF-8");
            if (srcFileContent.equals("")){
                srcFileContent = null;
                return;
            }
            // generate src_tree
            ITree tmp_src = GumTreeUtil.getITreeRoot(srcFileContent, this.used_ASTType);
            // get all method declarations
            Set<ITree> tmpSrcMethodDeclarations = DeclarationUtil.getMethodDeclarations(tmp_src);
            // save the whole method declarations
            this.srcMethodDeclarations.addAll(tmpSrcMethodDeclarations);
            // update counter num;
            this.methodDeclarationNum += tmpSrcMethodDeclarations.size();
            this.dstMethodDeclarations = new HashSet<ITree>();
            for (String dstPath : dstFilePathMapFiltered.keySet()) {
                ByteArrayOutputStream dstFileStream = GitUtils
                        .getFileContentOfCommitFile(project, commitId, dstPath);
                dstFileContent = dstFileStream.toString("UTF-8");
                if (dstFileContent.equals("")) {
                    dstFileContent = null;
                    return;
                }
                ITree tmp_dst = null;
                try {
                    tmp_dst = GumTreeUtil.getITreeRoot(dstFileContent, this.used_ASTType);
                } catch (Exception e){
                    break;
                }
                Set<ITree> tmpDstMethodDeclarations = DeclarationUtil.getMethodDeclarations(tmp_dst);
                this.dstMethodDeclarations.addAll(tmpDstMethodDeclarations);
//                System.out.println(this.dstMethodDeclarations);
                if (srcPath.equals(dstPath)){
                    List<ITree> dstMethodsToRemove = new ArrayList<>();
//                    List<ITree> srcMethodsToRemove = new ArrayList<>();
                    for (ITree mp_tmpSrcMethodDeclaration : tmpSrcMethodDeclarations){
                        for (ITree mp_tmpDstMethodDeclaration : tmpDstMethodDeclarations){
                            StringBuilder mp_srcContent = tree2String(mp_tmpSrcMethodDeclaration);
                            StringBuilder mp_dstContent = tree2String(mp_tmpDstMethodDeclaration);
                            if (mp_srcContent.toString().equals(mp_dstContent.toString())){
                                dstMethodsToRemove.add(mp_tmpDstMethodDeclaration);
                                double mp_similarity = 1.0;
//                                Map<ITree, Double> tmpMap = new HashMap<>();
//                                tmpMap.put(mp_tmpDstMethodDeclaration, mp_similarity);
                                mpSimMap.put(mp_tmpSrcMethodDeclaration, mp_similarity);
//                                srcMethodsToRemove.add(mp_tmpSrcMethodDeclaration);
//                                System.out.println("nishenmeqingkuang");
                            }
                            else {
                                double mp_similarity = compareTwo(mp_tmpSrcMethodDeclaration, mp_tmpDstMethodDeclaration);
//                                Map<ITree, Double> tmpMap = new HashMap<>();
//                                tmpMap.put(mp_tmpDstMethodDeclaration, mp_similarity);
                                Double e_sim = mpSimMap.getOrDefault(mp_tmpSrcMethodDeclaration, 0.0);
                                if(e_sim < mp_similarity) {
                                    mpSimMap.put(mp_tmpSrcMethodDeclaration, mp_similarity);
                                }
//                              System.out.println(mp_tmpDstMethodDeclaration);
                                dstMethodsToRemove.add(mp_tmpDstMethodDeclaration);
                            }
                        }
                    }
                    this.dstMethodDeclarations.removeAll(dstMethodsToRemove);
//                    tmpSrcMethodDeclarations.removeAll(srcMethodsToRemove);
//                    System.out.println(this.dstMethodDeclarations);
                }
            }
//            System.out.println("6");
            // tmpSrcMethodDeclarations中每一个元素分别看，是不是在dstMethodDeclarations里。
            for (ITree tmpSrcMethodDeclaration : tmpSrcMethodDeclarations) {
                int flag = 0;
                for (ITree dstMethodDeclaration : this.dstMethodDeclarations) {
                    double cross_similarity = compareTwo(tmpSrcMethodDeclaration, dstMethodDeclaration);
                    if (cross_similarity >= 0.8){
                        double mp_similarity = mpSimMap.get(tmpSrcMethodDeclaration);
//                        System.out.println(mp_similarity);
                        if(mp_similarity < 1.0){
                            if(mp_similarity < cross_similarity) {
//                               System.out.println("9");
                               flag = 1;
                               break;
                            }
                        }
                    }
                }
                if (flag == 1){
                    this.crossTransferMethodDeclarationNum += 1;
//                        System.out.println("wofengla");
                }
            }
            // clear tmp declaration set
            tmpSrcMethodDeclarations.clear();
            mpSimMap.clear();
//            System.out.println("methodDeclarationNum");
//            System.out.println(this.methodDeclarationNum);
//            System.out.println("crossTransferMethodDeclarationNum");
//            System.out.println(this.crossTransferMethodDeclarationNum);
        }
    }

//    public static double getComparisonSimilarity() {
//        return comparisonSimilarity;
//    }

    /**
     * public static boolean compareTwo(ITree src)
     * public static boolean compareTwo(Tree src)
     * @param src
     * @param dst
     * @return
     */
    public static double compareTwo(ITree src, ITree dst) {
        StringBuilder srcContent = tree2String(src);
        StringBuilder dstContent = tree2String(dst);
        String srcString = srcContent.toString();
        String dstString = dstContent.toString();
        String[] srcTokens = srcString.split("\\s+");
        String[] dstTokens = dstString.split("\\s+");
        Map<CharSequence, Integer> srcMaptokens = convertToMap(srcTokens);
        Map<CharSequence, Integer> dstMaptokens = convertToMap(dstTokens);
//        CosineSimilarity simEngine = new CosineSimilarity();
        CosSimilarity simEngine = new CosSimilarity();
        double comparisonSimilarity = simEngine.cosineSimilarity(srcMaptokens, dstMaptokens);
//        System.out.println("========");
//        System.out.println(comparisonSimilarity);
//        System.out.println("========");
//        if (comparisonSimilarity >= 0.8){
//            System.out.println("higher");
//        }
//        CosineSimilarity cosineSimilarity = new CosineSimilarity();
//        comparisonSimilarity = cosineSimilarity.cosineSimilarity(srcMaptokens, dstMaptokens);
//        comparisonSimilarity = mitigateRound(comparisonSimilarity, 4, BigDecimal.ROUND_HALF_UP);
        return comparisonSimilarity;
//        BigDecimal srcBigDecimal = BigDecimal.valueOf(comparisonSimilarity);
//        BigDecimal threshold = new BigDecimal("0.8");
//        return srcBigDecimal.compareTo(threshold) >= 0;
    }
//        int editDistance = calculateTokenEditDistance(tokens1, tokens2);
//        int editDistance = LevenshteinDistance.getDefaultInstance().apply(srcString, dstString);
//        if (editDistance <= 3 )
//            return true;
//        else{
//            return false;
//        }
//        return srcString.equals(dstString);

    private static Map<CharSequence, Integer> convertToMap(String[] tokens) {
        Map<CharSequence, Integer> map = new HashMap<>();
        for (String token : tokens) {
            map.put(token, map.getOrDefault(token, 0) + 1);
        }
        return map;
    }

//    public static double mitigateRound(double value, int scale, int mode){
//
//        BigDecimal bigDecimal = new BigDecimal(s);
//        bigDecimal.setScale(scale, mode);
//        double res = bigDecimal.doubleValue();
//        // clear the memory
//        bigDecimal = null;
//        return res;
//    }

//    private static int calculateTokenEditDistance(String[] tokens1, String[] tokens2) {
//        LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
//        int totalDistance = 0;
//        int minLength = Math.min(tokens1.length, tokens2.length);
//
//        for (int i = 0; i < minLength; i++) {
//            if (!(tokens1[i].equals(tokens2[i]))) {
//                for (int j = 0; j < minLength; j++) {
//                    if(!(tokens1[i].equals(tokens2[j])))
//                        totalDistance ++;
////              totalDistance += levenshteinDistance.apply(tokens1[i], tokens2[j]);
//                }
//            }
//        }
//        totalDistance += Math.abs(tokens1.length - tokens2.length);
//        System.out.println(totalDistance);
//        return totalDistance;
//    }

    public static StringBuilder tree2String(ITree tree){
        StringBuilder content = new StringBuilder();

        for (ITree child : tree.getChildren()) {
            //过滤掉没用的
            if (!((child.getType().name).equals("Block") ||
                    (child.getType().name).equals("MarkerAnnotation") ||
                    (child.getType().name).equals("SingleMemberAnnotation") ||
                    (child.getType().name).equals("NormalAnnotation")||
                    (child.getType().name).equals("Javadoc")
                )
            ){
                if ((child.getType().name).equals("SingleVariableDeclaration")) {
//                    content.append("(");
                    content.append(extractDeclaration(child));
//                    content.append(")");
                }
                else{
                    content.append(extractDeclaration(child));
                }
            }

        }
        return content;
    }

    public static String extractDeclaration(ITree tree){
        StringBuilder content = new StringBuilder();
        if (tree.getChildren().size() == 0){
            content.append(tree.getLabel());
        }
        else{
            // 这个地方根本拿不到数据集合类型，因为iASTMapper这部分没有储存！！！
//            content.append(tree.getType().name);
//            content.append("<");
            List<ITree> children = tree.getChildren();
            for (int i = 0; i < children.size(); i++) {
                content.append(extractDeclaration(children.get(i)));
//                content.append(" ");
            }
//            content.append(">");
        }
        content.append(" ");
        return content.toString();
    }

    public static List<String> getCommitList(String project, String filePath){
        List<String> commitList = new ArrayList<>();
        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while((line = br.readLine())!=null) {
//                System.out.println(666);
                commitList.add(line.split(" ")[0].trim());
            }
            br.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return commitList;

    }


    public static void main(String[] args) throws Exception {
        /**
         * test point for one commit
         * commit id: 0d529af312c9bc1bd0560e67b549f0012cda5937
         * 7077d2b910405dea7a60c5140824966ffebc66a8 Syntax error on source code
         * 58aca869816e893e7a2f34f0708c1d7fcbdca0f5 98% -> 97% -> 43% -> 22%
         * 0c93dfde72494a5906b2937d320bde8ef46794e6 22% -> 0%
         * 8f55d404affc0e4ab556ae1937a1ff8d21cdb368
         * project name: activemq
         */
        CrossFileRevisionAnalysis instance = new CrossFileRevisionAnalysis("activemq", "fb3b6dba571b9dbffaac45ac920037760ceb6dbc");
        System.out.println(instance.methodDeclarationNum);
        System.out.println(instance.crossTransferMethodDeclarationNum);
        System.out.println((100 * instance.crossTransferMethodDeclarationNum) / instance.methodDeclarationNum + "%");

        /**
         * test point for one project
         * project name: activemq
         */
//        String project = "activemq";
//        String filePath = "ase2023" + File.separator + "project_commits-1" + File.separator + project + ".txt";
//        List<String> commitList = getCommitList(project,filePath);
//        // transfer commitList to set
//        Set<String> commitSet = new HashSet<>(commitList);
////        System.out.println(commitSet.size());
////        System.out.println(commitSet);
//        // only remain 100 elements in commitSet
//        while (commitSet.size() > 100){
//            commitSet.remove(commitSet.iterator().next());
//        }
//        Integer totalCrossTransferMethodDeclarationNum = 0;
//        Integer totalMethodDeclarationNum = 0;
//        for (String commitId : commitSet){
//            if(!commitId.equals("58aca869816e893e7a2f34f0708c1d7fcbdca0f5") && !commitId.equals("fb3b6dba571b9dbffaac45ac920037760ceb6dbc")){
//                CrossFileRevisionAnalysis instanceTmp = new CrossFileRevisionAnalysis("activemq", commitId);
//                totalCrossTransferMethodDeclarationNum += instanceTmp.crossTransferMethodDeclarationNum;
//                totalMethodDeclarationNum += instanceTmp.methodDeclarationNum;
//                System.out.println(commitId + " finished");
//            }
//        }
//        System.out.println((100*totalCrossTransferMethodDeclarationNum) / totalMethodDeclarationNum +"%");
    }
}
