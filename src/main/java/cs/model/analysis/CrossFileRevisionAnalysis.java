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
    public Map<String, String> srcFilePathMap;
    public Map<String, String> dstFilePathMap;
    protected String srcFileContent;
    protected String dstFileContent;
    public Set<ITree> srcMethodDeclarations;

    public Set<ITree> dstMethodDeclarations;

    public static String used_ASTType = "gt";

    public Integer methodDeclarationNum;

    public List<String> crossList = new ArrayList<>();

    public Integer crossTransferMethodDeclarationNum;

//    public static double comparisonSimilarity;

//    public double mp_similarity;

//    public double cross_similarity;

    public CrossFileRevisionAnalysis(String project, String commitId) throws Exception {
        this.project = project;
        this.commitId = commitId;
//        System.out.println("===================================");
//        System.out.println("Commit: " + commitId);
//        System.out.println("===================================");
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);
        this.baseCommitId = baseCommitId;
        // old path 等价于
        try {
            this.srcFilePathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);
        }
        catch (Exception e){
            srcFilePathMap = null;
            return;
        }
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
//            System.out.println(srcPath);
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
            // format printer step2:
            System.out.println("=================");
            System.out.println("File: " + srcPath);
            System.out.println("=================");
            int t = 0;
            for (ITree tmpSrcMethodDeclaration : tmpSrcMethodDeclarations) {
                System.out.println("Method " + ++t + ": ");
                System.out.println(TreePrinter(tmpSrcMethodDeclaration));
            }

            // update counter num;
            this.methodDeclarationNum += tmpSrcMethodDeclarations.size();
            this.dstMethodDeclarations = new HashSet<ITree>();
            Map<ITree, String> reverseProjectMap = new HashMap<>();
            for (String dstPath : dstFilePathMapFiltered.keySet()) {
//                System.out.println(dstPath);
//                if (dstPath.equals("activemq-core/src/main/java/org/apache/activemq/state/CommandVisitorAdapter.java")){
//                    System.out.println("666");
//                }
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
                for (ITree declaration : tmpDstMethodDeclarations) {
                    reverseProjectMap.put(declaration, dstPath);
                }
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
                                if(e_sim <= mp_similarity) {
                                    mpSimMap.put(mp_tmpSrcMethodDeclaration, mp_similarity);
                                }
//                              System.out.println(mp_tmpDstMethodDeclaration);
                                dstMethodsToRemove.add(mp_tmpDstMethodDeclaration);
                            }
