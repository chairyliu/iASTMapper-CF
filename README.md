# Guide for running the code

# iASTMapper-CF:

An Iterative Similarity-Based Abstract Syntax Tree Mapping Algorithm

# How to run our code
1. Clone the ten studied projects (ActiveMQ and so on) to the directory `D://tmp`. You can change the path in `resources/config.properties`.
2. Run `/test/java/unit/baseline/BaselineTest`. This is the test for the baseline and the iASTMapper algorithms.
```
// To test the baseline. 
// Optional methods (gt,mtd,ijm)
TestUtils.testBaseline(project, commitId, file, "gt");

// To test iASTMapper
TestUtils.testiASTMapper(project, commitId);
```

3. Run `test.java.resultAnalyzer.iASTMapper_runner`. The `iASTMapper-CF` integrates all processes, including node extraction, mapping, output, and more.
```
// mappingResult using iASTMapper for all the file revisions of a commit.
CommitAnalysis mappingResult = new CommitAnalysis(project, commitId);

// mappingResult for each file revision using iASTMapper
mappingResult.calResultMappings(false, false);

// RevisionAnalysis provides APIs to retrieve the mappings of elements, 
// mappings of tree nodes, edit actions and so on
Map<String, RevisionAnalysis> resultMap = analysis.getRevisionAnalysisResultMap();

// Get AST edit actions and Code edit actions 
for (String filePath: resultMap) {
    List<TreeEditAction>  actions    = generateEditActions();
    List<StmtTokenAction> actionList = generateActions();
    ...
}
```
