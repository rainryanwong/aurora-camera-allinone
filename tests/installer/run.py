"""Run production LSP setup logic on an isolated JVM SQL/AtomicFile boundary.

The fixture refuses openDatabase(), so this cannot open a live LSPosed DB.
Gson comes from the already installed JADX tooling; nothing is downloaded.
"""
from pathlib import Path
import hashlib
import json
import subprocess

HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
JAVA=Path('D:/DevTools/jdks/jdk-21.0.12.1+1/bin')
GSON=Path('D:/DevTools/rea/engines/jadx-headless-mcp-0.7.1-all.jar')
FIX={
 'org/json/Codec.java':'''package org.json;import java.util.*;import com.google.gson.*;class Codec {
static Object from(JsonElement e){if(e.isJsonObject()){JSONObject o=new JSONObject();for(Map.Entry<String,JsonElement>x:e.getAsJsonObject().entrySet())o.put(x.getKey(),from(x.getValue()));return o;}
if(e.isJsonArray()){JSONArray a=new JSONArray();for(JsonElement x:e.getAsJsonArray())a.put(from(x));return a;}
if(e.isJsonNull())return null;JsonPrimitive p=e.getAsJsonPrimitive();return p.isBoolean()?p.getAsBoolean():p.isNumber()?p.getAsNumber():p.getAsString();}
static Object plain(Object o){if(o instanceof JSONObject){Map<String,Object> m=new LinkedHashMap<>();for(Map.Entry<String,Object> x:((JSONObject)o).values.entrySet())m.put(x.getKey(),plain(x.getValue()));return m;}
if(o instanceof JSONArray){List<Object>a=new ArrayList<>();for(Object x:((JSONArray)o).values)a.add(plain(x));return a;}return o;}
static String text(Object o){return new Gson().toJson(plain(o));}}''',
 'org/json/JSONObject.java':'''package org.json;import java.util.*;public class JSONObject {
final Map<String,Object> values=new LinkedHashMap<>();public JSONObject(){}public JSONObject(String s){values.putAll(((JSONObject)Codec.from(com.google.gson.JsonParser.parseString(s))).values);}
public JSONObject put(String k,Object v){values.put(k,v);return this;}public boolean has(String k){return values.containsKey(k);}
Object required(String k){if(!has(k))throw new IllegalStateException("missing JSON key "+k);return values.get(k);}
public int getInt(String k){return ((Number)required(k)).intValue();}public int optInt(String k,int d){return has(k)?getInt(k):d;}
public boolean getBoolean(String k){return (Boolean)required(k);}public JSONObject getJSONObject(String k){return (JSONObject)required(k);}
public JSONArray getJSONArray(String k){return (JSONArray)required(k);}public String toString(){return Codec.text(this);}public String toString(int n){return toString();}}''',
 'org/json/JSONArray.java':'''package org.json;import java.util.*;public class JSONArray {final List<Object> values=new ArrayList<>();
public JSONArray(){}public JSONArray(Collection<?> c){values.addAll(c);}public JSONArray put(Object v){values.add(v);return this;}
public int length(){return values.size();}public String getString(int i){return (String)values.get(i);}}''',
 'android/database/Cursor.java':'''package android.database;import java.util.*;public class Cursor implements AutoCloseable {
final List<Object[]> rows;int index=-1;public Cursor(List<Object[]> r){rows=r;}public boolean moveToNext(){return ++index<rows.size();}
public boolean moveToFirst(){index=0;return !rows.isEmpty();}public String getString(int n){return String.valueOf(rows.get(index)[n]);}
public int getInt(int n){return ((Number)rows.get(index)[n]).intValue();}public boolean isNull(int n){return rows.get(index)[n]==null;}public void close(){}}''',
 'android/database/sqlite/SQLiteDatabase.java':'''package android.database.sqlite;
import java.util.*;import android.database.Cursor;public class SQLiteDatabase implements AutoCloseable {
public static final int OPEN_READONLY=1,OPEN_READWRITE=0;public int version=101,writes,schemaReads;public boolean failScope;
public Map<String,String[]> tableColumns=new LinkedHashMap<>(),tableKeys=new LinkedHashMap<>();
public boolean extraRequired;public String extraDefault;
public SQLiteDatabase(){tableColumns.put("modules",new String[]{"module_pkg_name","apk_path"});tableColumns.put("modules_state",new String[]{"module_pkg_name","user_id","enabled","scope_request_blocked"});tableColumns.put("scope",new String[]{"module_pkg_name","app_pkg_name","user_id"});
tableKeys.put("modules",new String[]{"module_pkg_name"});tableKeys.put("modules_state",new String[]{"module_pkg_name","user_id"});tableKeys.put("scope",new String[]{"module_pkg_name","app_pkg_name","user_id"});}
public Map<String,String> modules=new LinkedHashMap<>();public Map<String,int[]> states=new LinkedHashMap<>();
public Map<String,TreeSet<String>> scopes=new LinkedHashMap<>();Map<String,String> pm;Map<String,int[]>ps;Map<String,TreeSet<String>>pc;boolean success;
public static SQLiteDatabase openDatabase(String p,Object f,int n){throw new AssertionError("live database must never be opened by test");}
public int getVersion(){return version;}public void setVersion(int v){version=v;}
public void beginTransaction(){pm=new LinkedHashMap<>(modules);ps=new LinkedHashMap<>();for(String k:states.keySet())ps.put(k,states.get(k).clone());pc=new LinkedHashMap<>();for(String k:scopes.keySet())pc.put(k,new TreeSet<>(scopes.get(k)));success=false;}
public void setTransactionSuccessful(){success=true;}public void endTransaction(){if(!success){modules=pm;states=ps;scopes=pc;}pm=null;ps=null;pc=null;}
String key(Object pkg,int user){return pkg+"/"+user;}
public Cursor rawQuery(String sql,String[] args){List<Object[]> r=new ArrayList<>();
if(sql.startsWith("PRAGMA table_info(")){schemaReads++;String table=sql.substring(18,sql.length()-1);String[] columns=tableColumns.get(table);List<String> keys=Arrays.asList(tableKeys.get(table));if(columns!=null)for(int i=0;i<columns.length;i++)r.add(new Object[]{i,columns[i],"",columns[i].equals("extra")&&extraRequired?1:0,columns[i].equals("extra")?extraDefault:null,keys.indexOf(columns[i])+1});}
else if(sql.startsWith("SELECT enabled,scope_request_blocked")){int[] state=states.get(key(args[0],0));if(state!=null)r.add(new Object[]{state[0],state[1]});}
else if(sql.startsWith("SELECT app_pkg_name")){TreeSet<String> s=scopes.get(key(args[0],0));if(s!=null)for(String v:s)r.add(new Object[]{v});}
else throw new AssertionError("unexpected query "+sql);return new Cursor(r);}
public int delete(String table,String where,String[] args){writes++;String k=key(args[0],0);if(table.equals("scope"))scopes.remove(k);else if(table.equals("modules_state"))states.remove(k);else throw new AssertionError(table);return 1;}
public void execSQL(String sql,Object[] args){writes++;
if(sql.startsWith("INSERT OR IGNORE INTO modules("))modules.putIfAbsent((String)args[0],(String)args[1]);
else if(sql.startsWith("UPDATE modules SET apk_path"))modules.put((String)args[1],(String)args[0]);
else if(sql.startsWith("INSERT OR IGNORE INTO modules_state"))states.putIfAbsent(key(args[0],0),new int[]{1,0});
else if(sql.startsWith("UPDATE modules_state SET enabled=1"))states.get(key(args[0],0))[0]=1;
else if(sql.startsWith("UPDATE modules_state SET enabled=?"))states.get(key(args[1],0))[0]=((Number)args[0]).intValue();
else if(sql.startsWith("INSERT INTO scope")){if(failScope)throw new IllegalStateException("injected SQL scope failure");scopes.computeIfAbsent(key(args[0],0),x->new TreeSet<>()).add((String)args[1]);}
else throw new AssertionError("unexpected statement "+sql);}
public void close(){}}''',
 'android/util/AtomicFile.java':'''package android.util;import java.io.*;import java.nio.file.*;
public class AtomicFile {final File base,temp;public static boolean failNextWrite;public AtomicFile(File b){base=b;temp=new File(b+".new");}
public byte[] readFully()throws IOException{return Files.readAllBytes(base.toPath());}
public FileOutputStream startWrite()throws IOException{if(!failNextWrite)return new FileOutputStream(temp);failNextWrite=false;return new FileOutputStream(temp){public void write(byte[] b)throws IOException{throw new IOException("injected backup write failure");}};}
public void finishWrite(FileOutputStream s)throws IOException{s.close();Files.move(temp.toPath(),base.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
public void failWrite(FileOutputStream s)throws IOException{s.close();Files.deleteIfExists(temp.toPath());}
public void delete()throws IOException{Files.deleteIfExists(base.toPath());Files.deleteIfExists(temp.toPath());}}''',
}
TEST=r'''import android.database.sqlite.SQLiteDatabase;import java.io.*;import java.nio.file.*;import java.util.*;import org.json.*;
public class InstallerTest {static int checks;static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
static File root;static int n;static File backup()throws Exception{File d=new File(root,"case"+(n++));if(!d.mkdirs())throw new AssertionError();return new File(d,"before.json");}
static SQLiteDatabase db(){SQLiteDatabase d=new SQLiteDatabase();String m=CameraLspSetup.MODULES[0];d.modules.put(m,"old-m3.apk");d.states.put(m+"/0",new int[]{0,1});d.scopes.put(m+"/0",new TreeSet<>(Arrays.asList("old.scope")));
d.states.put("unrelated/0",new int[]{0,0});d.scopes.put("unrelated/0",new TreeSet<>(Arrays.asList("unrelated.scope")));d.scopes.put(m+"/10",new TreeSet<>(Arrays.asList("other.user")));return d;}
static String[] paths(){return new String[]{"m3.apk","live.apk","compat.apk"};}
static JSONObject legacy(SQLiteDatabase d)throws Exception{return new JSONObject().put("format",1).put(CameraLspSetup.MODULES[0],CameraLspSetup.snapshot(d,CameraLspSetup.MODULES[0])).put(CameraLspSetup.MODULES[1],CameraLspSetup.snapshot(d,CameraLspSetup.MODULES[1]));}
public static void main(String[] args)throws Exception{root=new File(args[0]);if(root.exists()||!root.mkdirs())throw new AssertionError("fresh test directory only");
String m=CameraLspSetup.MODULES[0],live=CameraLspSetup.MODULES[1],compat=CameraLspSetup.MODULES[2];SQLiteDatabase d=db();File b=backup();String original=CameraLspSetup.snapshot(d,m).toString();
CameraLspSetup.apply(d,b,paths());ok(CameraLspSetup.appliedState(CameraLspSetup.snapshot(d,m),CameraLspSetup.SCOPES[0],1),"existing M3 scope/blocked");
ok(d.schemaReads==3,"apply checks the three table contracts once");
ok(CameraLspSetup.appliedState(CameraLspSetup.snapshot(d,live),CameraLspSetup.SCOPES[1],0),"LiveMotion scope");
ok(CameraLspSetup.appliedState(CameraLspSetup.snapshot(d,compat),CameraLspSetup.SCOPES[2],0),"new CameraCompat scope");
ok(CameraLspSetup.scopeSet(CameraLspSetup.snapshot(d,compat).getJSONArray("scopes")).equals(new TreeSet<>(Arrays.asList("com.android.camera","com.miui.mediaeditor"))),"Compat exact camera/editor scopes");
ok(CameraLspSetup.scopeSet(CameraLspSetup.snapshot(d,live).getJSONArray("scopes")).equals(new TreeSet<>(Arrays.asList("com.android.camera"))),"LiveMotion scope stays camera only");
byte[] bytes=Files.readAllBytes(b.toPath());CameraLspSetup.apply(d,b,paths());ok(Arrays.equals(bytes,Files.readAllBytes(b.toPath())),"repeat preserves first backup");
ok(d.states.get("unrelated/0")[0]==0&&d.scopes.get(m+"/10").contains("other.user"),"unrelated module/user retained");
int reads=d.schemaReads;CameraLspSetup.restore(d,b);ok(d.schemaReads==reads+3,"restore checks the three table contracts once");ok(original.equals(CameraLspSetup.snapshot(d,m).toString())&&!CameraLspSetup.snapshot(d,compat).getBoolean("state_exists")&&!CameraLspSetup.snapshot(d,live).getBoolean("state_exists")&&!b.exists(),"exact rollback including absent targets");
d=db();b=backup();JSONObject old=legacy(d);String oldM3=old.getJSONObject(m).toString();Files.write(b.toPath(),old.toString().getBytes("UTF-8"));
CameraLspSetup.apply(d,b,paths());JSONObject augmented=new JSONObject(new String(Files.readAllBytes(b.toPath()),"UTF-8"));
ok(augmented.getJSONObject(m).toString().equals(oldM3)&&!augmented.getJSONObject(compat).getBoolean("state_exists"),"legacy backup extended without recapturing original two");
CameraLspSetup.restore(d,b);ok(original.equals(CameraLspSetup.snapshot(d,m).toString())&&!CameraLspSetup.snapshot(d,compat).getBoolean("state_exists"),"legacy extension restores original settings");
d=db();b=backup();Files.write(b.toPath(),legacy(d).toString().getBytes("UTF-8"));CameraLspSetup.restore(d,b);ok(!CameraLspSetup.snapshot(d,compat).getBoolean("state_exists"),"restore accepts pre-apply legacy backup");
d=db();b=backup();CameraLspSetup.apply(d,b,paths());d.states.get(compat+"/0")[0]=0;CameraLspSetup.restore(d,b);
ok(d.states.get(compat+"/0")[0]==0&&b.isFile(),"preserve later user changes and backup");
d=db();b=backup();d.version=100;CameraLspSetup.apply(d,b,paths());ok(CameraLspSetup.appliedState(CameraLspSetup.snapshot(d,compat),CameraLspSetup.SCOPES[2],0),"compatible schema accepted independently of version");
d=db();b=backup();d.version=102;d.tableColumns.put("modules",new String[]{"extra","apk_path","module_pkg_name"});d.tableColumns.put("modules_state",new String[]{"scope_request_blocked","enabled","user_id","extra","module_pkg_name"});d.tableColumns.put("scope",new String[]{"user_id","app_pkg_name","extra","module_pkg_name"});d.tableKeys.put("scope",new String[]{"user_id","app_pkg_name","module_pkg_name"});
CameraLspSetup.apply(d,b,paths());CameraLspSetup.restore(d,b);ok(original.equals(CameraLspSetup.snapshot(d,m).toString())&&!b.exists(),"reordered columns and identity key order plus extra fields support apply/restore");
d=db();b=backup();d.extraRequired=true;d.extraDefault="0";d.tableColumns.put("modules",new String[]{"module_pkg_name","apk_path","extra"});CameraLspSetup.apply(d,b,paths());ok(b.isFile(),"extra required column with a default accepted");
d=db();b=backup();d.extraRequired=true;d.tableColumns.put("modules",new String[]{"module_pkg_name","apk_path","extra"});boolean rejected=false;try{CameraLspSetup.apply(d,b,paths());}catch(IllegalStateException e){rejected=true;}
ok(rejected&&d.writes==0&&!b.exists(),"extra insert-required column rejected before INSERT OR IGNORE can skip registration");
d=db();b=backup();d.tableColumns.put("modules_state",new String[]{"module_pkg_name","user_id","enabled"});rejected=false;try{CameraLspSetup.apply(d,b,paths());}catch(IllegalStateException e){rejected=true;}
ok(rejected&&d.writes==0&&!b.exists(),"missing consumed field rejected before writes");
d=db();b=backup();d.tableColumns.remove("scope");rejected=false;try{CameraLspSetup.apply(d,b,paths());}catch(IllegalStateException e){rejected=true;}
ok(rejected&&d.writes==0&&!b.exists(),"missing table rejected before writes");
for(String table:Arrays.asList("modules","modules_state","scope")){d=db();b=backup();d.tableKeys.put(table,new String[]{"module_pkg_name","apk_path"});rejected=false;try{CameraLspSetup.apply(d,b,paths());}catch(IllegalStateException e){rejected=true;}
ok(rejected&&d.writes==0&&!b.exists(),"changed identity key rejected before writes: "+table);}
d=db();b=backup();rejected=false;try{CameraLspSetup.apply(d,b,new String[]{"only.apk"});}catch(IllegalArgumentException e){rejected=true;}
ok(rejected&&d.writes==0&&!b.exists(),"three path contract checked before writes");
d=db();b=backup();Files.write(b.toPath(),new JSONObject().put("format",1).put(m,CameraLspSetup.snapshot(d,m)).toString().getBytes("UTF-8"));bytes=Files.readAllBytes(b.toPath());rejected=false;
try{CameraLspSetup.apply(d,b,paths());}catch(IllegalStateException e){rejected=true;}ok(rejected&&d.writes==0&&Arrays.equals(bytes,Files.readAllBytes(b.toPath())),"corrupt original snapshot not fabricated");
d=db();b=backup();Files.write(b.toPath(),legacy(d).toString().getBytes("UTF-8"));bytes=Files.readAllBytes(b.toPath());android.util.AtomicFile.failNextWrite=true;rejected=false;
try{CameraLspSetup.apply(d,b,paths());}catch(IOException e){rejected=true;}ok(rejected&&d.writes==0&&Arrays.equals(bytes,Files.readAllBytes(b.toPath())),"failed extension preserves atomic backup and DB");
d=db();b=backup();d.failScope=true;rejected=false;try{CameraLspSetup.apply(d,b,paths());}catch(IllegalStateException e){rejected=true;}
ok(rejected&&original.equals(CameraLspSetup.snapshot(d,m).toString())&&!d.states.containsKey(compat+"/0")&&b.isFile(),"SQL failure rolls back targets and retains recovery snapshot");
d=db();b=backup();d.modules.put(compat,"old-compat.apk");d.states.put(compat+"/0",new int[]{1,0});d.scopes.put(compat+"/0",new TreeSet<>(Arrays.asList("com.android.camera")));
String oldCompat=CameraLspSetup.snapshot(d,compat).toString();CameraLspSetup.apply(d,b,paths());
ok(CameraLspSetup.scopeSet(CameraLspSetup.snapshot(d,compat).getJSONArray("scopes")).equals(new TreeSet<>(Arrays.asList("com.android.camera","com.miui.mediaeditor"))),"upgrade adds editor to an existing camera-only Compat");
CameraLspSetup.restore(d,b);ok(oldCompat.equals(CameraLspSetup.snapshot(d,compat).toString()),"restore exact pre-upgrade camera-only scopes and enabled state");
System.out.println("PASS actual three-LSP setup/legacy-backup/rollback: "+checks+" assertions; live DB forbidden");}}
'''

