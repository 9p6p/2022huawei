package com.huawei.java.main;

import java.io.*;
import java.util.*;

public class Main {
    static int QOS_CONSTRAINT;
    static int M;
    static int N;
    static int T;
    String[] edgeNodes;
    int[] edgeMax;
    String[] clientNodes;
    boolean[][] transfer;
    Map<Integer, LinkedList<Integer>> mapClientToEdge;
    int[][] nodeOrder;
    int five_percent;
    Map<String, Integer> projectionToEdge;

    public void read_ini(String filepath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            Properties pps = new Properties();
            pps.load(br);
            QOS_CONSTRAINT = Integer.parseInt(pps.getProperty("qos_constraint"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read_base(String filepath) {
        try (BufferedReader bt = new BufferedReader(new FileReader(filepath));
             BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            int index = -1;
            while (bt.readLine() != null) {
                index++;
            }
            String line;
            N = index;
            line = br.readLine();
            String[] names = line.split(",");
            M = names.length - 1;
            clientNodes = new String[M];
            System.arraycopy(names, 1, clientNodes, 0, M);
            edgeNodes = new String[N];
            transfer = new boolean[M][N];
            index = 0;
            while ((line = br.readLine()) != null) {
                String[] element = line.split(",");
                edgeNodes[index++] = element[0];
                for (int i = 1; i <= M; i++) {
                    if (Integer.parseInt(element[i]) < QOS_CONSTRAINT) {
                        transfer[i - 1][index - 1] = true;
                    }
                }
            }
            nodeOrder = new int[M][2];
            mapClientToEdge = new HashMap<>();
            for (int i = 0; i < M; i++) {
                LinkedList<Integer> list = new LinkedList<>();
                nodeOrder[i][0] = i;
                int count = 0;
                for (int j = 0; j < N; j++) {
                    if (transfer[i][j]) {
                        count++;
                        list.add(j);
                    }
                }
                nodeOrder[i][1] = count;
                mapClientToEdge.put(i, list);
            }
            Arrays.sort(nodeOrder, Comparator.comparingInt(o -> o[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read_bandwidth(String filepath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            br.readLine();
            edgeMax = new int[N];
            projectionToEdge = new HashMap<>();
            while ((line = br.readLine()) != null) {
                String[] element = line.split(",");
                projectionToEdge.put(element[0], Integer.parseInt(element[1]));
            }
            for (int i = 0; i < N; i++) {
                edgeMax[i] = projectionToEdge.get(edgeNodes[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read_demand(String filepath) {
        String path = "/output/solution.txt";
        try (BufferedReader bt = new BufferedReader(new FileReader(filepath));
             BufferedReader br = new BufferedReader(new FileReader(filepath));
             PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            int index = -1;
            while (bt.readLine() != null) {
                index++;
            }
            T = index;
            five_percent = (int)(T * 0.95) + 1;
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] element = line.split(",");
                solution(element, pw);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void solution(String[] strings, PrintWriter pw) {
        int[] val = new int[M];
        for (int i = 0; i < M; i++) val[i] = Integer.parseInt(strings[i + 1]);
        int[][] capacity = new int[M][N];
        int[] sum_use = new int[N];
        for (int[] node : nodeOrder) {
            int client = node[0];
            LinkedList<Integer> list = mapClientToEdge.get(client);
            for (int edge : list) {
                if (val[client] > 0) {
                    if (sum_use[edge] + val[client] <= edgeMax[edge]) {
                        capacity[client][edge] = val[client];
                        sum_use[edge] += val[client];
                    } else {
                        capacity[client][edge] = edgeMax[edge] - sum_use[edge];
                        sum_use[edge] = edgeMax[edge];
                    }
                    val[client] -= capacity[client][edge];
                }
            }
        }
        write(capacity, pw);
    }

    public void write(int[][] capacity, PrintWriter pw) {
        for (int i = 0; i < M; i++) {
            boolean flag = false;
            StringBuilder sb = new StringBuilder();
            sb.append(clientNodes[i]).append(":");
            for (int j = 0; j < N; j++) {
                if (capacity[i][j] != 0) {
                    flag = true;
                    sb.append("<").append(edgeNodes[j]).append(",").append(capacity[i][j]).append(">,");
                }
            }
            if (flag) pw.print(sb.substring(0, sb.length() - 1));
            else pw.print(sb);
            pw.print("\n");
        }
    }

    public void createFile(String destFileName) {
        File file = new File(destFileName);
        if (file.exists()) return;
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        com.huawei.java.main.Main main = new com.huawei.java.main.Main();
        String path = "/output/solution.txt";
        main.createFile(path);
        String ini_path = "/data/config.ini";
        main.read_ini(ini_path);
        String base_path = "/data/qos.csv";
        main.read_base(base_path);
        String site_path = "/data/site_bandwidth.csv";
        main.read_bandwidth(site_path);
        String time_path = "/data/demand.csv";
        main.read_demand(time_path);
    }
}
