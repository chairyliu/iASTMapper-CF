package cs.model.algorithm.utils;

import com.github.gumtreediff.tree.ITree;
import cs.model.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Line number calculator for a program element.
 */
public class RangeCalculator {

    private List<Integer> lineEndIndexes;
    private String[] lines;
    private String fileContent;

    public RangeCalculator(String fileContent){//可能细节有问题，会超出索引
        initLineEndIndexes(fileContent);
        this.fileContent = fileContent;
        buildLines();
    }

    private void buildLines(){//查找每一行的内容，保存在lines中
        List<String> tmpLines = new ArrayList<>();//暂时存储每一行的内容
        String currentLine = "";//当前正在处理的行的内容
        for (int i = 0; i < fileContent.length(); i++){
            char c = fileContent.charAt(i);//获取文件内容中当前位置的字符
            if (c == '\n'){
                tmpLines.add(currentLine);
                currentLine = "";//重置
            } else {
                currentLine += c;
            }
        }
        if (fileContent.endsWith("\n"))
            tmpLines.add("");
        else if (!currentLine.equals(""))
            tmpLines.add(currentLine);
        lines = tmpLines.toArray(new String[tmpLines.size()]);//将tmpLines转换为字符串数组，lines包含了文件内容中每一行的字符串
    }

    public String[] getLines() {
        return lines;
    }

    //根据换行符确定每行结束的索引，以及每行有多少个字符
    private void initLineEndIndexes(String fileContent){
        lineEndIndexes = new ArrayList<>();//存储每行结束的索引
        int the_end = 0;//存储每行有多少个字符
        for (int i = 0; i < fileContent.length(); i++) {
            if (fileContent.charAt(i) == '\n')//如果当前字符是换行符，就把i添加到lineEndIndexes列表中，表示这是一行的结束位置
                lineEndIndexes.add(i);
            the_end++;
        }
        // repair the bug add the last line index (because the last line hasn't the end character) 20220510 21:57
        lineEndIndexes.add(the_end);//因为最后一行可能没有换行符，所以在遍历完成后，将the_end的最终值添加到lineEndIndexes
    }

    /**
     * Get the line number given a character position
     * @param pos the character position
     * @return the line number of pos
     */
    public int getLineNumberOfPos(int pos){//获取给定字符的行号
//        System.out.println(pos + " || " + lineEndIndexes);
        int ret = findEndIndexesBetweenNode(pos, lineEndIndexes);
        if (ret == -1)
            throw new RuntimeException("Cannot find the line number of pos!");
        return ret;
    }

    /**
     * Get start and end line of an ITree node
     * @param node an ITree node
     */
    public Pair<Integer, Integer> getLineRangeOfNode(ITree node){
        if (node == null)
            return null;
        int startPos = node.getPos();
        int endPos = node.getEndPos();
        int startLine = getLineNumberOfPos(startPos);
        int endLine = getLineNumberOfPos(endPos);
        return new Pair<>(startLine, endLine);
    }

    private static int findEndIndexesBetweenNode(int pos, List<Integer> endIndexes) {
        if (endIndexes.size() == 0)
            return -1;
        if (endIndexes.size() == 1) {
            if (pos > endIndexes.get(0))
                return -1;
            else
                return 1;
        }
        int medIndex = endIndexes.size() / 2;
        List<Integer> left = endIndexes.subList(0, medIndex);
        List<Integer> right = endIndexes.subList(medIndex, endIndexes.size());
        if (pos <= endIndexes.get(medIndex - 1))
            return findEndIndexesBetweenNode(pos, left);
        else
            return medIndex + findEndIndexesBetweenNode(pos, right);
    }

    public String getLineContent(int line){
        if (line <= 0 || line > lines.length)
            return null;
        return lines[line - 1];
    }

    public String getFileContent(){
        return fileContent;
    }

    public static String[] getAllLines(String content){
        return new RangeCalculator(content).getLines();
    }

}
