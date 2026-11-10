package com.aurora.camera.compat.editor.abi250;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Read-only DEX metadata and name-independent instruction fingerprints.
 *
 * A fingerprint is static evidence for a resolver, never permission to invoke a
 * candidate. Callers must require a unique match and verify the resulting ABI.
 */
public final class Dex250 {
    private Dex250() {}

    public static final class Index {
        public final Map<String, Clazz> classes = new LinkedHashMap<>();
        public final List<Dex> dexFiles = new ArrayList<>();
        public Clazz clazz(String descriptor) { return classes.get(descriptor); }
    }

    public static final class Clazz {
        public final String name, superType, dexName;
        public final int flags;
        public final List<String> interfaces;
        public final List<Field> fields = new ArrayList<>();
        public final List<Method> methods = new ArrayList<>();
        Clazz(String name, String superType, int flags, List<String> interfaces, String dexName) {
            this.name=name; this.superType=superType; this.flags=flags;
            this.interfaces=interfaces; this.dexName=dexName;
        }
        /** Complete declared ABI shape. Member ordering and obfuscated names are excluded. */
        public String shape() {
            List<String> rows=new ArrayList<>();
            for (String i:interfaces) rows.add("I:"+typeShape(i));
            for (Field f:fields) rows.add("F:"+f.flags+":"+typeShape(f.type));
            for (Method m:methods) rows.add("M:"+m.flags+":"+methodNameShape(m.name)+":"+prototypeShape(m.descriptor));
            Collections.sort(rows);
            return hash("C:"+flags+":"+typeShape(superType)+"\n"+join(rows));
        }
    }

    public static final class Field {
        public final String owner, name, type;
        public int flags;
        public Clazz declaringClass;
        Field(String owner,String name,String type) { this.owner=owner;this.name=name;this.type=type; }
        public String id() { return owner+"->"+name+":"+type; }
        /** Uses within the declaring class, anchored to complete method fingerprints. */
        public String usageFingerprint() throws IOException {
            List<String> uses=new ArrayList<>();
            if (declaringClass!=null) for (Method m:declaringClass.methods) {
                if (m.codeOffset==0) continue;
                int[] code=m.code();
                for(int pc=0;pc<code.length;) {
                    int op=code[pc]&255,w=width(code,pc);
                    if(op>=0x52&&op<=0x6d && m.dex.fields[code[pc+1]]==this)
                        uses.add(m.fingerprint()+":"+pc+":"+code[pc]);
                    pc+=w;
                }
            }
            Collections.sort(uses);
            return hash(flags+":"+typeShape(type)+"\n"+join(uses));
        }
    }

