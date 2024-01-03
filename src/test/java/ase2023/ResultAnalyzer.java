package ase2023;

import java.io.*;

public class ResultAnalyzer {

    public static void analyze(String alg_resPath) {

        int ASTNodeMappings_num_total = 0;
        int eleMappings_num_total = 0;
        int ASTESSize_total = 0, CodeESSize_total = 0;
        int time_total = 0, frN_total = 0;

        File fo = new File(alg_resPath);

        for(File f : fo.listFiles()) {
            if (f.isFile()) {
                String fn = f.getName();
                if (!fn.endsWith(".txt"))
                    continue;

                boolean hasZeroTime = false;
                int project_time_total = 0, frN = 0;
                String project = fn.replace(".txt", "");

                try {
                    String line = "";
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    while((line = br.readLine())!=null) {
                        String[] sa = line.split(" ");
                        if (sa.length == 7) {
                            String commitId = sa[0];
                            String filePath = sa[1];
                            int ASTNodeMappings_num = Integer.parseInt(sa[2]);
                            int eleMappings_num = Integer.parseInt(sa[3]);
                            int ASTESSize = Integer.parseInt(sa[4]);
                            int CodeESSize = Integer.parseInt(sa[5]);
                            int time = Integer.parseInt(sa[6]);
                            if (!hasZeroTime && time == 0)
                                hasZeroTime = true;

                            ASTNodeMappings_num_total += ASTNodeMappings_num;
                            eleMappings_num_total += eleMappings_num;
                            ASTESSize_total += ASTESSize;
                            CodeESSize_total += CodeESSize;
                            project_time_total += time;
                            frN++;
                        }
                    }
                    br.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Double project_avg_time = project_time_total / (frN+0.0);
                System.out.println(project + " -> Avg time: " + project_avg_time);
                time_total += project_time_total;
                frN_total += frN;
                if (hasZeroTime) {
                    System.out.println(project + " has records with zero time!");
                }
            }
        }

        System.out.println("#AST Node Mappings: " + ASTNodeMappings_num_total);
        System.out.println("#Element Mappings: " + eleMappings_num_total);
        System.out.println("AST ES Size: " + ASTESSize_total);
        System.out.println("Code ES Size: " + CodeESSize_total);

        Double avg_time = time_total / (frN_total + 0.0);
        System.out.println("Avg time: " + avg_time);

    }


    public static void main(String[] args) {

        String alg_resPath = "C:\\Users\\29366\\Desktop\\iASTMapper\\ase2023\\iASTMapper_res";

        analyze(alg_resPath);

    }

}
