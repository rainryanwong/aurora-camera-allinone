"""Patch only pinned Stock1770 MiFilterSDK's CV shader getter; never touch existing inputs."""
from pathlib import Path
import argparse,hashlib,io,json,struct,subprocess,sys,uuid
sys.path.insert(0,'D:/cam18pm/tools/native_analysis')
from elftools.elf.elffile import ELFFile
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM

HERE=Path(__file__).resolve().parent
PROJECT=HERE.parents[1]
LLVM=Path('D:/DevTools/AndroidSdk/ndk/29.0.14206865/toolchains/llvm/prebuilt/windows-x86_64/bin')
BASE_SHA='a63acd57e2156bcd4bc5bf77439185954533007fefaa3f84742cec2a16ba148c'
GETTER_VA=0x715d0
GETTER_OFFSET=0x6d5d0
SHADER_VA=0x2faab
ALIGN=0x4000
IMPORTS={'_Znwm':0xab850,'memcpy':0xab810,'memchr':0xabe90,
         'fopen':0xabfb0,'fseek':0xabfc0,'ftell':0xabfd0,
         'fread':0xabfe0,'fclose':0xabff0,
         '__android_log_print':0xab7d0,'abort':0xac650}
def sha(b):return hashlib.sha256(b).hexdigest()
def align(n,a=ALIGN):return (n+a-1)&~(a-1)
def run(args,work,label):
    r=subprocess.run(list(map(str,args)),capture_output=True)
    (work/(label+'.stdout.bin')).write_bytes(r.stdout);(work/(label+'.stderr.bin')).write_bytes(r.stderr)
    if r.returncode:raise RuntimeError(label+': '+(r.stdout+r.stderr).decode('utf8','replace'))
    return r.stdout
def fields(e):return [{k:p[k] for k in ['p_type','p_flags','p_offset','p_vaddr','p_paddr','p_filesz','p_memsz','p_align']} for p in e.iter_segments()]
def ranges(values):
    out=[]
    for v in values:
        if out and v==out[-1][1]:out[-1][1]+=1
        else:out.append([v,v+1])
    return out