    public static final class Method {
        public final String owner,name,descriptor;
        public int flags,codeOffset;
        public Clazz declaringClass;
        private final Dex dex;
        private String fingerprint;
        Method(Dex dex,String owner,String name,String descriptor) {
            this.dex=dex;this.owner=owner;this.name=name;this.descriptor=descriptor;
        }
        public String id() { return owner+"->"+name+descriptor; }
        private int codeUnit(int pc)throws IOException {
            if(codeOffset==0||pc<0||pc>=dex.i32(codeOffset+12))throw new IOException("DEX instruction PC outside method: "+id()+" @"+pc);
            return dex.u16(codeOffset+16+2*pc);
        }
        /** Exact field-table target at an iget/iput/sget/sput instruction, or null. */
        public Field fieldAt(int pc)throws IOException {
            int op=codeUnit(pc)&255;
            return op>=0x52&&op<=0x6d?dex.fields[codeUnit(pc+1)]:null;
        }
        /** Exact method-table target at an invoke instruction, or null for non-invokes/custom sites. */
        public Method methodAt(int pc)throws IOException {
            int op=codeUnit(pc)&255;
            return (op>=0x6e&&op<=0x72)||(op>=0x74&&op<=0x78)||op==0xfa||op==0xfb?dex.methods[codeUnit(pc+1)]:null;
        }
        /** Exact descriptor at a const-class/check-cast/instance-of/new/filled-new-array instruction. */
        public String typeAt(int pc)throws IOException {
            int op=codeUnit(pc)&255;
            return op==0x1c||op==0x1f||op==0x20||op==0x22||op==0x23||op==0x24||op==0x25?dex.types[codeUnit(pc+1)]:null;
        }
        /** Exact MUTF8 string constant, or null when this instruction has no string operand. */
        public String stringAt(int pc)throws IOException {
            int op=codeUnit(pc)&255;
            return op==0x1a?dex.strings[codeUnit(pc+1)]:op==0x1b?dex.strings[codeUnit(pc+1)|(codeUnit(pc+2)<<16)]:null;
        }
        public int[] code() throws IOException {
            if(codeOffset==0)return new int[0];
            int count=dex.i32(codeOffset+12);
            dex.range(codeOffset+16,((long)count)*2);
            int[] code=new int[count];
            for(int i=0;i<count;i++)code[i]=dex.u16(codeOffset+16+2*i);
            return code;
        }
        public List<String> strings() throws IOException {
            List<String> result=new ArrayList<>();int[] code=code();
            for(int pc=0;pc<code.length;) {
                int op=code[pc]&255;
                if(op==0x1a)result.add(dex.strings[code[pc+1]]);
                else if(op==0x1b)result.add(dex.strings[code[pc+1]|code[pc+2]<<16]);
                pc+=width(code,pc);
            }
            return result;
        }
        public String fingerprint() throws IOException {
            if(fingerprint!=null)return fingerprint;
            StringBuilder out=new StringBuilder();
            out.append(flags).append(':').append(methodNameShape(name)).append(':').append(prototypeShape(descriptor)).append('\n');
            if(codeOffset!=0) {
                out.append(dex.u16(codeOffset)).append(':').append(dex.u16(codeOffset+2)).append(':').append(dex.u16(codeOffset+4)).append('\n');
                int[] code=code();
                for(int pc=0;pc<code.length;) {
                    int w=width(code,pc),op=code[pc]&255;
                    int[] words=new int[w];System.arraycopy(code,pc,words,0,w);
                    String ref=null;
                    if(op==0x1a||op==0x1b) {
                        int index=code[pc+1]|(op==0x1b?code[pc+2]<<16:0);
                        ref="S:"+dex.literalShape(dex.strings[index]);words[1]=0;if(op==0x1b)words[2]=0;
                    } else if(op==0x1c||op==0x1f||op==0x20||op==0x22||op==0x23||op==0x24||op==0x25) {
                        ref="T:"+typeShape(dex.types[code[pc+1]]);words[1]=0;
                    } else if(op>=0x52&&op<=0x6d) {
                        Field f=dex.fields[code[pc+1]];ref="F:"+typeShape(f.owner)+":"+typeShape(f.type)
                                +":"+(f.owner.equals(typeShape(f.owner))&&f.name.length()>2?f.name:"?");words[1]=0;
                    } else if((op>=0x6e&&op<=0x72)||(op>=0x74&&op<=0x78)||op==0xfa||op==0xfb) {
                        Method m=dex.methods[code[pc+1]];
                        ref=dex.stringDecoders.containsKey(m.id())?"STRING_DECODER":"M:"+typeShape(m.owner)+":"+methodNameShape(m.name)+":"+prototypeShape(m.descriptor);words[1]=0;
                        if(op==0xfa||op==0xfb){ref+="|P:"+prototypeShape(dex.protos[code[pc+3]]);words[3]=0;}
                    } else if(op==0xff) {ref="P:"+prototypeShape(dex.protos[code[pc+1]]);words[1]=0;
                    } else if(op==0xfe) {ref="H:"+dex.methodHandleShape(code[pc+1]);words[1]=0;
                    } else if(op==0xfc||op==0xfd) {ref="C:"+dex.callSiteShape(code[pc+1]);words[1]=0;}
                    // Resource table entry numbers may move independently of the code.
                    if(op==0x14||op==0x17)normalizeResource(words,1);
                    if(op==0x15&&(words[1]&0xff00)==0x7f00)words[1]=0x7f00;
                    if(op==0&&words[0]==0x0200)for(int i=2;i<2+words[1]*2;i+=2)normalizeResource(words,i);
                    if(op==0&&words[0]==0x0100)normalizeResource(words,2);
                    for(int word:words)out.append(Integer.toHexString(word)).append(',');
                    if(ref!=null)out.append(ref.length()).append(':').append(ref);
                    out.append('\n');pc+=w;
                }
            }
            fingerprint=hash(out.toString());return fingerprint;
        }
        /** Complete reference/register graph, retaining PCs but excluding literal-only instructions. */
        public String referenceFingerprint()throws IOException {
            StringBuilder out=new StringBuilder();int[] code=code();
            for(int pc=0;pc<code.length;) {
                int op=code[pc]&255,w=width(code,pc);String ref=null;
                Field f=fieldAt(pc);Method m=methodAt(pc);String type=typeAt(pc),string=stringAt(pc);
                if(f!=null)ref="F:"+typeShape(f.owner)+":"+typeShape(f.type)+":"+(f.owner.equals(typeShape(f.owner))&&f.name.length()>2?f.name:"?");
                else if(m!=null)ref=dex.stringDecoders.containsKey(m.id())?"STRING_DECODER":"M:"+typeShape(m.owner)+":"+methodNameShape(m.name)+":"+prototypeShape(m.descriptor);
                else if(type!=null)ref="T:"+typeShape(type);
                else if(string!=null)ref="S:"+dex.literalShape(string);
                if(ref!=null){out.append(pc).append(':').append(code[pc]).append(':').append(ref);if(m!=null&&w>=3)out.append(':').append(code[pc+2]);out.append('\n');}
                pc+=w;
            }
            return hash(out.toString());
        }
    }

