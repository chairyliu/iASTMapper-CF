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


public class CrossFileRevisionAnalysis {
    protected String srcFileContent;
    protected String dstFileContent;
    public static String used_ASTType = "gt";
    public Integer methodDeclarationNum;
    public List<String> crossList = new ArrayList<>();
    public Integer crossTransferMethodDeclarationNum;
    public Map<String, List<String>> srcPathAndMethodsMap;
    public Map<String, List<String>> dstPathAndMethodsMap;
    public List<String> allDstMethods;

    public CrossFileRevisionAnalysis(String project, String commitId) throws Exception {
//        System.out.println("===================================");
//        System.out.println("Commit: " + commitId);
//        System.out.println("===================================");
        String baseCommitId = GitUtils.getBaseCommitId(project, commitId);
        Map<String, String> srcFilePathMap = null;
        try {
            srcFilePathMap = GitInfoRetrieval.getOldModifiedFileMap(project, commitId);
        } catch (Exception e) {
            srcFilePathMap = null;
            return;
        }

        //所有srcpath下的方法总数，也就是分母
        this.methodDeclarationNum = 0;
        //存在跨文件映射的方法，也就是分子
        this.crossTransferMethodDeclarationNum = 0;

        //srcPathAndAllMethodsMap中第一个String代表srcpath，第二个List存放每一个srcpath下的所有改动过的方法声明，把所有方法汇总起来存到map里，dst同理
        this.srcPathAndMethodsMap = new HashMap<>();
        this.dstPathAndMethodsMap = new HashMap<>();
        List<String> srcMethodsList;
        List<String> dstMethodsList;

        //因为在跨文件映射的时候，需要srcpath下的方法和其非对应dstpath下的所有方法比较，如果直接用dstPathAndMethodsMap，
        //除了遍历路径外，还需要遍历每个list，会进行多层循环。所以用allDstMethods存储所有dstpath下的方法，减少了循环次数
        allDstMethods = new ArrayList<>();

        //第一个String表示srcpath，第二个map集合绑定了每个方法和其相似度值，方便比较同一个方法同文件映射和跨文件映射的相似度大小
        Map<String, Map<String,Double>> mpSimMap = new HashMap<>();
        //第一个参数是dst方法，第二个参数是其对应的dstpath。因为后续输出时，需要拿到每一个dst方法对应的路径
        Map<String, String> reverseProjectMap = new HashMap<>();

        //这一块总体是遍历每个srcpath和dstpath，拿到其AST树和每个路径下的所有方法。再把ITree节点转换成字符串，存入对应的map集合中
        for (Map.Entry<String, String> entry : srcFilePathMap.entrySet()) {
            String srcPath = entry.getKey();
            String dstPath = entry.getValue();

            // get Content for both src and dst
            ByteArrayOutputStream srcFileStream = GitUtils
                    .getFileContentOfCommitFile(project, baseCommitId, srcPath);
            srcFileContent = srcFileStream.toString("UTF-8");
            if (srcFileContent.equals("")) {
                srcFileContent = null;
                return;
            }
            ByteArrayOutputStream dstFileStream = GitUtils
                    .getFileContentOfCommitFile(project, commitId, dstPath);
            dstFileContent = dstFileStream.toString("UTF-8");
            if (dstFileContent.equals("")) {
                dstFileContent = null;
                return;
            }

            // generate src_tree, and dst_tree
            ITree tmp_src = null;
            ITree tmp_dst = null;
            try {
                tmp_src = GumTreeUtil.getITreeRoot(srcFileContent, used_ASTType);
                tmp_dst = GumTreeUtil.getITreeRoot(dstFileContent, used_ASTType);
            } catch (Exception e) {
                break;
            }
            // fresh the method list for each srcPath
            srcMethodsList = new ArrayList<>();
            dstMethodsList = new ArrayList<>();
            // get all method declarations
            Set<ITree> srcMethodDeclarations = DeclarationUtil.getMethodDeclarations(tmp_src);
            for (ITree srcMethodDeclaration : srcMethodDeclarations) {
                String srcMethod = TreePrinter(srcMethodDeclaration);
                srcMethodsList.add(srcMethod);
            }
            Set<ITree> dstMethodDeclarations = DeclarationUtil.getMethodDeclarations(tmp_dst);
            for (ITree dstMethodDeclaration : dstMethodDeclarations) {
                String dstMethod = TreePrinter(dstMethodDeclaration);
                dstMethodsList.add(dstMethod);
            }

            // format printer step2:
            System.out.println("=================");
            System.out.println("File: " + srcPath);
            System.out.println("=================");
//            int t = 0;
            System.out.println("The total number of methods is " + srcMethodsList.size() + ": ");
            for (ITree srcMethodDeclaration : srcMethodDeclarations) {
                System.out.println(TreePrinter(srcMethodDeclaration));
            }

            // 每拿完一个srcpath下的所有方法，都会更新methodDeclarationNum的值
            this.methodDeclarationNum += srcMethodDeclarations.size();

            //过滤掉没有修改的方法。commonMethods表示src和dst里相同的方法，如果两个方法完全相同，那么就说明没有更改过，不考虑未修改的方法
            List<String> commonMethods = new ArrayList<>(srcMethodsList);
            commonMethods.retainAll(dstMethodsList);
            //filterSrcMethodList表示已经过滤后的方法集合，只包含修改过的语句
            List<String> filterSrcMethodList = new ArrayList<>(srcMethodsList);
            List<String> filterDstMethodList = new ArrayList<>(dstMethodsList);
            filterSrcMethodList.removeAll(commonMethods);
            filterDstMethodList.removeAll(commonMethods);
//            System.out.println("6");

            srcPathAndMethodsMap.put(srcPath, filterSrcMethodList);
            dstPathAndMethodsMap.put(dstPath, filterDstMethodList);

            //对于每个dst中的方法，都将它和其dstpath绑定起来，方便后面输出
            for (String filterDstMethod : filterDstMethodList) {
                reverseProjectMap.put(filterDstMethod, dstPath);
            }
            //每一次方法过滤后，都同步更新allDstMethods列表，所有dstpath都遍历一遍后，该参数内存着所有修改过的dst方法
            allDstMethods.addAll(filterDstMethodList);

            //mpSimMap的第二个参数，将每个方法和其相似度值绑定
            HashMap<String, Double> methodToSimMap = new HashMap<>();

            for (String sameFile_srcMethodDeclaration : filterSrcMethodList) {
                for (String sameFile_dstMethodDeclaration : filterDstMethodList) {
                    //src和dst路径相同时，获取每个方法之间的相似度
                    double sameFile_similarity = compareTwo(sameFile_srcMethodDeclaration, sameFile_dstMethodDeclaration);
                    //将src的方法和相似度绑定，方便后面和跨文件的相似度进行对比
                    methodToSimMap.put(sameFile_srcMethodDeclaration, sameFile_similarity);
                    //定义max_similarity用来更迭src方法最大的相似度
                    Double max_similarity = methodToSimMap.getOrDefault(sameFile_srcMethodDeclaration, 0.0);
                    if (max_similarity <= sameFile_similarity) {
                        methodToSimMap.put(sameFile_srcMethodDeclaration, sameFile_similarity);
//                        System.out.println("123");
                    }
                }
            }
            mpSimMap.put(srcPath, methodToSimMap);
        }

        for (Map.Entry<String, List<String>> entry : srcPathAndMethodsMap.entrySet()) {
            String srcPath = entry.getKey();
            List<String> srcMethodList = entry.getValue();
            //获取当前dstpath下所有修改过的方法
            List<String> curDstMethods = dstPathAndMethodsMap.get(srcPath);
            //初始化一个crossDstMethods，现在里面存了所有dstpath下的所有修改方法
            List<String> crossDstMethods = new ArrayList<>(allDstMethods);
            //从crossDstMethods中移除掉当前dstpath下的修改方法，剩下的方法列表就是用于跨文件映射的方法
            crossDstMethods.removeAll(curDstMethods);
            for (String srcMethod : srcMethodList) {
                int flag = 0;
                for (String dstMethod : crossDstMethods) {
                    double cross_similarity = compareTwo(srcMethod, dstMethod);
                    String dstPath = reverseProjectMap.get(dstMethod);
                    double sameFile_similarity = 0.0;
                    if (cross_similarity >= 0.8) {
                        Map<String, Double> methodToSimMap = mpSimMap.getOrDefault(srcPath, null);
                        if (methodToSimMap == null) {
                            sameFile_similarity = 0.0;
                        }
                        else {
                            sameFile_similarity = methodToSimMap.getOrDefault(srcMethod, 0.0);
                        }
                        if (sameFile_similarity < 1.0) {
                            if (sameFile_similarity < cross_similarity) {
                                String r = "In file: " + srcPath + "\n" + "[" + srcMethod + "] ----> "
                                                + "\n" + reverseProjectMap.get(dstMethod) + "-[" + dstMethod + "]"
                                                + "\n" + "Similarity: " + cross_similarity + "\n\n";
                                System.out.print(r);
                                //存储了所有跨文件映射的方法，用于后续集体输出
                                crossList.add(r);
                                flag = 1;
//                                  break;
                            }
                        }
                    }
                }
                if (flag == 1)
                    this.crossTransferMethodDeclarationNum += 1;
            }
        }
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
    public static double compareTwo(String src, String dst) {
        String[] srcTokens = src.split("\\s+");
        String[] dstTokens = dst.split("\\s+");
        Map<CharSequence, Integer> srcMaptokens = convertToMap(srcTokens);
        Map<CharSequence, Integer> dstMaptokens = convertToMap(dstTokens);
        CosSimilarity simEngine = new CosSimilarity();
        double comparisonSimilarity = simEngine.cosineSimilarity(srcMaptokens, dstMaptokens);
        return comparisonSimilarity;
    }

    private static Map<CharSequence, Integer> convertToMap(String[] tokens) {
        Map<CharSequence, Integer> map = new HashMap<>();
        for (String token : tokens) {
            map.put(token, map.getOrDefault(token, 0) + 1);
        }
        return map;
    }

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
