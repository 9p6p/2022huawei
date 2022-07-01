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
    Map<Integer, LinkedList<Integer>> mapEdgeToClient;
    int[][] nodeOrder;
    int[][] clientOrder;
    int five_percent;
    int[] nodeUseNum;
    Map<String, Integer> projectionToEdge;
    int[][][] finalTable;
    int[][] F_table;
    int F_index = 0;
    int[] fivePercentNumTable;

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
            clientOrder = new int[N][2];
            mapEdgeToClient = new HashMap<>();
            for (int j = 0; j < N; j++) {
                LinkedList<Integer> list = new LinkedList<>();
                clientOrder[j][0] = j;
                int count = 0;
                for (int i = 0; i < M; i++) {
                    if (transfer[i][j]) {
                        count++;
                        list.add(i);
                    }
                }
                clientOrder[j][1] = count;
                mapEdgeToClient.put(j, list);
            }
            Arrays.sort(clientOrder, Comparator.comparingInt(o -> o[1]));
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
        try (BufferedReader bt = new BufferedReader(new FileReader(filepath))) {
            int index = -1;
            while (bt.readLine() != null) {
                index++;
            }
            T = index;
            five_percent = T - (int) Math.ceil(T * 0.95);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read_and_solute(String filepath) {
        String path = "/output/solution.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filepath));
             PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            String line;
            br.readLine();
            nodeUseNum = new int[N];
            Arrays.fill(nodeUseNum, five_percent - 1);
            finalTable = new int[T][M][N];
            F_table = new int[T][N];
            fivePercentNumTable = new int[N];
            while ((line = br.readLine()) != null) {
                String[] element = line.split(",");
                greed(element);
                F_index++;
            }
            writeTable(finalTable, pw);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void greed(String[] element) {
        int count;
        Random random = new Random();
        count = random.nextInt(6);
        int[][] capacity;
        switch (count) {
            case 0 -> capacity = solutionOrder(element);
            case 1 -> capacity = solutionEdgeRandom(element);
            case 2 -> capacity = solutionAverage(element);
            case 3 -> capacity = solutionRandom(element);
            default -> {
                Set<Integer> useNode = new HashSet<>();
                capacity = solutionEdgeOrder(element, useNode);
                for (int edge : useNode) {
                    nodeUseNum[edge]--;
                }
            }
        }
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < M; i++) {
                F_table[F_index][j] += capacity[i][j];
                finalTable[F_index][i][j] = capacity[i][j];
            }
        }
        for (int j = 0; j < N; j++) {
            int[] use = new int[F_index + 1];
            for (int i = 0; i <= F_index; i++) {
                use[i] += F_table[i][j];
            }
            Arrays.sort(use);
            fivePercentNumTable[j] = use[(int) Math.ceil((F_index + 1) * 0.95) - 1];
        }
    }

    public int getCost(int[][] t_table, int t_index, int[] capacityTable) {
        int[] fivePercentNum = new int[N];
        for (int j = 0; j < N; j++) {
            int[] use = new int[t_index + 1];
            for (int i = 0; i < t_index; i++) {
                use[i] += t_table[i][j];
            }
            use[t_index] += capacityTable[j];
            Arrays.sort(use);
            fivePercentNum[j] = use[(int) Math.ceil((t_index + 1) * 0.95) - 1];
        }
        int out = 0;
        for (int i = 0; i < N; i++) {
            out += fivePercentNum[i];
        }
        return out;
    }

    public int[][] solutionAverage(String[] strings) {
        int[] val = new int[M];
        for (int i = 0; i < M; i++) val[i] = Integer.parseInt(strings[i + 1]);
        int[][] capacity = new int[M][N];
        int[] sum_use = new int[N];
        Random rand = new Random();
        for (int[] node : nodeOrder) {
            int client = node[0];
            LinkedList<Integer> list = mapClientToEdge.get(client);
            boolean flag = false;
            while (val[client] > 0) {
                if (flag) {
                    Set<Integer> u = new HashSet<>();
                    while (val[client] > 0) {
                        int use = rand.nextInt(list.size());
                        while (u.contains(use)) {
                            use = rand.nextInt(list.size());
                        }
                        u.add(use);
                        int edge = list.get(use);
                        int add;
                        if (sum_use[edge] + val[client] <= edgeMax[edge]) {
                            add = val[client];
                            capacity[client][edge] += val[client];
                            sum_use[edge] += val[client];
                        } else {
                            add = edgeMax[edge] - sum_use[edge];
                            capacity[client][edge] += edgeMax[edge] - sum_use[edge];
                            sum_use[edge] = edgeMax[edge];
                        }
                        val[client] -= add;
                    }
                } else {
                    int k = val[client] / list.size();
                    for (int edge : list) {
                        if (sum_use[edge] + k <= edgeMax[edge]) {
                            capacity[client][edge] = k;
                            sum_use[edge] += k;
                        } else {
                            capacity[client][edge] = edgeMax[edge] - sum_use[edge];
                            sum_use[edge] = edgeMax[edge];
                        }
                        val[client] -= capacity[client][edge];
                    }
                    flag = true;
                }
            }
        }
        return capacity;
    }

    public int[][] solutionRandom(String[] strings) {
        int[] val = new int[M];
        for (int i = 0; i < M; i++) val[i] = Integer.parseInt(strings[i + 1]);
        int[][] capacity = new int[M][N];
        int[] sum_use = new int[N];
        Random rand = new Random();
        for (int[] node : nodeOrder) {
            int client = node[0];
            LinkedList<Integer> list = mapClientToEdge.get(client);
            Set<Integer> u = new HashSet<>();
            while (val[client] > 0) {
                int use = rand.nextInt(list.size());
                while (u.contains(use)) {
                    use = rand.nextInt(list.size());
                }
                u.add(use);
                int edge = list.get(use);
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
        return capacity;
    }

    public int[][] solutionEdgeRandom(String[] strings) {
        int[] val = new int[M];
        for (int i = 0; i < M; i++) val[i] = Integer.parseInt(strings[i + 1]);
        int[][] capacity = new int[M][N];
        int[] sum_use = new int[N];
        for (int[] node : clientOrder) {
            int edgeNode = node[0];
            int k = fivePercentNumTable[edgeNode];
            LinkedList<Integer> list = mapEdgeToClient.get(edgeNode);
            if (sum_use[edgeNode] < k) {
                for (int client : list) {
                    if (sum_use[edgeNode] < k && val[client] > 0) {
                        if (sum_use[edgeNode] + val[client] <= k) {
                            capacity[client][edgeNode] += val[client];
                            sum_use[edgeNode] += val[client];
                            val[client] = 0;
                        } else {
                            capacity[client][edgeNode] = k - sum_use[edgeNode];
                            val[client] -= k - sum_use[edgeNode];
                            sum_use[edgeNode] = k;
                        }
                    }
                }
            }
        }
        Random rand = new Random();
        for (int[] node : nodeOrder) {
            int client = node[0];
            LinkedList<Integer> list = mapClientToEdge.get(client);
            Set<Integer> u = new HashSet<>();
            while (val[client] > 0) {
                int use = rand.nextInt(list.size());
                while (u.contains(use)) {
                    use = rand.nextInt(list.size());
                }
                u.add(use);
                int edge = list.get(use);
                if (sum_use[edge] + val[client] <= edgeMax[edge]) {
                    capacity[client][edge] += val[client];
                    sum_use[edge] += val[client];
                    val[client] = 0;
                } else {
                    capacity[client][edge] += edgeMax[edge] - sum_use[edge];
                    val[client] -= edgeMax[edge] - sum_use[edge];
                    sum_use[edge] = edgeMax[edge];
                }
            }
        }
        return capacity;
    }

    public int[][] solutionOrder(String[] strings) {
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
        return capacity;
    }

    public int[][] solutionEdgeOrder(String[] strings, Set<Integer> useNode) {
        int[] val = new int[M];
        for (int i = 0; i < M; i++) val[i] = Integer.parseInt(strings[i + 1]);
        int[][] capacity = new int[M][N];
        int[] sum_use = new int[N];
        for (int[] node : clientOrder) {
            int edgeNode = node[0];
            LinkedList<Integer> list = mapEdgeToClient.get(edgeNode);
            if (nodeUseNum[edgeNode] < 0) {
                int k = fivePercentNumTable[edgeNode + 1];
                for (int client : list) {
                    if (val[client] > 0) {
                        if (sum_use[edgeNode] + k <= edgeMax[edgeNode]) {
                            capacity[client][edgeNode] += k;
                            sum_use[edgeNode] += val[client];
                            val[client] -= k;
                        } else {
                            capacity[client][edgeNode] = edgeMax[edgeNode] - sum_use[edgeNode];
                            val[client] -= edgeMax[edgeNode] - sum_use[edgeNode];
                            sum_use[edgeNode] = edgeMax[edgeNode];
                        }
                    }
                }
            } else {
                for (int client : list) {
                    if (val[client] > 0) {
                        if (sum_use[edgeNode] + val[client] <= edgeMax[edgeNode]) {
                            capacity[client][edgeNode] = val[client];
                            sum_use[edgeNode] += val[client];
                        } else {
                            capacity[client][edgeNode] = edgeMax[edgeNode] - sum_use[edgeNode];
                            sum_use[edgeNode] = edgeMax[edgeNode];
                        }
                        val[client] -= capacity[client][edgeNode];
                        useNode.add(edgeNode);
                    }
                }
            }
        }
        return capacity;
    }

    public void writeTable(int[][][] capacityTable, PrintWriter pw) {
        for (int k = 0; k < T; k++) {
            for (int i = 0; i < M; i++) {
                boolean flag = false;
                StringBuilder sb = new StringBuilder();
                sb.append(clientNodes[i]).append(":");
                for (int j = 0; j < N; j++) {
                    if (capacityTable[k][i][j] != 0) {
                        flag = true;
                        sb.append("<").append(edgeNodes[j]).append(",").append(capacityTable[k][i][j]).append(">,");
                    }
                }
                if (flag) pw.print(sb.substring(0, sb.length() - 1));
                else pw.print(sb);
                pw.print("\n");
            }
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
        main.read_and_solute(time_path);
    }
}
