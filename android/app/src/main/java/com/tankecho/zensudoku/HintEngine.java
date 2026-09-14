package com.tankecho.zensudoku;
import java.util.*;

/** Pure logical solver. It never reads the answer grid or guesses. */
public final class HintEngine {
    public static final int[][] UNITS=new int[27][9];
    static {for(int i=0;i<81;i++){UNITS[i/9][i%9]=i;UNITS[9+i%9][i/9]=i;UNITS[18+i/27*3+(i%9)/3][(i/9)%3*3+i%3]=i;}}
    public static final class Plan {
        public final int cell,digit,rank;public final String explanation;public final int[] related;
        Plan(int cell,int digit,int rank,String explanation,Set<Integer> related){this.cell=cell;this.digit=digit;this.rank=rank;this.explanation=explanation;this.related=related.stream().mapToInt(Integer::intValue).toArray();}
    }
    private static String cell(int i){return "第"+(i/9+1)+"行第"+(i%9+1)+"列";}
    private static String unit(int u){return u<9?"第"+(u+1)+"行":u<18?"第"+(u-8)+"列":"第"+(u-17)+"宫";}
    private static String nums(int mask){StringBuilder s=new StringBuilder();for(int n=1;n<=9;n++)if((mask&(1<<(n-1)))!=0){if(s.length()>0)s.append("、");s.append(n);}return s.toString();}
    private static final class State {
        int[] b,c=new int[81];int rank=0;List<String> steps=new ArrayList<>();Set<Integer> related=new LinkedHashSet<>();
        State(int[] board){b=board.clone();for(int i=0;i<81;i++)c[i]=Game.mask(b,i);}
        void note(int r,String s,int... units){rank=Math.max(rank,r);steps.add(s);for(int u:units)for(int i:UNITS[u])related.add(i);}
        void put(int i,int n){b[i]=n;c[i]=0;for(int j=0;j<81;j++)if(Game.peer(i,j))c[j]&=~(1<<(n-1));}
    }
    public static Plan find(int[] board,int preferred){return next(new State(board),preferred,3);}
    /** -1 means unsupported logical path; 0..3 are proven technique grades. */
    public static int grade(int[] board){State s=new State(board);int rank=0;for(int k=0;k<81;k++){boolean done=true;for(int v:s.b)if(v==0)done=false;if(done)return rank;Plan p=next(s,-1,3);if(p==null)return -1;rank=Math.max(rank,p.rank);s.put(p.cell,p.digit);s.steps.clear();s.related.clear();s.rank=0;}return -1;}
    private static Plan next(State s,int preferred,int maxRank){
        for(int rounds=0;rounds<200;rounds++){
            for(int v=0;v<82;v++){int i=v==0?preferred:v-1;if(i<0||i>=81||s.b[i]!=0)continue;if(s.c[i]==0)return null;if(Integer.bitCount(s.c[i])==1){int n=Integer.numberOfTrailingZeros(s.c[i])+1;s.note(0,cell(i)+"的行、列和宫排除其他数字后，只剩 "+n+"。",i/9,9+i%9,18+i/27*3+(i%9)/3);return plan(s,i,n);}}
            if(maxRank<1)return null;
            for(int u=0;u<27;u++)for(int n=1;n<=9;n++){int at=-1,count=0;for(int i:UNITS[u])if((s.c[i]&(1<<(n-1)))!=0){at=i;count++;}if(count==1){s.note(1,unit(u)+"中，数字 "+n+" 只有"+cell(at)+"这一个位置。",u);return plan(s,at,n);}}
            if(maxRank<2)return null;
            if(locked(s)||subsets(s,2))continue;
            if(maxRank<3)return null;
            if(subsets(s,3)||xwing(s))continue;
            return null;
        }return null;
    }
    private static Plan plan(State s,int i,int n){s.related.add(i);return new Plan(i,n,s.rank,String.join("\n\n",s.steps),s.related);}
    private static boolean locked(State s){
        for(int u=0;u<27;u++)for(int n=1;n<=9;n++){int bit=1<<(n-1);List<Integer> pos=new ArrayList<>();for(int i:UNITS[u])if((s.c[i]&bit)!=0)pos.add(i);if(pos.size()<2)continue;
            for(int other=0;other<27;other++){if(other==u)continue;boolean all=true;for(int i:pos)if(!contains(UNITS[other],i)){all=false;break;}if(!all)continue;boolean changed=false;for(int i:UNITS[other])if(!contains(UNITS[u],i)&&(s.c[i]&bit)!=0){s.c[i]&=~bit;changed=true;}if(changed){s.note(2,"区块排除："+unit(u)+"的 "+n+" 都落在"+unit(other)+"，因此"+unit(other)+"其余格不能是 "+n+"。",u,other);return true;}}
        }return false;
    }
    private static boolean subsets(State s,int size){
        for(int u=0;u<27;u++){int[] a=UNITS[u];for(int x=0;x<9;x++)for(int y=x+1;y<9;y++)for(int z=(size==2?9:y+1);z<(size==2?10:9);z++){
            int i=a[x],j=a[y],k=size==2?-1:a[z];if(s.c[i]==0||s.c[j]==0||(k>=0&&s.c[k]==0))continue;int mask=s.c[i]|s.c[j]|(k<0?0:s.c[k]);if(Integer.bitCount(mask)!=size)continue;
            boolean changed=false;for(int at:a)if(at!=i&&at!=j&&at!=k&&(s.c[at]&mask)!=0){s.c[at]&=~mask;changed=true;}if(changed){s.note(size==2?2:3,(size==2?"显性数对：":"显性三数组：")+unit(u)+"内的"+cell(i)+"、"+cell(j)+(k<0?"":"、"+cell(k))+"共同占据 "+nums(mask)+"，可从该区域其余格排除这些数。",u);return true;}
        }}return false;
    }
    private static boolean xwing(State s){
        for(int axis=0;axis<2;axis++)for(int n=1;n<=9;n++){int bit=1<<(n-1);int[] lines=new int[9];for(int line=0;line<9;line++)for(int off=0;off<9;off++){int i=axis==0?line*9+off:off*9+line;if((s.c[i]&bit)!=0)lines[line]|=1<<off;}
            for(int a=0;a<9;a++)if(Integer.bitCount(lines[a])==2)for(int b=a+1;b<9;b++)if(lines[a]==lines[b]){
                boolean changed=false;for(int line=0;line<9;line++)if(line!=a&&line!=b)for(int off=0;off<9;off++)if((lines[a]&(1<<off))!=0){int i=axis==0?line*9+off:off*9+line;if((s.c[i]&bit)!=0){s.c[i]&=~bit;changed=true;}}
                if(changed){s.note(3,"X-Wing：数字 "+n+" 在第"+(a+1)+"、"+(b+1)+(axis==0?"行":"列")+"只落在相同的两个"+(axis==0?"列":"行")+"，四个角构成矩形，可从这两"+(axis==0?"列":"行")+"的其余格排除 "+n+"。",axis*9+a,axis*9+b);return true;}
            }
        }return false;
    }
    private static boolean contains(int[] a,int n){for(int v:a)if(v==n)return true;return false;}
}