    /** Model for one classes*.dex. Exposes original table identities for focused graph analysis. */
    public static final class Dex {
        public final String name;
        public final byte[] bytes;
        public String[] strings,types,protos;
        public Field[] fields;
        public Method[] methods;
        public final Map<String,Integer> stringDecoders=new LinkedHashMap<>();
        private int methodHandlesOffset,callSitesOffset;
        Dex(String name,byte[] bytes) {this.name=name;this.bytes=bytes;}
        String literalShape(String value) {
            if(!value.isEmpty())for(int key:stringDecoders.values()) {
                int ascii=0;for(int i=0;i<value.length();i++)if((value.charAt(i)^key)<128)ascii++;
                if(ascii>value.length()*0.9) {StringBuilder out=new StringBuilder();for(int i=0;i<value.length();i++)out.append((char)(value.charAt(i)^key));return out.toString();}
            }
            return value;
        }
        void range(int off,long length)throws IOException {
            if(off<0||length<0||off+length>bytes.length)throw new IOException("DEX range outside "+name+": "+off+"+"+length);
        }
        public int u16(int off)throws IOException {range(off,2);return(bytes[off]&255)|((bytes[off+1]&255)<<8);}
        public int i32(int off)throws IOException {range(off,4);return(bytes[off]&255)|((bytes[off+1]&255)<<8)|((bytes[off+2]&255)<<16)|(bytes[off+3]<<24);}
        int uleb(Cursor p)throws IOException {
            int value=0;for(int shift=0;shift<35;shift+=7){range(p.off,1);int b=bytes[p.off++]&255;value|=(b&127)<<shift;if(b<128)return value;}
            throw new IOException("DEX ULEB128 too long");
        }
        String mutf8(int off)throws IOException {
            Cursor p=new Cursor(off);int count=uleb(p);char[] text=new char[count];
            for(int i=0;i<count;i++) {
                range(p.off,1);int b=bytes[p.off++]&255;
                if(b==0)throw new IOException("Short DEX string");
                if(b<128)text[i]=(char)b;
                else if((b&0xe0)==0xc0){range(p.off,1);int c=bytes[p.off++]&255;if((c&0xc0)!=0x80)throw new IOException("Bad DEX MUTF8");text[i]=(char)(((b&31)<<6)|(c&63));}
                else if((b&0xf0)==0xe0){range(p.off,2);int c=bytes[p.off++]&255,d=bytes[p.off++]&255;if((c&0xc0)!=0x80||(d&0xc0)!=0x80)throw new IOException("Bad DEX MUTF8");text[i]=(char)(((b&15)<<12)|((c&63)<<6)|(d&63));}
                else throw new IOException("Bad DEX MUTF8 lead");
            }
            range(p.off,1);if(bytes[p.off]!=0)throw new IOException("Long DEX string");return new String(text);
        }
        String methodHandleShape(int index)throws IOException {
            if(methodHandlesOffset==0)throw new IOException("DEX method handle table missing");
            int at=methodHandlesOffset+index*8,kind=u16(at),target=u16(at+4);
            if(kind<=3){Field f=fields[target];return kind+":"+typeShape(f.owner)+":"+typeShape(f.type);}
            Method m=methods[target];return kind+":"+typeShape(m.owner)+":"+methodNameShape(m.name)+":"+prototypeShape(m.descriptor);
        }
        String callSiteShape(int index)throws IOException {
            if(callSitesOffset==0)throw new IOException("DEX call site table missing");
            Cursor p=new Cursor(i32(callSitesOffset+index*4));int count=uleb(p);StringBuilder out=new StringBuilder();
            for(int i=0;i<count;i++)out.append(encodedValue(p)).append('|');return out.toString();
        }
        String encodedValue(Cursor p)throws IOException {
            range(p.off,1);int head=bytes[p.off++]&255,type=head&31,arg=head>>>5;
            if(type==0x1e)return "null";if(type==0x1f)return "bool:"+arg;
            if(type==0x1c){int count=uleb(p);StringBuilder b=new StringBuilder("[");for(int i=0;i<count;i++)b.append(encodedValue(p)).append('|');return b.append(']').toString();}
            if(type==0x1d)throw new IOException("DEX annotation in call site unsupported");
            long v=0;range(p.off,arg+1);for(int i=0;i<=arg;i++)v|=((long)bytes[p.off++]&255)<<(i*8);
            int id=(int)v;
            if(type==0x15)return "proto:"+prototypeShape(protos[id]);
            if(type==0x16)return "handle:"+methodHandleShape(id);
            if(type==0x17)return "string:"+strings[id];
            if(type==0x18)return "type:"+typeShape(types[id]);
            if(type==0x19||type==0x1b){Field f=fields[id];return "field:"+typeShape(f.owner)+":"+typeShape(f.type);}
            if(type==0x1a){Method m=methods[id];return "method:"+typeShape(m.owner)+":"+methodNameShape(m.name)+":"+prototypeShape(m.descriptor);}
            return type+":"+v;
        }
    }

