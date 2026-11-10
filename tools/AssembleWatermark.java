import java.nio.file.*;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import brut.androlib.smali.SmaliBuilder;
/** Compile retained, narrowly extracted Phoenix composition helpers. */
public class AssembleWatermark {
    public static void main(String[] args)throws Exception {
        DexBuilder dex=new DexBuilder(new Opcodes(31));SmaliBuilder smali=new SmaliBuilder(31);
        try(java.util.stream.Stream<Path> files=Files.walk(Paths.get(args[0]))){
            for(Path p:(Iterable<Path>)files.filter(v->v.toString().endsWith(".smali")).sorted()::iterator)
                if(!smali.buildFile(p.toFile(),dex))throw new IllegalStateException("Smali assembly failed: "+p);
        }
        dex.writeTo(new FileDataStore(Paths.get(args[1]).toFile()));
    }
}
