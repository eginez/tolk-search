package index.tolkien;

import io.micronaut.core.annotation.ReflectionConfig;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.runtime.Micronaut;
import java.nio.file.*;
import java.util.List;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.search.*;
import java.io.*;
import java.util.*;


@ReflectionConfig(type=org.apache.lucene.index.ConcurrentMergeScheduler.class,
        accessType = {TypeHint.AccessType.ALL_DECLARED_METHODS, TypeHint.AccessType.ALL_DECLARED_CONSTRUCTORS})
@ReflectionConfig(type=org.apache.lucene.index.IndexWriter.class,
        accessType = {TypeHint.AccessType.ALL_DECLARED_METHODS, TypeHint.AccessType.ALL_DECLARED_CONSTRUCTORS})
@ReflectionConfig(type=org.apache.lucene.store.MMapDirectory.class,
        accessType = {TypeHint.AccessType.ALL_DECLARED_METHODS, TypeHint.AccessType.ALL_DECLARED_CONSTRUCTORS})
public class Application {
    public static void main(String[] args) {
        //Micronaut.run(Application.class, args);
        var app = new Application();
        app.run(args);
    }

    public void run(String[] args) {
        try {
            var search = true;
            if (search) {
                search(args[0]);
                return;
            }
            var lines = readFile("/Users/eginez/src/index-tolken/data/silmarill.txt");
            var docs = createDocument(lines);
            var index = openIndex(true);
            for(var doc : docs){
                index.addDocument(doc);
            }
            index.commit();
            index.close();
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> readFile(String filename) throws IOException {
        Path p = Paths.get(filename);
        var allLines = Files.readAllLines(p);
        var res = new ArrayList();
        for(String line : allLines) {
            if (!line.isBlank()){
                res.add(line);
            }
        }
        return res;
    }

    public void search(String query) throws Exception {
        System.out.println("searching for " + query);
        var indexWriter = openIndex(false);
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(indexWriter));
        TopDocs search = searcher.search(new TermQuery(new Term("content", query.toLowerCase())), 100);

        int i = 0;
        for (var sdoc : search.scoreDocs) {
            var doc = searcher.doc(sdoc.doc);
            System.out.println(String.format("%d) %s %s", i, doc.get("lineNumber") , doc.get("content")));
            i++;
        }
        indexWriter.close();
        
    }

    public static List<Document> createDocument(List<String> lines) {
        List<Document> all = new ArrayList<>();
        for(int i =0; i < lines.size(); i++) {
            Field fieldPn = new Field("lineNumber", String.format("%d", i), TextField.TYPE_STORED);
            Field fieldContent = new Field("content", lines.get(i), TextField.TYPE_STORED);
            Document doc = new Document();
            doc.add(fieldPn);
            doc.add(fieldContent);
            all.add(doc);
        };
        return all;
    }

    public record IndexableData(int id, float[] embeddings, String text){};


    public static List<Document> createDocumentWithEmbeddings(List<IndexableData> data) {
        List<Document> all = new ArrayList<>();
        for(IndexableData d : data) {
            Field content = new StringField("contents", d.text(), Field.Store.YES);
            Field embeddigns = new KnnVectorField("embeddings", d.embeddings(), VectorSimilarityFunction.COSINE);
            Document doc = new Document();
            doc.add(content);
            doc.add(embeddigns);
            all.add(doc);
        };
        return all;
    }

     public static IndexWriter openIndex(boolean clear) throws IOException,InterruptedException {
        if (clear)
            new ProcessBuilder("rm -rf ./index".split(" ")).start().waitFor();
        Path indexPath = new File("./index/").toPath();
        FSDirectory indexDir = FSDirectory.open(indexPath);
         return new IndexWriter(indexDir, new IndexWriterConfig());
    }
}
