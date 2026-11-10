package com.aurora.camera.features.watermark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Publish an entire bundled asset without removing a file OEM readers use. */
public final class AtomicWatermarkAsset234 {
    private AtomicWatermarkAsset234(){}
    public static void copy(InputStream input,Path cacheDirectory,Path target)throws IOException{
        Path temporary=null;
        try(InputStream source=input){
            // The camera cache is outside the watermark tree enumerated by Gg.r.c.
            temporary=Files.createTempFile(cacheDirectory,"aurora-wm-",".tmp");
            Files.copy(source,temporary,StandardCopyOption.REPLACE_EXISTING);
            // App cache/files share a filesystem. Failure keeps the old target;
            // never fall back to unlinking a file that another thread reads.
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{
            if(temporary!=null)Files.deleteIfExists(temporary);
        }
    }
}