def verify(original,patched,loader,entry,payload_offset,work):
    src=ELFFile(io.BytesIO(original));dst=ELFFile(io.BytesIO(patched));ld=ELFFile(io.BytesIO(loader))
    before,after=fields(src),fields(dst)
    loads0=[p for p in before if p['p_type']=='PT_LOAD'];loads1=[p for p in after if p['p_type']=='PT_LOAD']
    assert loads1[:-1]==loads0 and len(loads1)==len(loads0)+1
    new=loads1[-1];table=after[0];count=len(after)
    assert table['p_type']=='PT_PHDR' and table['p_offset']==dst['e_phoff']==new['p_offset']==payload_offset
    assert table['p_vaddr']==new['p_vaddr'] and table['p_filesz']==table['p_memsz']==count*56
    assert table['p_filesz']<=new['p_filesz'] and table['p_flags']==4
    assert new['p_flags']==5 and new['p_filesz']==new['p_memsz'] and new['p_align']==ALIGN
    assert new['p_vaddr']%ALIGN==new['p_offset']%ALIGN==0
    assert new['p_offset']>=len(original) and new['p_offset']+new['p_filesz']==len(patched)
    assert new['p_vaddr']>=max(p['p_vaddr']+p['p_memsz'] for p in loads0)
    assert before[1:]==after[1:-1], 'Only copied PT_PHDR changes; existing other program headers stay exact'
    for k in ['e_type','e_machine','e_version','e_entry','e_shoff','e_flags','e_ehsize','e_phentsize','e_shentsize','e_shnum','e_shstrndx']:assert src[k]==dst[k],k
    allowed=[(32,40),(56,58),(GETTER_OFFSET,GETTER_OFFSET+8)]
    changed=[i for i,(a,b) in enumerate(zip(original,patched)) if a!=b]
    assert all(any(start<=i<end for start,end in allowed) for i in changed)
    assert len(patched)>=len(original) and all(original[i]==patched[i] for i in range(len(original)) if not any(a<=i<b for a,b in allowed))
    immutable={}
    for s in src.iter_sections():
        if s.name in ['.dynsym','.dynstr','.gnu.hash','.gnu.version','.gnu.version_r','.dynamic','.rela.dyn','.rela.plt','.data.rel.ro','.got','.got.plt','.data','.bss','.note.gnu.build-id']:
            out=dst.get_section_by_name(s.name);assert s.data()==out.data(),s.name;immutable[s.name]=sha(s.data())
    ms=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
    glue=list(ms.disasm(patched[GETTER_OFFSET:GETTER_OFFSET+8],GETTER_VA))
    assert len(glue)==2 and glue[0].mnemonic=='mov' and glue[0].op_str=='x0, x8'
    assert glue[1].mnemonic=='b' and int(glue[1].op_str[1:],16)==entry
    sym=ld.get_section_by_name('.symtab');symbols={s.name:s['st_value'] for s in sym.iter_symbols()}
    assert symbols['auth_get_cv_shader']==entry and symbols['auth_original_shader']==SHADER_VA
    assert all(symbols[name]==address for name,address in IMPORTS.items())
    assert not any(s['st_shndx']=='SHN_UNDEF' and s.name for s in sym.iter_symbols())
    assert not any(s['sh_size'] for s in ld.iter_sections() if s['sh_type'] in ['SHT_REL','SHT_RELA'])
    assert not any(s['sh_size'] for s in ld.iter_sections() if s.name in ['.got','.got.plt','.bss','.data'])
    code=ld.get_section_by_name('.text');branches=[]
    for x in ms.disasm(code.data(),code['sh_addr']):
        if x.mnemonic in ['b','bl']:
            target=int(x.op_str[1:],16)
            assert new['p_vaddr']<=target<new['p_vaddr']+new['p_memsz'] or target in IMPORTS.values(),(hex(x.address),hex(target))
            branches.append({'address':hex(x.address),'instruction':x.mnemonic,'target':hex(target)})
    disassembly='\n'.join(hex(x.address)+': '+x.mnemonic+' '+x.op_str for x in ms.disasm(code.data(),code['sh_addr']))
    (work/'loader-disassembly.txt').write_text(disassembly+'\n',encoding='utf8')
    return {'passed':True,'old_load_headers_unchanged':True,'old_file_bytes_unchanged_except_named_ranges':True,'changed_original_ranges':[[hex(a),hex(b)] for a,b in ranges(changed)],
            'allowed_original_ranges':[[hex(a),hex(b)] for a,b in allowed],'new_bytes':len(patched)-len(original),'new_load':new,'new_phdr':table,
            'relocated_phdr_inside_new_readable_load':True,'native_16kb_alignment':True,'new_load_writable':False,'existing_all_other_phdrs_unchanged':True,
            'immutable_sections':immutable,'imports_and_export_hashes_unchanged':True,'all_loader_imports_resolved_to_original_plt':True,'unresolved_relocations':0,
            'loader_entry':hex(entry),'entry_glue':[x.mnemonic+' '+x.op_str for x in glue],'direct_branches':branches,'loader_disassembly':str(work/'loader-disassembly.txt')}
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--input',type=Path,default=HERE/'input/libMiFilterSDK.so');ap.add_argument('--output',type=Path,default=HERE/'libMiFilterSDK.so');ap.add_argument('--manifest',type=Path,default=HERE/'PATCH_MANIFEST_250.json');ap.add_argument('--verify-output',action='store_true');args=ap.parse_args()
    assert args.verify_output or not args.output.exists(), 'Refusing to overwrite an existing candidate'
    if args.verify_output:assert args.output.is_file(),'Verification requires an existing output'
    original=args.input.read_bytes();assert sha(original)==BASE_SHA
    src=ELFFile(io.BytesIO(original));old=fields(src);assert src['e_machine']=='EM_AARCH64' and src['e_phentsize']==56 and src['e_phnum']==10
    assert original[GETTER_OFFSET:GETTER_OFFSET+8]==bytes.fromhex('fd7bbea9f44f01a9')
    shader=original[SHADER_VA:original.index(0,SHADER_VA)];assert len(shader)==2015 and sha(shader)=='2bbffc24137ddcecf39067136d41df68f49b4b579f97b988b6f43eba5b0da4ef'
    rel=src.get_section_by_name('.rela.plt');syms=src.get_section_by_name('.dynsym');plt=src.get_section_by_name('.plt');observed={}
    for i,r in enumerate(rel.iter_relocations()):observed[syms.get_symbol(r['r_info_sym']).name]=plt['sh_addr']+32+16*i
    assert all(observed[name]==address for name,address in IMPORTS.items())
    payload_offset=align(len(original));payload_va=align(max(p['p_vaddr']+p['p_memsz'] for p in old if p['p_type']=='PT_LOAD'));phnum=len(old)+1
    code_va=payload_va+align(phnum*56,16)
    work=PROJECT/'build'/('auth250-preview-'+uuid.uuid4().hex[:10]);work.mkdir(parents=True)
    layout='ENTRY(auth_get_cv_shader)\nPHDRS { code PT_LOAD FLAGS(5); }\nSECTIONS { . = '+hex(code_va)+'; .text : { KEEP(*(.text.auth_get_cv_shader)) *(.text*) } :code .rodata : { *(.rodata*) } :code /DISCARD/ : { *(.comment) *(.note*) *(.eh_frame*) *(.llvm_addrsig) } }\n'
    (work/'layout.ld').write_text(layout,encoding='ascii')
    run([LLVM/'clang.exe','--target=aarch64-linux-android29','-std=c11','-Oz','-fPIC','-fvisibility=hidden','-ffunction-sections','-fdata-sections','-fno-builtin','-fno-stack-protector','-fno-unwind-tables','-fno-asynchronous-unwind-tables','-mbranch-protection=standard','-c',HERE/'loader.c','-o',work/'loader.o'],work,'compile')
    defs=[f'--defsym={name}={hex(address)}' for name,address in {**IMPORTS,'auth_original_shader':SHADER_VA}.items()]
    run([LLVM/'ld.lld.exe','-m','aarch64elf','--no-undefined','-T',work/'layout.ld',*defs,'-o',work/'loader.elf',work/'loader.o'],work,'link')
    linked=(work/'loader.elf').read_bytes();elf=ELFFile(io.BytesIO(linked));parts=[s for s in elf.iter_sections() if s['sh_flags']&2 and s['sh_size']]
    assert all(s.name in ['.text','.rodata'] for s in parts),[s.name for s in parts]
    extent=max(s['sh_addr']+s['sh_size'] for s in parts);payload=bytearray(extent-payload_va)
    for s in parts:
        pos=s['sh_addr']-payload_va;payload[pos:pos+s['sh_size']]=s.data()
    entry=next(s['st_value'] for s in elf.get_section_by_name('.symtab').iter_symbols() if s.name=='auth_get_cv_shader');assert entry==code_va
    def pack(t,fl,o,v,p,fs,ms,a):return struct.pack('<IIQQQQQQ',t,fl,o,v,p,fs,ms,a)
    phdrs=[pack(6,4,payload_offset,payload_va,payload_va,phnum*56,phnum*56,8)]
    original_table=original[src['e_phoff']:src['e_phoff']+src['e_phnum']*56]
    phdrs.extend(original_table[i*56:(i+1)*56] for i in range(1,len(old)))
    phdrs.append(pack(1,5,payload_offset,payload_va,payload_va,len(payload),len(payload),ALIGN))
    table=b''.join(phdrs);assert len(table)==phnum*56;payload[:len(table)]=table
    patched=bytearray(original);struct.pack_into('<Q',patched,32,payload_offset);struct.pack_into('<H',patched,56,phnum)
    delta=entry-(GETTER_VA+4);assert delta%4==0 and -(1<<27)<=delta<(1<<27)
    patched[GETTER_OFFSET:GETTER_OFFSET+8]=struct.pack('<II',0xaa0803e0,0x14000000|((delta>>2)&0x3ffffff))
    patched.extend(b'\0'*(payload_offset-len(patched)));patched.extend(payload)
    audit=verify(original,patched,linked,entry,payload_offset,work)
    if args.verify_output:assert args.output.read_bytes()==patched,'Existing output differs from rebuilt bytes'
    else:
        args.output.parent.mkdir(parents=True,exist_ok=True)
        with args.output.open('xb') as out:out.write(patched)
    (work/'original-shader.glsl').write_bytes(shader)
    sections={s.name:sha(s.data()) for s in src.iter_sections() if s.name in ['.dynsym','.dynstr','.dynamic','.gnu.hash']}
    manifest={'passed':True,'input':str(args.input),'input_sha256':sha(original),'output':str(args.output),'output_sha256':sha(patched),'input_bytes':len(original),'output_bytes':len(patched),
              'loader_source_sha256':sha((HERE/'loader.c').read_bytes()),'build_script_sha256':sha(Path(__file__).read_bytes()),'work':str(work),'formula_path':'/data/user/0/com.android.camera/files/phoenix-vignette/colorDark.glsl',
              'formula_body_bytes':{'min':1,'max':16384,'embedded_nul_rejected':True},'template':{'original_va':hex(SHADER_VA),'original_bytes':len(shader),'original_sha256':sha(shader),'prefix_bytes':1223,'suffix_bytes':341,'linefeed_before_body':True,'linefeed_after_body':True},
              'allocator':'existing _Znwm PLT; current NDK24 long-string capacity_tag=allocated_bytes|1,length,data','new_dependencies':[],'old_abi_section_hashes':sections,'audit':audit,
              'device_executed':False,'runtime_consumer_and_image_acceptance':'pending Terra after full module/provider restart; mount or hash alone is not success'}
    args.manifest.parent.mkdir(parents=True,exist_ok=True);args.manifest.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps({k:manifest[k] for k in ['passed','input_sha256','output','output_sha256','input_bytes','output_bytes','work']},ensure_ascii=False,indent=2))
if __name__=='__main__':main()
