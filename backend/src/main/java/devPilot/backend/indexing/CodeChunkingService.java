package devPilot.backend.indexing;

import java.util.*;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Lightweight language-aware chunker. It favours declarations, then falls back to line windows. */
@Service
public class CodeChunkingService {
    private static final Pattern DECLARATION = Pattern.compile("(?m)^\\s*(?:public|private|protected|internal|static|final|async|export|class|interface|enum|record|fun|def|function|const|let|var|CREATE\\s+(?:TABLE|FUNCTION|PROCEDURE))\\b[^\\n{;]*");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*(?:import|from|package|using|#include)\\s+([^;\\n]+)");
    @Value("${app.indexing.chunk-size:800}") private int chunkSize;
    @Value("${app.indexing.chunk-overlap:100}") private int chunkOverlap;

    public List<ChunkDraft> chunk(String content, String language) {
        if (content == null || content.isBlank()) return List.of();
        String[] lines = content.split("\\R", -1);
        List<Integer> boundaries = new ArrayList<>(); boundaries.add(0);
        Matcher declarations = DECLARATION.matcher(content);
        while (declarations.find()) { int line = lineOf(content, declarations.start()); if (line > 0 && !boundaries.contains(line - 1)) boundaries.add(line - 1); }
        boundaries.add(lines.length); Collections.sort(boundaries);
        List<String> imports = new ArrayList<>(); Matcher importMatcher = IMPORT.matcher(content);
        while (importMatcher.find()) imports.add(importMatcher.group(1).trim());
        List<ChunkDraft> result = new ArrayList<>();
        for (int b=0; b<boundaries.size()-1; b++) addWindows(lines, boundaries.get(b), boundaries.get(b+1), imports, result);
        return result;
    }
    private void addWindows(String[] lines,int from,int to,List<String> imports,List<ChunkDraft> out){
        int start=from;
        while(start<to){ int end=start; int chars=0; while(end<to && chars+lines[end].length()+1<=chunkSize){ chars+=lines[end++].length()+1; }
            if(end==start) end=Math.min(to,start+1);
            String text=String.join("\n",Arrays.copyOfRange(lines,start,end));
            if(!text.isBlank()) out.add(new ChunkDraft(text,start+1,end,findSymbol(text),imports));
            if(end>=to) break; int overlap=0,next=end; while(next>start && overlap<chunkOverlap) overlap+=lines[--next].length()+1; start=Math.max(start+1,next);
        }
    }
    private static int lineOf(String content,int position){ int n=1; for(int i=0;i<position;i++)if(content.charAt(i)=='\n')n++; return n; }
    private static String findSymbol(String content){ Matcher m=DECLARATION.matcher(content); if(!m.find()) return null; String v=m.group().trim(); String[] p=v.split("\\s+"); return p.length==0?null:p[p.length-1].replaceAll("[({].*", ""); }
    public record ChunkDraft(String content,int startLine,int endLine,String symbolName,List<String> imports) {}
}