def main():
    build=HERE/'build';fixtures=build/'fixtures';fixtures.mkdir(parents=True,exist_ok=True)
    for name,text in FIX.items():p=fixtures/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
    (fixtures/'InstallerTest.java').write_text(TEST,encoding='utf-8');classes=build/'classes';classes.mkdir(exist_ok=True)
    source=ROOT/'installer/java/CameraLspSetup.java';outdir=build/('db-'+__import__('uuid').uuid4().hex[:10]);output=''
    for command in [[JAVA/'javac.exe','--release','8','-encoding','UTF-8','-cp',GSON,'-d',classes,source,*fixtures.rglob('*.java')],
                    [JAVA/'java.exe','-cp',str(classes)+';'+str(GSON),'InstallerTest',outdir]]:
        p=subprocess.run([str(x) for x in command],capture_output=True);log=(p.stdout+p.stderr).decode('utf-8','replace')
        with (build/'run.log').open('a',encoding='utf-8') as f:f.write(log)
        if p.returncode:raise RuntimeError(f'installer check failed exit={p.returncode}; see build/run.log')
        output=p.stdout.decode('utf-8').strip()
    assert 'PASS actual three-LSP setup/legacy-backup/rollback:' in output
    report=dict(passed=True,result=output.splitlines()[-1],source=str(source),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                live_database_opened=False,installed=False,scope='production Java against test-only SQL/AtomicFile and real Gson JSON codec')
    (HERE/'RESULT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print(report['result'])

if __name__=='__main__':main()
