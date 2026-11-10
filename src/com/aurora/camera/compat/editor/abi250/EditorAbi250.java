package com.aurora.camera.compat.editor.abi250;

import com.aurora.camera.compat.Host;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Editor-only static symbol resolver. A result is installed only after Host checks its ABI. */
public final class EditorAbi250 implements Host.Symbols {
    private static final String ASSET="assets/editor250/contract.tsv";
    private static final String NATIVE_ASSET="assets/editor291/contract.tsv";
    private static final String NATIVE_ALIASES="assets/editor291/aliases.tsv";
    private static final int CACHE_FORMAT=0x41424931;
    public boolean cacheHit;
    public String cacheIssue;
    private final Map<String,String> types=new LinkedHashMap<>(),reverseTypes=new LinkedHashMap<>();
    private final Map<String,String> methods=new LinkedHashMap<>(),fields=new LinkedHashMap<>();
    private final Map<String,String> reverseMethods=new LinkedHashMap<>(),reverseFields=new LinkedHashMap<>();
    private final Map<String,int[]> resources=new LinkedHashMap<>();
    private final Map<String,String> reasons=new LinkedHashMap<>();
    private final Map<String,String> typeAliases=new LinkedHashMap<>(),fieldAliases=new LinkedHashMap<>();
    private final Map<String,Set<String>> methodAliases=new LinkedHashMap<>();
    private Profile profile;
    private Dex250.Index index;
    private final Map<String,List<Dex250.Clazz>> candidates=new LinkedHashMap<>();
    private final Set<String> propagated=new LinkedHashSet<>();
    private int changes;

    private static final class PClass {
        String name,shape;
        final List<PMethod> methods=new ArrayList<>();
        final List<PField> fields=new ArrayList<>();
    }
    private static final class PMethod {
        String owner,name,descriptor,fingerprint,referenceFingerprint;
        int flags;
        final List<Ref> refs=new ArrayList<>();
        String id(){return owner+"->"+name+descriptor;}
    }
    private static final class PField {
        String owner,name,type,usage;
        int flags;
        String id(){return owner+"->"+name+":"+type;}
    }
    private static final class Ref {
        int pc,opcode,word;
        String kind,target;
    }
    private static final class Profile {
        final Map<String,PClass> classes=new LinkedHashMap<>();
        final Map<String,PMethod> methods=new LinkedHashMap<>();
        final Map<String,PField> fields=new LinkedHashMap<>();
    }

    private EditorAbi250() {}
    public static EditorAbi250 resolve(String targetApk,String moduleApk)throws IOException {
        try(ZipFile zip=new ZipFile(moduleApk)) {
            ZipEntry entry=zip.getEntry(ASSET);
            if(entry==null)throw new IOException("Editor250 symbol contract is missing from module");
            try(InputStream in=zip.getInputStream(entry)){return resolve(targetApk,in);}
        }
    }
    public static EditorAbi250 resolve(String targetApk,InputStream contract)throws IOException {
        EditorAbi250 result=new EditorAbi250();
        result.profile=readProfile(contract);
        result.resolveTarget(targetApk);
        return result;
    }

    public static EditorAbi250 resolve(String targetApk,InputStream contract,InputStream aliases)throws IOException {
        EditorAbi250 result=new EditorAbi250();
        result.profile=readProfile(contract);
        result.readAliases(aliases);
        result.resolveTarget(targetApk);
        return result;
    }

    @Override public boolean usesNativeCloudFilter(){return !typeAliases.isEmpty();}

