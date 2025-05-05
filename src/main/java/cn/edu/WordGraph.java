package cn.edu;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

// graphviz-java imports
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.parse.Parser;
import guru.nidi.graphviz.model.MutableGraph;

/**
 * WordGraph.java
 *
 * 功能需求:
 * 1. 读入文本并生成有向图（带权邻接表）
 * 2. 在 CLI 上展示有向图，支持导出 DOT 文件及 PNG 图像（基于 graphviz-java）
 * 3. 查询桥接词
 * 4. 根据桥接词生成新文本
 * 5. 计算加权最短路径并突出显示
 * 6. 计算 PageRank (d=0.85)
 * 7. 随机游走并写入文件
 *
 * 使用方法:
 *   java WordGraph [input.txt]
 *
 * 依赖:
 *   graphviz-java (e.g. guru.nidi:graphviz-java:0.18.1)
 */
public class WordGraph {
    private final Map<String, Map<String, Integer>> adj = new HashMap<>();
    private static final double D = 0.85;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String path;
        if (args.length >= 1) {
            path = args[0];
        } else {
            System.out.print("请输入文本文件路径: ");
            path = sc.nextLine();
        }
        WordGraph wg = new WordGraph();
        try {
            wg.buildGraph(path);
        } catch (IOException e) {
            System.err.println("读取文件出错: " + e.getMessage());
            return;
        }
        while (true) {
            System.out.println("\n===== WordGraph CLI =====");
            System.out.println("1) 展示有向图");
            System.out.println("2) 导出 Graphviz PNG 图像");
            System.out.println("3) 查询桥接词");
            System.out.println("4) 生成新文本");
            System.out.println("5) 计算最短路径");
            System.out.println("6) 计算 PageRank");
            System.out.println("7) 随机游走");
            System.out.println("0) 退出");
            System.out.print("选择功能: ");
            String choice = sc.nextLine();
            try {
                switch (choice) {
                    case "1": wg.showDirectedGraph(); break;
                    case "2":
                        wg.exportGraphImage("graph.png");
                        System.out.println("已生成图像文件: graph.png");
                        break;
                    case "3":
                        System.out.print("word1: "); String w1 = sc.nextLine();
                        System.out.print("word2: "); String w2 = sc.nextLine();
                        System.out.println(wg.queryBridgeWords(w1, w2));
                        break;
                    case "4":
                        System.out.print("输入新文本: "); String input = sc.nextLine();
                        System.out.println("新文本: " + wg.generateNewText(input));
                        break;
                    case "5":
                        System.out.print("word1: "); w1 = sc.nextLine();
                        System.out.print("word2: "); w2 = sc.nextLine();
                        System.out.println(wg.calcShortestPath(w1, w2));
                        break;
                    case "6":
                        System.out.print("word: "); String wt = sc.nextLine();
                        System.out.printf("PageRank(%s)=%.6f%n", wt, wg.calPageRank(wt));
                        break;
                    case "7":
                        String walk = wg.randomWalk();
                        System.out.println("随机游走: " + walk);
                        Files.write(Paths.get("walk.txt"), List.of(walk), StandardCharsets.UTF_8);
                        System.out.println("已写入 walk.txt");
                        break;
                    case "0":
                        System.out.println("退出程序。"); sc.close(); return;
                    default:
                        System.out.println("无效选择，请重试。");
                }
            } catch (Exception ex) {
                System.err.println("操作失败: " + ex.getMessage());
            }
        }
    }

    public void buildGraph(String filepath) throws IOException {
        String content = Files.readString(Paths.get(filepath), StandardCharsets.UTF_8);
        content = content.replaceAll("\\R", " ");
        content = content.replaceAll("\\p{Punct}", " ");
        content = content.replaceAll("[^a-zA-Z\\s]", "");
        content = content.toLowerCase();
        String[] words = content.split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            String u = words[i], v = words[i+1];
            if (u.isEmpty() || v.isEmpty()) continue;
            adj.computeIfAbsent(u, k -> new HashMap<>())
                    .merge(v, 1, Integer::sum);
            adj.computeIfAbsent(v, k -> new HashMap<>());
        }
    }

    public void showDirectedGraph() {
        System.out.println("有向图边 (u -> v [w]):");
        for (String u : adj.keySet()) {
            for (var e : adj.get(u).entrySet()) {
                System.out.printf("%s -> %s [w=%d]%n", u, e.getKey(), e.getValue());
            }
        }
    }

    /**
     * 使用纯 Java API graphviz-java 生成 PNG 图像
     */
    public void exportGraphImage(String imgPath) throws IOException {
        // 构造 DOT 源
        StringBuilder sb = new StringBuilder("digraph G {\n");
        for (String u : adj.keySet()) {
            for (var e : adj.get(u).entrySet()) {
                sb.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\"];\n",
                        u, e.getKey(), e.getValue()));
            }
        }
        sb.append("}\n");
        // 解析并渲染
        Parser parser = new Parser();
        MutableGraph g = parser.read(sb.toString());
        Graphviz.fromGraph(g)
                .render(Format.PNG)
                .toFile(new File(imgPath));
    }

    public String queryBridgeWords(String word1, String word2) {
        word1 = word1.toLowerCase(); word2 = word2.toLowerCase();
        if (!adj.containsKey(word1) || !adj.containsKey(word2))
            return "No word1 or word2 in the graph!";
        List<String> bridges = new ArrayList<>();
        for (String mid : adj.get(word1).keySet()) {
            if (adj.getOrDefault(mid, Map.of()).containsKey(word2)) {
                bridges.add(mid);
            }
        }
        if (bridges.isEmpty())
            return String.format("No bridge words from %s to %s!", word1, word2);
        return String.format("The bridge words from %s to %s are: %s.",
                word1, word2, String.join(", ", bridges));
    }

    public String generateNewText(String inputText) {
        String txt = inputText.replaceAll("\\p{Punct}", " ")
                .replaceAll("[^a-zA-Z\\s]", "")
                .toLowerCase();
        String[] ws = txt.split("\\s+");
        Random r = new Random();
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ws.length - 1; i++) {
            String u = ws[i], v = ws[i+1];
            out.add(u);
            List<String> bs = new ArrayList<>();
            for (String mid : adj.getOrDefault(u, Map.of()).keySet()) {
                if (adj.getOrDefault(mid, Map.of()).containsKey(v)) bs.add(mid);
            }
            if (!bs.isEmpty()) out.add(bs.get(r.nextInt(bs.size())));
        }
        if (ws.length > 0) out.add(ws[ws.length - 1]);
        return String.join(" ", out);
    }

    public String calcShortestPath(String src, String dst) {
        src = src.toLowerCase(); dst = dst.toLowerCase();
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Map.Entry<String, Integer>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());
        for (String u : adj.keySet()) dist.put(u, Integer.MAX_VALUE);
        if (!dist.containsKey(src) || !dist.containsKey(dst))
            return "One or both words not in graph.";
        dist.put(src, 0);
        pq.offer(new AbstractMap.SimpleEntry<>(src, 0));
        while (!pq.isEmpty()) {
            var e = pq.poll(); String u = e.getKey(); int du = e.getValue();
            if (du > dist.get(u)) continue;
            if (u.equals(dst)) break;
            for (var en : adj.get(u).entrySet()) {
                String v = en.getKey(); int w = en.getValue();
                int alt = du + w;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.offer(new AbstractMap.SimpleEntry<>(v, alt));
                }
            }
        }
        if (!prev.containsKey(dst)) return "No path from " + src + " to " + dst;
        List<String> path = new LinkedList<>();
        for (String at = dst; at != null; at = prev.get(at)) path.add(0, at);
        return String.format("Path: %s (cost=%d)", String.join(" -> ", path), dist.get(dst));
    }

    public double calPageRank(String target) {
        target = target.toLowerCase();
        int N = adj.size();
        Map<String, Double> pr = new HashMap<>();
        double init = 1.0 / N;
        for (String u : adj.keySet()) pr.put(u, init);
        for (int iter = 0; iter < 20; iter++) {
            Map<String, Double> next = new HashMap<>();
            for (String u : adj.keySet()) next.put(u, (1-D)/N);
            for (String u : adj.keySet()) {
                int outW = adj.get(u).values().stream().mapToInt(i -> i).sum();
                if (outW == 0) continue;
                for (var en : adj.get(u).entrySet()) {
                    String v = en.getKey(); int w = en.getValue();
                    next.put(v, next.get(v) + D * pr.get(u) * (w / (double)outW));
                }
            }
            pr = next;
        }
        return pr.getOrDefault(target, 0.0);
    }

    public String randomWalk() {
        if (adj.isEmpty()) return "Graph empty.";
        Random r = new Random();
        List<String> nodes = new ArrayList<>(adj.keySet());
        String cur = nodes.get(r.nextInt(nodes.size()));
        List<String> walk = new ArrayList<>();
        Set<String> seenEdges = new HashSet<>();
        walk.add(cur);
        while (true) {
            var neigh = adj.get(cur);
            if (neigh.isEmpty()) break;
            List<String> vs = new ArrayList<>(neigh.keySet());
            String nxt = vs.get(r.nextInt(vs.size()));
            String edge = cur + "->" + nxt;
            if (seenEdges.contains(edge)) break;
            seenEdges.add(edge);
            walk.add(nxt);
            cur = nxt;
        }
        return String.join(" -> ", walk);
    }
}
