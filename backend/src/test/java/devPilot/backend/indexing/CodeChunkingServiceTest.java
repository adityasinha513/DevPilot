package devPilot.backend.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CodeChunkingServiceTest {
    private CodeChunkingService chunker;

    @BeforeEach
    void setUp() {
        chunker = new CodeChunkingService();
        ReflectionTestUtils.setField(chunker, "chunkSize", 120);
        ReflectionTestUtils.setField(chunker, "chunkOverlap", 20);
    }

    @Test
    void preservesDeclarationLineRangesAndImports() {
        String source = "package demo;\nimport java.util.List;\n\npublic class Orders {\n  public void create() {}\n}\n";
        List<CodeChunkingService.ChunkDraft> chunks = chunker.chunk(source, "Java");
        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.startLine()).isPositive();
            assertThat(chunk.endLine()).isGreaterThanOrEqualTo(chunk.startLine());
            assertThat(chunk.imports()).contains("java.util.List");
        });
    }

    @Test
    void isDeterministicForPlainTextFallback() {
        String text = "one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\n";
        assertThat(chunker.chunk(text, "Markdown")).isEqualTo(chunker.chunk(text, "Markdown"));
    }
}
