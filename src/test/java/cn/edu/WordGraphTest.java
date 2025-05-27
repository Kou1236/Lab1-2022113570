package cn.edu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

class WordGraphTest {

    @Test
    void testSelfLoop(@TempDir Path tempDir) throws Exception {
        // 构造仅含自环 "a->a" 的输入文件
        Path file = tempDir.resolve("input.txt");
        Files.writeString(file, "a a");

        WordGraph graph = new WordGraph();
        graph.buildGraph(file.toString());

        String result = graph.randomWalk();
        assertEquals(
                "a -> a",
                result
        );
    }
}