    private static final class Cursor {int off;Cursor(int off){this.off=off;}}

    public static Index readApk(Path path)throws IOException {return readApk(path.toString());}
    public static Index readApk(String path)throws IOException {
        Index result=new Index();
        try(ZipFile apk=new ZipFile(new File(path))) {
            List<ZipEntry> entries=new ArrayList<>();Enumeration<? extends ZipEntry> e=apk.entries();
            while(e.hasMoreElements()){ZipEntry item=e.nextElement();if(item.getName().matches("classes(?:[2-9]|[1-9][0-9]+)?\\.dex"))entries.add(item);}
            entries.sort(Comparator.comparing(ZipEntry::getName));
            if(entries.isEmpty())throw new IOException("APK contains no classes DEX");
            for(ZipEntry entry:entries) {
                byte[] bytes;try(InputStream in=apk.getInputStream(entry);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1)out.write(block,0,n);bytes=out.toByteArray();
                }
                Dex dex=new Dex(entry.getName(),bytes);readDex(dex,result);result.dexFiles.add(dex);
            }
        }
        discoverStringDecoders(result);
        return result;
    }

    /** Identify the actually shipped UTF16 XOR loop, rather than shipping a build key. */
    private static void discoverStringDecoders(Index index)throws IOException {
        Map<String,Integer> found=new LinkedHashMap<>();
        for(Clazz cls:index.classes.values())for(Method m:cls.methods) {
            if(m.codeOffset==0||(m.flags&8)==0||!m.descriptor.equals("(Ljava/lang/String;)Ljava/lang/String;"))continue;
            boolean chars=false,value=false,narrow=false,xor=false;List<Integer> constants=new ArrayList<>();int[] code=m.code();
            for(int pc=0;pc<code.length;) {
                int op=code[pc]&255;Method called=m.methodAt(pc);
                if(called!=null&&called.owner.equals("Ljava/lang/String;")) {
                    if(called.name.equals("toCharArray")&&called.descriptor.equals("()[C"))chars=true;
                    if(called.name.equals("valueOf")&&called.descriptor.equals("([C)Ljava/lang/String;"))value=true;
                }
                if(op==0x8e)narrow=true;if(op==0x97||op==0xb7)xor=true;
                if(op==0x14)constants.add(code[pc+1]|code[pc+2]<<16);
                pc+=width(code,pc);
            }
            if(chars&&value&&narrow&&xor&&constants.size()==1)found.put(m.id(),constants.get(0)&0xffff);
        }
        for(Dex dex:index.dexFiles)dex.stringDecoders.putAll(found);
    }

    private static void readDex(Dex d,Index index)throws IOException {
        d.range(0,112);
        if(d.bytes[0]!='d'||d.bytes[1]!='e'||d.bytes[2]!='x'||d.bytes[3]!='\n'||d.bytes[7]!=0)
            throw new IOException("Not a standard DEX: "+d.name);
        if(d.i32(32)!=d.bytes.length||d.i32(36)!=112||d.i32(40)!=0x12345678)
            throw new IOException("Unsupported DEX layout: "+d.name);
        d.strings=new String[d.i32(56)];int off=d.i32(60);
        for(int i=0;i<d.strings.length;i++)d.strings[i]=d.mutf8(d.i32(off+4*i));
        d.types=new String[d.i32(64)];off=d.i32(68);
        for(int i=0;i<d.types.length;i++)d.types[i]=d.strings[d.i32(off+4*i)];
        d.protos=new String[d.i32(72)];off=d.i32(76);
        for(int i=0;i<d.protos.length;i++) {
            int at=off+12*i,list=d.i32(at+8);StringBuilder proto=new StringBuilder("(");
            if(list!=0)for(int j=0,n=d.i32(list);j<n;j++)proto.append(d.types[d.u16(list+4+2*j)]);
            d.protos[i]=proto.append(')').append(d.types[d.i32(at+4)]).toString();
        }
        d.fields=new Field[d.i32(80)];off=d.i32(84);
        for(int i=0;i<d.fields.length;i++){int at=off+8*i;d.fields[i]=new Field(d.types[d.u16(at)],d.strings[d.i32(at+4)],d.types[d.u16(at+2)]);}
        d.methods=new Method[d.i32(88)];off=d.i32(92);
        for(int i=0;i<d.methods.length;i++){int at=off+8*i;d.methods[i]=new Method(d,d.types[d.u16(at)],d.strings[d.i32(at+4)],d.protos[d.u16(at+2)]);}
        int map=d.i32(52);
        if(map!=0)for(int i=0,n=d.i32(map);i<n;i++){int at=map+4+12*i,type=d.u16(at);if(type==7)d.callSitesOffset=d.i32(at+8);else if(type==8)d.methodHandlesOffset=d.i32(at+8);}
        off=d.i32(100);
        for(int i=0,n=d.i32(96);i<n;i++) {
            int at=off+32*i,superIndex=d.i32(at+8),list=d.i32(at+12);
            List<String> interfaces=new ArrayList<>();if(list!=0)for(int j=0,k=d.i32(list);j<k;j++)interfaces.add(d.types[d.u16(list+4+2*j)]);
            Clazz cls=new Clazz(d.types[d.i32(at)],superIndex==-1?null:d.types[superIndex],d.i32(at+4),interfaces,d.name);
            if(index.classes.put(cls.name,cls)!=null)throw new IOException("Duplicate DEX class: "+cls.name);
            int data=d.i32(at+24);if(data==0)continue;Cursor p=new Cursor(data);int[] counts=new int[4];for(int j=0;j<4;j++)counts[j]=d.uleb(p);
            for(int group=0;group<4;group++){int id=0;for(int j=0;j<counts[group];j++) {
                id+=d.uleb(p);int flags=d.uleb(p);
                if(group<2){Field f=d.fields[id];f.flags=flags;f.declaringClass=cls;cls.fields.add(f);}
                else {Method m=d.methods[id];m.flags=flags;m.codeOffset=d.uleb(p);m.declaringClass=cls;cls.methods.add(m);}
            }}
        }
    }

    /** Normalize only compact root packages (the observed R8 namespace), preserving stable namespaces. */
    public static String typeShape(String descriptor) {
        if(descriptor==null)return "-";StringBuilder out=new StringBuilder();
        for(int i=0;i<descriptor.length();) {
            char c=descriptor.charAt(i);
            if(c!='L'){out.append(c);i++;continue;}
            int end=descriptor.indexOf(';',i);if(end<0)throw new IllegalArgumentException("Bad DEX descriptor: "+descriptor);
            String type=descriptor.substring(i,end+1),body=type.substring(1,type.length()-1);
            int slash=body.indexOf('/');boolean compact=slash>0&&slash<=3&&body.indexOf('/',slash+1)<0;
            // R8 also renames AndroidX/miuix declarations inside their retained package.
            String leaf=body.substring(Math.max(body.lastIndexOf('/'),body.lastIndexOf('$'))+1);
            if((body.startsWith("androidx/")||body.startsWith("miuix/"))&&leaf.length()<=2)compact=true;
            out.append(compact?"L?;":type);i=end+1;
        }
        return out.toString();
    }
    public static String prototypeShape(String descriptor) {return typeShape(descriptor);}
    /** R8's observed build-wide UTF16 XOR strings retain their character deltas.
     * Plain strings remain available verbatim through strings()/stringAt(). */
    public static String stringShape(String value) {
        if(value.length()<2)return value;
        int high=value.charAt(0)>>>8;
        if(high<0x80)return value;
        for(int i=1;i<value.length();i++)if((value.charAt(i)>>>8)!=high)return value;
        StringBuilder out=new StringBuilder("xor:");int first=value.charAt(0);
        for(int i=0;i<value.length();i++)out.append(Integer.toHexString(value.charAt(i)^first)).append(',');
        return out.toString();
    }
    private static String methodNameShape(String name) {return name.startsWith("<")||name.length()>2?name:"?";}
    private static void normalizeResource(int[] words,int at) {if(((words[at]|words[at+1]<<16)&0xff000000)==0x7f000000){words[at]=0;words[at+1]=0x7f00;}}
    public static int width(int[] code,int pc)throws IOException {
        int word=code[pc],op=word&255,w;
        if(op==0&&(word>>>8)!=0) {
            int kind=word>>>8;if(pc+1>=code.length)throw new IOException("Truncated DEX payload");
            if(kind==1)w=4+code[pc+1]*2;
            else if(kind==2)w=2+code[pc+1]*4;
            else if(kind==3) {if(pc+3>=code.length)throw new IOException("Truncated DEX array payload");long bytes=(long)code[pc+1]*((code[pc+2]&0xffffL)|((long)code[pc+3]<<16));if(bytes>Integer.MAX_VALUE)throw new IOException("DEX array payload too large");w=4+(int)((bytes+1)/2);}
            else throw new IOException("Unknown DEX payload: "+word);
        } else if(op==3||op==6||op==9||op==20||op==23||op==27||op==36||op==37||op==38||op==42||op==43||op==44||(op>=0x6e&&op<=0x72)||(op>=0x74&&op<=0x78)||op==0xfc||op==0xfd)w=3;
        else if(op==24)w=5;
        else if(op==0xfa||op==0xfb)w=4;
        else if(op==2||op==5||op==8||op==19||op==21||op==22||op==25||op==26||op==28||op==31||op==32||op==34||op==35||op==41||op==0xfe||op==0xff||(op>=0x2d&&op<=0x3d)||(op>=0x44&&op<=0x6d)||(op>=0x90&&op<=0xaf)||(op>=0xd0&&op<=0xe2))w=2;
        else w=1;
        if(w<=0||pc+w>code.length)throw new IOException("Truncated DEX instruction at "+pc);return w;
    }
    private static String join(List<String> rows){StringBuilder out=new StringBuilder();for(String row:rows)out.append(row).append('\n');return out.toString();}
    public static String hash(String value) {
        try{MessageDigest digest=MessageDigest.getInstance("SHA-256");for(int i=0;i<value.length();i++){int unit=value.charAt(i);digest.update((byte)unit);digest.update((byte)(unit>>>8));}byte[] bytes=digest.digest();StringBuilder out=new StringBuilder(64);for(byte b:bytes){out.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));}return out.toString();}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }

    /** Static inventory for the desktop verifier; this never loads target classes. */
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Dex250 <input.apk> <output.tsv>");
        Index index=readApk(args[0]);int methods=0,fields=0;
        try(PrintWriter out=new PrintWriter(args[1],"UTF-8")) {
            for(Clazz c:index.classes.values()) {
                out.println("C\t"+c.name+"\t"+c.shape());
                for(Method m:c.methods){out.println("M\t"+m.id()+"\t"+m.fingerprint());methods++;}
                for(Field f:c.fields){out.println("F\t"+f.id()+"\t"+f.usageFingerprint());fields++;}
            }
        }
        System.out.println("DEX="+index.dexFiles.size()+" classes="+index.classes.size()+" methods="+methods+" fields="+fields);
    }
}
