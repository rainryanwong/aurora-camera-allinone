#define _GNU_SOURCE
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <poll.h>
#include <stdio.h>
#include <string.h>
#include <sys/file.h>
#include <sys/inotify.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/xattr.h>
#include <unistd.h>

/* Opaque PXM3AUX1 delivery. No photo rendering or metadata generation here. */
static const char *src_path="/data/vendor/camera/offlinelog/phoenix_m3_aux";
static const char *dst_path="/data/user/0/com.android.camera/files/phoenix_m3_aux";
static int srcfd;
static int retry_pending;
static unsigned sequence;

static int valid_name(const char *name) {
    size_t n=strlen(name);
    if(n<9 || n>127 || strcmp(name+n-8,".jpg.aux")) return 0;
    for(size_t i=0;i<n;i++) {
        unsigned char c=(unsigned char)name[i];
        if(!((c>='A'&&c<='Z')||(c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='_'||c=='-'||c=='.')) return 0;
    }
    return 1;
}

static ssize_t read_at(int fd,void *buf,size_t count,off_t offset) {
    size_t done=0;
    while(done<count) {
        ssize_t n=pread(fd,(char *)buf+done,count-done,offset+(off_t)done);
        if(n<0 && errno==EINTR) continue;
        if(n<=0) return n<0 ? -1 : (ssize_t)done;
        done+=(size_t)n;
    }
    return (ssize_t)done;
}

static int same_contents(int a,int b,off_t size) {
    struct stat s;
    if(fstat(b,&s)||!S_ISREG(s.st_mode)||s.st_size!=size) return 0;
    unsigned char x[65536],y[65536];
    for(off_t pos=0;pos<size;) {
        size_t n=(size-pos)>(off_t)sizeof(x)?sizeof(x):(size_t)(size-pos);
        if(read_at(a,x,n,pos)!=(ssize_t)n || read_at(b,y,n,pos)!=(ssize_t)n || memcmp(x,y,n)) return 0;
        pos+=(off_t)n;
    }
    return 1;
}

static int unlink_source_if_same(const char *name,const struct stat *original) {
    struct stat now;
    if(fstatat(srcfd,name,&now,AT_SYMLINK_NOFOLLOW)) return errno==ENOENT?0:-1;
    if(now.st_dev!=original->st_dev || now.st_ino!=original->st_ino) return 0;
    if(unlinkat(srcfd,name,0) && errno!=ENOENT) return -1;
    return 0;
}

/* The camera data directory can be replaced while this process stays alive. */
static int open_destination(struct stat *owner,char *context,ssize_t *context_len) {
    int fd=open(dst_path,O_RDONLY|O_DIRECTORY|O_CLOEXEC|O_NOFOLLOW);
    if(fd<0 && errno==ENOENT) {
        int parent=open("/data/user/0/com.android.camera/files",O_RDONLY|O_DIRECTORY|O_CLOEXEC|O_NOFOLLOW);
        if(parent<0) return -1;
        int created=0;
        if(mkdirat(parent,"phoenix_m3_aux",0700)==0) created=1;
        else if(errno!=EEXIST) {int saved=errno;close(parent);errno=saved;return -1;}
        fd=openat(parent,"phoenix_m3_aux",O_RDONLY|O_DIRECTORY|O_CLOEXEC|O_NOFOLLOW);
        if(fd>=0 && created) {
            struct stat app;
            char label[512];
            ssize_t size=fgetxattr(parent,"security.selinux",label,sizeof(label));
            if(size<0 || fstat(parent,&app) || fchown(fd,app.st_uid,app.st_gid) ||
                    fsetxattr(fd,"security.selinux",label,(size_t)size,0)) {
                int saved=errno;close(fd);fd=-1;errno=saved;
            }
        }
        int saved=errno;close(parent);errno=saved;
    }
    if(fd<0) return -1;
    *context_len=fgetxattr(fd,"security.selinux",context,512);
    if(*context_len<0 || fstat(fd,owner)) {
        int saved=errno;close(fd);errno=saved;return -1;
    }
    return fd;
}

static int destination_is_current(int fd) {
    struct stat opened,current;
    if(fstat(fd,&opened) || lstat(dst_path,&current)) return 0;
    if(opened.st_dev!=current.st_dev || opened.st_ino!=current.st_ino) {errno=EAGAIN;return 0;}
    return 1;
}

static void transfer(const char *name) {
    if(!valid_name(name)) return;
    int in=-1,out=-1,existing=-1,dstfd=-1,published=0,created=0,saved=0;
    struct stat owner;
    char context[512];
    ssize_t context_len;
    struct stat s,after;
    char temp[128]={0};
    unsigned char header[168],buf[65536];
    in=openat(srcfd,name,O_RDONLY|O_CLOEXEC|O_NOFOLLOW);
    if(in<0) {if(errno!=ENOENT) perror(name);return;}
    if(fstat(in,&s)||!S_ISREG(s.st_mode)||s.st_size<168||s.st_size>(off_t)512*1024*1024) {errno=EINVAL;goto failed;}
    if(read_at(in,header,sizeof(header),0)!=(ssize_t)sizeof(header) || memcmp(header,"PXM3AUX1",8)) {errno=EINVAL;goto failed;}
    size_t length=strnlen((char *)header+40,128),name_len=strlen(name)-4;
    if(length!=name_len || memcmp(header+40,name,name_len)) {errno=EINVAL;goto failed;}
    dstfd=open_destination(&owner,context,&context_len);
    if(dstfd<0) {
        if(errno==ENOENT || errno==ESTALE) retry_pending=1;
        goto failed;
    }
    existing=openat(dstfd,name,O_RDONLY|O_CLOEXEC|O_NOFOLLOW);
    if(existing>=0) {
        if(!same_contents(in,existing,s.st_size)) {errno=EEXIST;goto failed;}
        published=1;goto finished;
    }
    if(errno!=ENOENT) goto failed;
    /* Never share basename.tmp with the legacy relay or unlink its temp. */
    snprintf(temp,sizeof(temp),".relay.%ld.%u.tmp",(long)getpid(),++sequence);
    out=openat(dstfd,temp,O_WRONLY|O_CREAT|O_EXCL|O_CLOEXEC|O_NOFOLLOW,0600);
    if(out<0) goto failed;
    created=1;
    if(fchown(out,owner.st_uid,owner.st_gid) || fsetxattr(out,"security.selinux",context,(size_t)context_len,0)) goto failed;
    for(off_t pos=0;pos<s.st_size;) {
        size_t n=(s.st_size-pos)>(off_t)sizeof(buf)?sizeof(buf):(size_t)(s.st_size-pos);
        if(read_at(in,buf,n,pos)!=(ssize_t)n) {errno=EIO;goto failed;}
        for(size_t done=0;done<n;) {
            ssize_t written=write(out,buf+done,n-done);
            if(written<0 && errno==EINTR) continue;
            if(written<=0) {if(written==0) errno=EIO;goto failed;}
            done+=(size_t)written;
        }
        pos+=(off_t)n;
    }
    if(fstat(in,&after)||after.st_size!=s.st_size||after.st_mtim.tv_sec!=s.st_mtim.tv_sec||after.st_mtim.tv_nsec!=s.st_mtim.tv_nsec) {errno=EAGAIN;goto failed;}
    if(fsync(out)) goto failed;
    if(close(out)) {out=-1;goto failed;} out=-1;
    /* Atomically publish without replacing an already delivered capture. */
    if(linkat(dstfd,temp,dstfd,name,0)) {
        if(errno!=EEXIST) goto failed;
        existing=openat(dstfd,name,O_RDONLY|O_CLOEXEC|O_NOFOLLOW);
        if(existing<0 || !same_contents(in,existing,s.st_size)) {errno=EEXIST;goto failed;}
    }
    published=1;
finished:
    if(!destination_is_current(dstfd)) {
        if(errno==ENOENT || errno==ESTALE || errno==EAGAIN) retry_pending=1;
        goto failed;
    }
    if(published && unlink_source_if_same(name,&s)) {perror("aux source cleanup; delivered copy retained");}
    printf("aux delivered %s bytes=%lld\n",name,(long long)s.st_size);fflush(stdout);
    goto cleanup;
failed:
    saved=errno;
    if(dstfd>=0 && !destination_is_current(dstfd) &&
            (errno==ENOENT || errno==ESTALE || errno==EAGAIN)) retry_pending=1;
    fprintf(stderr,"aux retained %s errno=%d %s\n",name,saved,strerror(saved));fflush(stderr);
cleanup:
    if(existing>=0) close(existing);
    if(out>=0) close(out);
    if(created) unlinkat(dstfd,temp,0);
    if(dstfd>=0) close(dstfd);
    close(in);
}

static void scan(void) {
    int fd=openat(srcfd,".",O_RDONLY|O_DIRECTORY|O_CLOEXEC);
    if(fd<0) {perror("aux rescan");return;}
    DIR *dir=fdopendir(fd);
    if(!dir) {close(fd);perror("aux rescan dir");return;}
    struct dirent *entry;
    while((entry=readdir(dir))) transfer(entry->d_name);
    closedir(dir);
}

int main(void) {
    srcfd=open(src_path,O_RDONLY|O_DIRECTORY|O_CLOEXEC|O_NOFOLLOW);
    if(srcfd<0) {perror("aux source directory");return 1;}
    int lockfd=openat(srcfd,".relay.lock",O_RDWR|O_CREAT|O_CLOEXEC|O_NOFOLLOW,0600);
    if(lockfd<0) {perror("aux lock");return 1;}
    if(flock(lockfd,LOCK_EX|LOCK_NB)) {
        if(errno==EWOULDBLOCK) {puts("aux relay already owned");return 0;}
        perror("aux flock");return 1;
    }
    int events=inotify_init1(IN_CLOEXEC);
    if(events<0 || inotify_add_watch(events,src_path,IN_MOVED_TO|IN_CLOSE_WRITE|IN_DELETE_SELF|IN_MOVE_SELF)<0) {perror("aux watch");return 1;}
    scan();puts("aux relay v3 ready");fflush(stdout);
    union {char bytes[8192];struct inotify_event align;} buffer;
    for(;;) {
        struct pollfd watch={.fd=events,.events=POLLIN};
        int ready=poll(&watch,1,retry_pending?1000:-1);
        if(ready<0 && errno==EINTR) continue;
        if(ready<0) {perror("aux poll");return 1;}
        if(ready==0) {retry_pending=0;scan();continue;}
        ssize_t n=read(events,buffer.bytes,sizeof(buffer.bytes));
        if(n<0 && errno==EINTR) continue;
        if(n<=0) {perror("aux events");return 1;}
        for(size_t pos=0;pos+sizeof(struct inotify_event)<=(size_t)n;) {
            struct inotify_event *e=(struct inotify_event *)(buffer.bytes+pos);
            if(pos+sizeof(*e)+e->len>(size_t)n) {fprintf(stderr,"invalid aux event\n");return 1;}
            if(e->mask&IN_Q_OVERFLOW) scan();
            if(e->mask&(IN_IGNORED|IN_DELETE_SELF|IN_MOVE_SELF)) {fprintf(stderr,"aux watch lost\n");return 1;}
            if(e->len && !(e->mask&IN_ISDIR) && (e->mask&(IN_MOVED_TO|IN_CLOSE_WRITE))) transfer(e->name);
            pos+=sizeof(*e)+e->len;
        }
    }
}