//                            if (srcPath.equals("activemq-core/src/main/java/org/apache/activemq/broker/region/Topic.java")) {
//                            String sameFileMethodspair = "The same file: " + srcPath + "\n" + "[" + TreePrinter(mp_tmpSrcMethodDeclaration) + "] ----> "
//                                    + "\n" + reverseProjectMap.get(mp_tmpDstMethodDeclaration) + "-[" + TreePrinter(mp_tmpDstMethodDeclaration) + "]"
//                                    + "\n" +"mp_similarity: " +mpSimMap.get(mp_tmpSrcMethodDeclaration);
//                            System.out.println(sameFileMethodspair);
//                            }
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
//                    if (srcPath.equals("activemq-core/src/main/java/org/apache/activemq/state/ConnectionStateTracker.java")){
//                        int a  =3;
//                        if (reverseProjectMap.get(dstMethodDeclaration).equals("activemq-core/src/main/java/org/apache/activemq/ActiveMQConnection.java")){
//                            a = 2;
//                            for (ITree child : dstMethodDeclaration.getChildren()) {
////                                System.out.println(child.getType().name);
//                                if (child.getType().name.equals("SimpleName")){
//                                    System.out.println("666");
//                                    System.out.println(child.getLabel());
//                                    if (child.getLabel().equals("processBrokerInfo")){
//                                        a = 4;
//                                    }
//                                }
//                            }
//                        }
//                    }
                    if (cross_similarity >= 0.8){
//                        if (reverseProjectMap.get(dstMethodDeclaration).equals("activemq-core/src/main/java/org/apache/activemq/ActiveMQConnection.java")){
//                            int a = 2;
//                        }
                        double mp_similarity = mpSimMap.getOrDefault(tmpSrcMethodDeclaration, 0.0);
//                        System.out.println(mp_similarity);
                        if(mp_similarity < 1.0){
//                            if (reverseProjectMap.get(dstMethodDeclaration).equals("activemq-core/src/main/java/org/apache/activemq/ActiveMQConnection.java")){
//                                int a = 2;
//                            }
                            if(mp_similarity < cross_similarity) {
//                               System.out.println("9");
                                boolean f = checkForReverse(dstMethodDeclaration, reverseProjectMap.get(dstMethodDeclaration));
                                if (!f){
                                    continue;
                                }
                                String r = "In file: " + srcPath + "\n" + "[" + TreePrinter(tmpSrcMethodDeclaration) + "] ----> "
                                        + "\n" + reverseProjectMap.get(dstMethodDeclaration) + "-[" + TreePrinter(dstMethodDeclaration) + "]"
                                        + "\n" +"Similarity: " +cross_similarity + "\n\n";
                                System.out.print(r);
                                crossList.add(r);
                                flag = 1;
//                                break;
                            }
                        }
                    }
                }
                if (flag == 1){
//                    System.out.println(reverseProjectMap.get(dstMethodDeclarations));
                    this.crossTransferMethodDeclarationNum += 1;
//                        System.out.println("wofengla");
                }
            }
            // clear tmp declaration set
            tmpSrcMethodDeclarations.clear();
            mpSimMap.clear();
            reverseProjectMap.clear();
        }
    }

    private boolean checkForReverse(ITree declaration, String file) throws Exception {
//        if (file.equals("activemq-core/src/main/java/org/apache/activemq/state/CommandVisitorAdapter.java")){
//            System.out.println('t');
//        }
//        System.out.println("tttest");
//        System.out.println(file);
        ByteArrayOutputStream srcFileStream = null;
        try {
            srcFileStream = GitUtils
                    .getFileContentOfCommitFile(project, this.baseCommitId, file);
        } catch (Exception e){
            return true;
        }
        String srcFileContent = srcFileStream.toString("UTF-8");
        if (srcFileContent.equals("")){
            srcFileContent = null;
            return true;
        }
        // generate src_tree
        ITree tmp_src = GumTreeUtil.getITreeRoot(srcFileContent, this.used_ASTType);
        // get all method declarations
        Set<ITree> declarations = DeclarationUtil.getMethodDeclarations(tmp_src);
        StringBuilder uncheckedContent = tree2String(declaration);
        for (ITree node : declarations) {
            StringBuilder src_content = tree2String(node);
            if (src_content.toString().equals(uncheckedContent.toString())) {
                return false;
            }
        }
        return true;
    }

    public static String TreePrinter(ITree node){
        StringBuilder builder = tree2String(node);
        String res = builder.toString();
        return res;
    }

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
//        CosineSimilarity cosineSimilarity = new CosineSimilarity();
//        comparisonSimilarity = cosineSimilarity.cosineSimilarity(srcMaptokens, dstMaptokens);
//        comparisonSimilarity = mitigateRound(comparisonSimilarity, 4, BigDecimal.ROUND_HALF_UP);
        return comparisonSimilarity;
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
         * fb3b6dba571b9dbffaac45ac920037760ceb6dbc(342)
         * 8d11f07a96fe4e2a0a338e68c9785438813d53b6
         * project name: activemq
         */
        System.out.println("===================================");
        String commitID = "8d11f07a96fe4e2a0a338e68c9785438813d53b6";
        System.out.println("Commit: " + commitID);
        System.out.println("===================================");
        CrossFileRevisionAnalysis instance = new CrossFileRevisionAnalysis("activemq", commitID);
        System.out.println(instance.methodDeclarationNum);
        System.out.println(instance.crossTransferMethodDeclarationNum);
        System.out.println("The rate of cross transfer method is: ");
        System.out.println((100 * instance.crossTransferMethodDeclarationNum) / instance.methodDeclarationNum + "%");

        System.out.println("=============================");
        for (String s : instance.crossList) {
            System.out.println(s);
        }
        System.out.println("=============================");

//        CommitAnalysis commitAnalysis = new CommitAnalysis("activemq", "fb3b6dba571b9dbffaac45ac920037760ceb6dbc");
//        commitAnalysis.calResultMappings(false, false);
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
//        while (commitSet.size() > 2000){
//            commitSet.remove(commitSet.iterator().next());
//        }
//        Integer totalCrossTransferMethodDeclarationNum = 0;
//        Integer totalMethodDeclarationNum = 0;
//        for (String commitId : commitSet){
//            if(!commitId.equals("58aca869816e893e7a2f34f0708c1d7fcbdca0f5") && !commitId.equals("a9223e42ebc04d420b2e62e9e57450408ee9f513")){
//                CrossFileRevisionAnalysis instanceTmp = new CrossFileRevisionAnalysis("activemq", commitId);
//                if (instanceTmp.srcFilePathMap == null) {
//                    continue;
//                }
//                totalCrossTransferMethodDeclarationNum += instanceTmp.crossTransferMethodDeclarationNum;
//                totalMethodDeclarationNum += instanceTmp.methodDeclarationNum;
//
//                System.out.println("=============================");
//
//                System.out.println((instanceTmp.methodDeclarationNum != 0 ? ((100 * instanceTmp.crossTransferMethodDeclarationNum) / instanceTmp.methodDeclarationNum) : "0") + "%");
//                System.out.println("=============================");
//                for (String s : instanceTmp.crossList) {
//                    System.out.println(s);
//                }
//                System.out.println(commitId + " finished");
//            }
//        }
//        System.out.println("=============================");
//        System.out.println("The total rate of cross transfer method is: ");
//        System.out.println((100*totalCrossTransferMethodDeclarationNum) / totalMethodDeclarationNum +"%");
//        System.out.println("=============================");
    }
}