    private void readAliases(InputStream input)throws IOException {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
            String line;while((line=reader.readLine())!=null) {
                if(line.isEmpty()||line.startsWith("#"))continue;
                String[] row=line.split("\t",-1);
                if(row[0].equals("T"))typeAliases.put(row[1],row[2]);
                else if(row[0].equals("M"))methodAliases.computeIfAbsent(row[1],key->new LinkedHashSet<>()).add(row[2]);
                else if(row[0].equals("F"))fieldAliases.put(row[1],row[2]);
                else throw new IOException("Unknown editor symbol alias: "+row[0]);
            }
        }
    }

    /** Android installs APKs at immutable package paths; both APK identities invalidate one private cache. */
    public static EditorAbi250 resolveCached(String targetApk,String moduleApk,File directory)throws IOException {
        return resolveCached(targetApk,moduleApk,directory,false);
    }
    public static EditorAbi250 resolveCached(String targetApk,String moduleApk,File directory,ClassLoader loader)throws IOException {
        boolean nativeCloud;
        try {Class.forName("com.miui.mediaeditor.photo.filter.viewmodel.PhotoFilterViewModel",false,loader);nativeCloud=false;}
        catch(ClassNotFoundException renamed){nativeCloud=true;}
        return resolveCached(targetApk,moduleApk,directory,nativeCloud);
    }
    private static EditorAbi250 resolveCached(String targetApk,String moduleApk,File directory,boolean nativeCloud)throws IOException {
        EditorAbi250 result=new EditorAbi250();
        try(ZipFile zip=new ZipFile(moduleApk)) {
            ZipEntry entry=zip.getEntry(nativeCloud?NATIVE_ASSET:ASSET);
            if(entry==null)throw new IOException("Editor250 symbol contract is missing from module");
            try(InputStream in=zip.getInputStream(entry)){result.profile=readProfile(in);}
            if(nativeCloud) {
                ZipEntry aliases=zip.getEntry(NATIVE_ALIASES);
                if(aliases==null)throw new IOException("Native cloud editor symbol aliases are missing from module");
                try(InputStream in=zip.getInputStream(aliases)){result.readAliases(in);}
            }
        }
        String identity=fileIdentity(targetApk)+"\n"+fileIdentity(moduleApk);
        File cache=new File(directory,"aurora-editor-abi251.bin");
        if(cache.isFile()) {
            try {
                if(result.readCache(cache,identity)){result.cacheHit=true;return result;}
            } catch(IOException failure) {
                result.cacheIssue="cache read: "+failure.getMessage();
                result.clearResolved();
            }
        }
        result.resolveTarget(targetApk);
        try {result.writeCache(cache,identity);}
        catch(IOException failure){result.cacheIssue="cache write: "+failure.getMessage();}
        return result;
    }

    private static String fileIdentity(String path)throws IOException {
        File file=new File(path);
        if(!file.isFile())throw new IOException("Editor APK is missing: "+path);
        return file.getAbsolutePath()+":"+file.length()+":"+file.lastModified();
    }
    private boolean readCache(File file,String identity)throws IOException {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if(in.readInt()!=CACHE_FORMAT||!in.readUTF().equals(identity))return false;
            readMap(in,types);readMap(in,methods);readMap(in,fields);
            int count=readCount(in);
            for(int i=0;i<count;i++) {
                String key=in.readUTF();int[] values=new int[readCount(in)];
                for(int j=0;j<values.length;j++)values[j]=in.readInt();
                resources.put(key,values);
            }
            if(in.read()!=-1)throw new IOException("Trailing editor symbol cache data");
            for(Map.Entry<String,String> e:types.entrySet())reverseTypes.put(e.getValue(),e.getKey());
            return true;
        }
    }
    private static int readCount(DataInputStream in)throws IOException {
        int count=in.readInt();if(count<0)throw new IOException("Invalid editor symbol cache count");return count;
    }
    private static void readMap(DataInputStream in,Map<String,String> output)throws IOException {
        int count=readCount(in);for(int i=0;i<count;i++)output.put(in.readUTF(),in.readUTF());
    }
    private static void writeMap(DataOutputStream out,Map<String,String> values)throws IOException {
        out.writeInt(values.size());for(Map.Entry<String,String> e:values.entrySet()){out.writeUTF(e.getKey());out.writeUTF(e.getValue());}
    }
    private void writeCache(File file,String identity)throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Path temporary=Files.createTempFile(file.getParentFile().toPath(),"editor-abi251-",".tmp");
        try {
            try(FileOutputStream stream=new FileOutputStream(temporary.toFile());DataOutputStream out=new DataOutputStream(new BufferedOutputStream(stream))) {
                out.writeInt(CACHE_FORMAT);out.writeUTF(identity);
                writeMap(out,types);writeMap(out,methods);writeMap(out,fields);
                out.writeInt(resources.size());
                for(Map.Entry<String,int[]> e:resources.entrySet()) {
                    out.writeUTF(e.getKey());out.writeInt(e.getValue().length);
                    for(int value:e.getValue())out.writeInt(value);
                }
                out.flush();stream.getFD().sync();
            }
            Files.move(temporary,file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(temporary);}
    }
    private void clearResolved() {
        types.clear();reverseTypes.clear();methods.clear();fields.clear();resources.clear();
    }

    private void resolveTarget(String targetApk)throws IOException {
        index=Dex250.readApk(targetApk);
        solve();
        for(String id:methods.values()) {
            Dex250.Method method=findActualMethod(id);
            Set<Integer> values=new LinkedHashSet<>();
            int[] code=method.code();
            for(int pc=0;pc<code.length;) {
                int op=code[pc]&255;
                if(op==0x14){int value=code[pc+1]|code[pc+2]<<16;if((value&0xff000000)==0x7f000000)values.add(value);}
                pc+=Dex250.width(code,pc);
            }
            int[] array=new int[values.size()];int at=0;for(int value:values)array[at++]=value;
            resources.put(id,array);
        }
        // The runtime object retains only resolved names, compact contract rows and constants.
        // APK bytes and every parsed class/code table become collectible before hooks install.
        index=null;candidates.clear();propagated.clear();
    }

    @Override public String typeName(String oldBinary)throws ClassNotFoundException {
        String old=descriptor(oldBinary);old=typeAliases.getOrDefault(old,old);String actual=types.get(old);
        if(actual==null&&stable(old))actual=old;
        if(actual==null)throw new ClassNotFoundException("Editor250 unresolved class "+oldBinary);
        return binary(actual);
    }
    @Override public String methodName(String actualOwnerBinary,String oldName,String actualPrototype)throws NoSuchMethodException {
        String actualOwner=descriptor(actualOwnerBinary),oldOwner=reverseTypes.get(actualOwner);
        if(oldOwner==null&&stable(actualOwner))oldOwner=actualOwner;
        Set<String> matches=new LinkedHashSet<>();
        Set<String> aliases=methodAliases.get(oldOwner+"->"+oldName);
        for(PMethod pm:profile.methods.values())if(pm.owner.equals(oldOwner)&&(aliases==null?pm.name.equals(oldName):aliases.contains(pm.id()))) {
            String id=methods.get(pm.id());if(id==null)continue;
            int arrow=id.indexOf("->"),paren=id.indexOf('(',arrow),end=id.indexOf(')',paren)+1;
            String prototype=id.substring(paren);
            if((actualPrototype.endsWith(")")?prototype.substring(0,end-paren):prototype).equals(actualPrototype))
                matches.add(id.substring(arrow+2,paren));
        }
        if(matches.size()!=1)throw new NoSuchMethodException("Editor250 unresolved/ambiguous method "+actualOwnerBinary+"#"+oldName+actualPrototype+" matches="+matches);
        return matches.iterator().next();
    }
    @Override public String fieldName(String actualOwnerBinary,String oldName)throws NoSuchFieldException {
        String actualOwner=descriptor(actualOwnerBinary),oldOwner=reverseTypes.get(actualOwner);
        if(oldOwner==null&&stable(actualOwner))oldOwner=actualOwner;
        oldName=fieldAliases.getOrDefault(oldOwner+"->"+oldName,oldName);
        Set<String> matches=new LinkedHashSet<>();
        for(PField pf:profile.fields.values())if(pf.owner.equals(oldOwner)&&pf.name.equals(oldName)) {
            String id=fields.get(pf.id());if(id!=null)matches.add(id.substring(id.indexOf("->")+2,id.lastIndexOf(':')));
        }
        if(matches.size()!=1)throw new NoSuchFieldException("Editor250 unresolved/ambiguous field "+actualOwnerBinary+"#"+oldName+" matches="+matches);
        return matches.iterator().next();
    }
    public int[] resourceConstants(String actualMethodId) {
        int[] result=resources.get(actualMethodId);
        if(result==null)throw new IllegalArgumentException("Editor250 method constants unavailable: "+actualMethodId);
        return result.clone();
    }

    private void solve()throws IOException {
        Map<String,List<Dex250.Clazz>> byShape=new LinkedHashMap<>();
        for(Dex250.Clazz cls:index.classes.values())byShape.computeIfAbsent(cls.shape(),k->new ArrayList<>()).add(cls);
        for(PClass cls:profile.classes.values()) {
            List<Dex250.Clazz> list=byShape.get(cls.shape);
            candidates.put(cls.name,list==null?Collections.emptyList():list);
            if(stable(cls.name)&&index.classes.containsKey(cls.name))bindType(cls.name,cls.name,"stable class");
        }
        bindApplicationContext();
        for(int round=0;round<64;round++) {
            changes=0;
            for(PClass cls:profile.classes.values())if(!types.containsKey(cls.name)) {
                List<Dex250.Clazz> available=new ArrayList<>();
                for(Dex250.Clazz c:candidates.get(cls.name))if(typeCompatible(cls.name,c.name))available.add(c);
                if(available.size()==1)bindType(cls.name,available.get(0).name,"unique class shape");
            }
            for(PClass cls:profile.classes.values()) {
                List<Dex250.Clazz> owners=owners(cls.name);
                if(owners.isEmpty())continue;
                for(PMethod pm:cls.methods)if(!methods.containsKey(pm.id())) {
                    List<Dex250.Method> exact=new ArrayList<>();
                    for(Dex250.Clazz owner:owners)for(Dex250.Method m:owner.methods)
                        if(methodCompatible(pm,m)&&pm.fingerprint.equals(m.fingerprint()))exact.add(m);
                    if(exact.size()==1){bindMethod(pm.id(),exact.get(0),"unique method fingerprint");continue;}
                    if(types.containsKey(cls.name)) {
                        List<Dex250.Method> structural=new ArrayList<>();
                        for(Dex250.Method m:owners.get(0).methods)if(methodCompatible(pm,m))structural.add(m);
                        if(structural.size()==1)bindMethod(pm.id(),structural.get(0),"unique mapped prototype");
                    }
                }
                if(!types.containsKey(cls.name))continue;
                Dex250.Clazz owner=index.classes.get(types.get(cls.name));
                for(PField pf:cls.fields)if(!fields.containsKey(pf.id())) {
                    List<Dex250.Field> exact=new ArrayList<>(),structural=new ArrayList<>();
                    for(Dex250.Field f:owner.fields)if(fieldCompatible(pf,f)) {
                        structural.add(f);if(pf.usage.equals(f.usageFingerprint()))exact.add(f);
                    }
                    if(exact.size()==1)bindField(pf.id(),exact.get(0),"unique field use fingerprint");
                    else if(structural.size()==1)bindField(pf.id(),structural.get(0),"unique mapped field type");
                }
            }
            for(Map.Entry<String,String> binding:new ArrayList<>(methods.entrySet())) {
                if(propagated.contains(binding.getKey()))continue;
                PMethod old=profile.methods.get(binding.getKey());if(old==null)continue;
                Dex250.Method actual=findActualMethod(binding.getValue());
                if(!old.fingerprint.equals(actual.fingerprint()))continue;
                propagated.add(binding.getKey());
                for(Ref ref:old.refs) {
                    if("F".equals(ref.kind)) {
                        Dex250.Field field=actual.fieldAt(ref.pc);
                        if(field==null)throw new IOException("Editor250 field reference drift at "+old.id()+" @"+ref.pc);
                        bindField(ref.target,field,"matched method field edge");
                    } else if("M".equals(ref.kind)) {
                        Dex250.Method method=actual.methodAt(ref.pc);
                        if(method==null)throw new IOException("Editor250 method reference drift at "+old.id()+" @"+ref.pc);
                        bindMethod(ref.target,method,"matched method invoke edge");
                    } else {
                        String type=actual.typeAt(ref.pc);
                        if(type==null)throw new IOException("Editor250 type reference drift at "+old.id()+" @"+ref.pc);
                        unifyDescriptor(ref.target,type,"matched method type edge");
                    }
                }
            }
            bindConstructorFieldRoles();
            if(changes==0)break;
        }
    }

    /** Constructor writes identify roles even when unrelated lazy-lambda constants change.
     * A field observed by another stable consumer additionally needs its complete
     * reference/register skeleton to agree before the role can be published. */
    private void bindConstructorFieldRoles()throws IOException {
        for(PMethod pm:profile.methods.values()) {
            if(!pm.name.equals("<init>")||!methods.containsKey(pm.id()))continue;
            Dex250.Method actual=findActualMethod(methods.get(pm.id()));int[] code=actual.code();
            List<Ref> expected=new ArrayList<>();List<Integer> actualPcs=new ArrayList<>();
            for(Ref ref:pm.refs)if(ref.kind.equals("F")&&ref.opcode>=0x59&&ref.opcode<=0x5f&&ref.target.startsWith(pm.owner+"->"))expected.add(ref);
            for(int pc=0;pc<code.length;){Dex250.Field f=actual.fieldAt(pc);int op=code[pc]&255;if(f!=null&&f.owner.equals(actual.owner)&&op>=0x59&&op<=0x5f)actualPcs.add(pc);pc+=Dex250.width(code,pc);}
            if(expected.isEmpty()||expected.size()!=actualPcs.size())continue;
            boolean same=true;
            for(int i=0;i<expected.size();i++) {
                Ref ref=expected.get(i);int pc=actualPcs.get(i);Dex250.Field field=actual.fieldAt(pc);
                PField old=profile.fields.get(ref.target);
                if(ref.pc!=pc||ref.word!=code[pc]||old==null||!fieldCompatible(old,field)){same=false;break;}
            }
            if(!same)continue;
            for(int i=0;i<expected.size();i++) {
                Ref ref=expected.get(i);if(fields.containsKey(ref.target))continue;
                Dex250.Field field=actual.fieldAt(actualPcs.get(i));boolean hasConsumer=false,confirmed=false;
                for(PMethod consumer:profile.methods.values())if(stable(consumer.owner)&&!consumer.name.startsWith("<")&&methods.containsKey(consumer.id())) {
                    for(Ref use:consumer.refs)if(use.kind.equals("F")&&use.target.equals(ref.target)) {
                        hasConsumer=true;Dex250.Method target=findActualMethod(methods.get(consumer.id()));
                        if(consumer.referenceFingerprint!=null&&consumer.referenceFingerprint.equals(target.referenceFingerprint())) {
                            Dex250.Field check=target.fieldAt(use.pc);
                            if(check!=null&&check.id().equals(field.id()))confirmed=true;
                        }
                    }
                }
                if(!hasConsumer||confirmed)bindField(ref.target,field,confirmed?"constructor role + stable consumer reference graph":"complete constructor field-write roles");
            }
        }
    }

    /** Stable Application lifecycle plus the exact typed sput, independent of utility-class layout. */
    private void bindApplicationContext()throws IOException {
        String app="Lcom/miui/mediaeditor/PhotoShopApp;";
        PMethod old=profile.methods.get(app+"->attachBaseContext(Landroid/content/Context;)V");
        Dex250.Clazz cls=index.classes.get(app);if(old==null||cls==null)return;
        List<Ref> oldFields=new ArrayList<>();
        for(Ref ref:old.refs)if("F".equals(ref.kind)&&ref.opcode==0x69&&ref.target.endsWith(":"+app))oldFields.add(ref);
        List<Dex250.Field> actualFields=new ArrayList<>();
        for(Dex250.Method m:cls.methods)if(m.name.equals("attachBaseContext")&&m.descriptor.equals("(Landroid/content/Context;)V")) {
            int[] code=m.code();for(int pc=0;pc<code.length;){Dex250.Field f=m.fieldAt(pc);if((code[pc]&255)==0x69&&f!=null&&f.type.equals(app)&&!actualFields.contains(f))actualFields.add(f);pc+=Dex250.width(code,pc);}
        }
        if(oldFields.size()==1&&actualFields.size()==1)bindField(oldFields.get(0).target,actualFields.get(0),"Application typed context publication");
    }

    private List<Dex250.Clazz> owners(String old) {
        String actual=types.get(old);
        if(actual!=null){Dex250.Clazz cls=index.classes.get(actual);return cls==null?Collections.emptyList():Collections.singletonList(cls);}
        List<Dex250.Clazz> result=new ArrayList<>();
        for(Dex250.Clazz cls:candidates.get(old))if(typeCompatible(old,cls.name))result.add(cls);
        return result;
    }
    private boolean methodCompatible(PMethod old,Dex250.Method actual) {
        String previous=reverseMethods.get(actual.id());if(previous!=null&&!previous.equals(old.id()))return false;
        if((old.flags&0x109)!=(actual.flags&0x109))return false; // public/static/native are semantic ABI.
        if((old.name.startsWith("<")||old.name.length()>2)&&!old.name.equals(actual.name))return false;
        return descriptorCompatible(old.descriptor,actual.descriptor)&&typeCompatible(old.owner,actual.owner);
    }
    private boolean fieldCompatible(PField old,Dex250.Field actual) {
        String previous=reverseFields.get(actual.id());if(previous!=null&&!previous.equals(old.id()))return false;
        return (old.flags&0x18)==(actual.flags&0x18)&&descriptorCompatible(old.type,actual.type)&&typeCompatible(old.owner,actual.owner);
    }
    private boolean typeCompatible(String old,String actual) {
        String found=types.get(old);if(found!=null)return found.equals(actual);
        String reverse=reverseTypes.get(actual);if(reverse!=null&&!reverse.equals(old))return false;
        return !stable(old)||old.equals(actual);
    }
    private boolean descriptorCompatible(String old,String actual) {
        List<String> a=parts(old),b=parts(actual);if(a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++) {
            String x=a.get(i),y=b.get(i);
            if(x.startsWith("L")&&y.startsWith("L")){if(!typeCompatible(x,y))return false;}
            else if(!x.equals(y))return false;
        }
        return true;
    }
    private void unifyDescriptor(String old,String actual,String reason)throws IOException {
        List<String> a=parts(old),b=parts(actual);
        if(a.size()!=b.size())throw new IOException("Editor250 descriptor arity conflict: "+old+" / "+actual);
        for(int i=0;i<a.size();i++) {
            String x=a.get(i),y=b.get(i);
            if(x.startsWith("L")&&y.startsWith("L")){if(profile.classes.containsKey(x)||stable(x))bindType(x,y,reason);}
            else if(!x.equals(y))throw new IOException("Editor250 descriptor conflict: "+old+" / "+actual);
        }
    }
    private void bindType(String old,String actual,String reason)throws IOException {
        if(!typeCompatible(old,actual))throw new IOException("Editor250 class relation conflict "+old+" -> "+actual+" previous="+types.get(old));
        if(!types.containsKey(old)){types.put(old,actual);reverseTypes.put(actual,old);reasons.put("C:"+old,reason);changes++;}
    }
    private void bindMethod(String old,Dex250.Method actual,String reason)throws IOException {
        int arrow=old.indexOf("->"),paren=old.indexOf('(',arrow);
        String oldOwner=old.substring(0,arrow),oldDescriptor=old.substring(paren);
        bindType(oldOwner,actual.owner,reason);unifyDescriptor(oldDescriptor,actual.descriptor,reason);
        String previous=methods.get(old);
        String reverse=reverseMethods.get(actual.id());
        if(reverse!=null&&!reverse.equals(old))throw new IOException("Editor250 method aliases collide: "+old+" / "+reverse+" -> "+actual.id());
        if(previous!=null&&!previous.equals(actual.id()))throw new IOException("Editor250 method relation conflict "+old+" -> "+actual.id()+" previous="+previous);
        if(previous==null){methods.put(old,actual.id());reverseMethods.put(actual.id(),old);reasons.put("M:"+old,reason);changes++;}
    }
    private void bindField(String old,Dex250.Field actual,String reason)throws IOException {
        int arrow=old.indexOf("->"),colon=old.lastIndexOf(':');
        bindType(old.substring(0,arrow),actual.owner,reason);unifyDescriptor(old.substring(colon+1),actual.type,reason);
        String previous=fields.get(old);
        String reverse=reverseFields.get(actual.id());
        if(reverse!=null&&!reverse.equals(old))throw new IOException("Editor250 field aliases collide: "+old+" / "+reverse+" -> "+actual.id());
        if(previous!=null&&!previous.equals(actual.id()))throw new IOException("Editor250 field relation conflict "+old+" -> "+actual.id()+" previous="+previous);
        if(previous==null){fields.put(old,actual.id());reverseFields.put(actual.id(),old);reasons.put("F:"+old,reason);changes++;}
    }
    private Dex250.Method findActualMethod(String id)throws IOException {
        int arrow=id.indexOf("->");Dex250.Clazz cls=index.classes.get(id.substring(0,arrow));
        if(cls!=null)for(Dex250.Method m:cls.methods)if(m.id().equals(id))return m;
        // Reference-only methods may be platform declarations absent from class_def.
        for(Dex250.Dex dex:index.dexFiles)for(Dex250.Method m:dex.methods)if(m.id().equals(id))return m;
        throw new IOException("Editor250 resolved method not present: "+id);
    }
    private static List<String> parts(String value) {
        List<String> result=new ArrayList<>();
        for(int i=0;i<value.length();) {
            if(value.charAt(i)=='L'){int end=value.indexOf(';',i);if(end<0)throw new IllegalArgumentException("Bad descriptor: "+value);result.add(value.substring(i,end+1));i=end+1;}
            else result.add(value.substring(i,++i));
        }
        return result;
    }
    private static boolean stable(String type){return type.equals(Dex250.typeShape(type));}
    private static String descriptor(String binary){return binary.startsWith("L")&&binary.endsWith(";")?binary:"L"+binary.replace('.','/')+";";}
    private static String binary(String descriptor){return descriptor.substring(1,descriptor.length()-1).replace('/','.');}

    private static Profile readProfile(InputStream in)throws IOException {
        Profile p=new Profile();boolean version=false;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))) {
            String line;while((line=reader.readLine())!=null) {
                if(line.isEmpty()||line.startsWith("#"))continue;String[] row=line.split("\t",-1);
                if("V".equals(row[0])){if(!"1".equals(row[1]))throw new IOException("Unsupported Editor250 profile version");version=true;continue;}
                if("C".equals(row[0])){PClass c=new PClass();c.name=row[1];c.shape=row[2];p.classes.put(c.name,c);}
                else if("M".equals(row[0])){PMethod m=new PMethod();m.owner=row[1];m.name=row[2];m.descriptor=row[3];m.flags=Integer.parseInt(row[4]);m.fingerprint=row[5];m.referenceFingerprint=row.length>6?row[6]:null;p.methods.put(m.id(),m);p.classes.get(m.owner).methods.add(m);}
                else if("F".equals(row[0])){PField f=new PField();f.owner=row[1];f.name=row[2];f.type=row[3];f.flags=Integer.parseInt(row[4]);f.usage=row[5];p.fields.put(f.id(),f);p.classes.get(f.owner).fields.add(f);}
                else if("R".equals(row[0])){Ref ref=new Ref();ref.pc=Integer.parseInt(row[2]);ref.opcode=Integer.parseInt(row[3]);ref.kind=row[4];ref.target=row[5];ref.word=row.length>6?Integer.parseInt(row[6]):-1;p.methods.get(row[1]).refs.add(ref);}
                else throw new IOException("Unknown Editor250 profile row: "+row[0]);
            }
        }
        if(!version||p.classes.isEmpty())throw new IOException("Editor250 profile is empty or lacks version");return p;
    }

    public static void generateProfile(String apk,String typeList,String output)throws IOException {
        Dex250.Index index=Dex250.readApk(apk);
        String contextOwner=null;
        Dex250.Clazz application=index.clazz("Lcom/miui/mediaeditor/PhotoShopApp;");
        if(application!=null)for(Dex250.Method method:application.methods)
            if(method.name.equals("attachBaseContext")&&method.descriptor.equals("(Landroid/content/Context;)V")) {
                int[] code=method.code();for(int pc=0;pc<code.length;) {
                    Dex250.Field field=method.fieldAt(pc);
                    if((code[pc]&255)==0x69&&field!=null&&field.type.equals(application.name))contextOwner=field.owner;
                    pc+=Dex250.width(code,pc);
                }
            }
        try(PrintWriter out=new PrintWriter(output,"UTF-8")) {
            out.println("V\t1");
            List<Dex250.Clazz> selected=new ArrayList<>();
            Set<String> selectedNames=new LinkedHashSet<>();
            for(String line:Files.readAllLines(Paths.get(typeList),StandardCharsets.UTF_8)) {
                line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;
                Dex250.Clazz cls=index.classes.get(descriptor(line));
                if(cls==null)throw new IOException("Profile class missing: "+line);if(!selectedNames.add(cls.name))continue;selected.add(cls);out.println("C\t"+cls.name+"\t"+cls.shape());
            }
            for(Dex250.Clazz cls:selected) {
                // R8 split this former utility owner across unrelated new holders.
                // Only its typed application-context publication belongs to our contract.
                boolean contextHolder=cls.name.equals(contextOwner);
                for(Dex250.Field f:cls.fields)if(!contextHolder||f.type.equals("Lcom/miui/mediaeditor/PhotoShopApp;"))out.println("F\t"+f.owner+"\t"+f.name+"\t"+f.type+"\t"+f.flags+"\t"+f.usageFingerprint());
                if(contextHolder)continue;
                for(Dex250.Method m:cls.methods) {
                    out.println("M\t"+m.owner+"\t"+m.name+"\t"+m.descriptor+"\t"+m.flags+"\t"+m.fingerprint()+"\t"+m.referenceFingerprint());
                    int[] code=m.code();for(int pc=0;pc<code.length;) {
                        String kind=null,target=null;Dex250.Field f=m.fieldAt(pc);Dex250.Method called=m.methodAt(pc);String type=m.typeAt(pc);
                        if(f!=null&&selectedNames.contains(f.owner)){kind="F";target=f.id();}
                        else if(called!=null&&selectedNames.contains(called.owner)&&!called.owner.equals(contextOwner)){kind="M";target=called.id();}
                        else if(type!=null&&selectedNames.contains(type)){kind="T";target=type;}
                        if(kind!=null)out.println("R\t"+m.id()+"\t"+pc+"\t"+(code[pc]&255)+"\t"+kind+"\t"+target+"\t"+code[pc]);
                        pc+=Dex250.width(code,pc);
                    }
                }
            }
        }
    }
    public void writeReport(String path)throws IOException {
        try(PrintWriter out=new PrintWriter(path,"UTF-8")) {
            for(Map.Entry<String,String> e:types.entrySet())out.println("C\t"+e.getKey()+"\t"+e.getValue()+"\t"+reasons.get("C:"+e.getKey()));
            for(Map.Entry<String,String> e:methods.entrySet())out.println("M\t"+e.getKey()+"\t"+e.getValue()+"\t"+reasons.get("M:"+e.getKey()));
            for(Map.Entry<String,String> e:fields.entrySet())out.println("F\t"+e.getKey()+"\t"+e.getValue()+"\t"+reasons.get("F:"+e.getKey()));
            for(PClass c:profile.classes.values())if(!types.containsKey(c.name))out.println("UNRESOLVED_C\t"+c.name);
            for(PMethod m:profile.methods.values())if(!methods.containsKey(m.id()))out.println("UNRESOLVED_M\t"+m.id());
            for(PField f:profile.fields.values())if(!fields.containsKey(f.id()))out.println("UNRESOLVED_F\t"+f.id());
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length==4&&args[0].equals("profile")){generateProfile(args[1],args[2],args[3]);System.out.println("Editor250 profile generated");return;}
        if((args.length==4||args.length==5)&&args[0].equals("resolve")) {
            EditorAbi250 abi;try(InputStream in=new FileInputStream(args[2])){
                if(args.length==5)try(InputStream aliases=new FileInputStream(args[4])){abi=resolve(args[1],in,aliases);}
                else abi=resolve(args[1],in);
            }abi.writeReport(args[3]);
            System.out.println("Editor250 resolved classes="+abi.types.size()+" methods="+abi.methods.size()+" fields="+abi.fields.size());return;
        }
        throw new IllegalArgumentException("EditorAbi250 profile <old.apk> <types.txt> <contract.tsv> | resolve <target.apk> <contract.tsv> <report.tsv>");
    }
}
