import com.tankecho.zensudoku.*;
import java.util.*;import java.nio.file.*;
/** Independent randomized full grids; each accepted puzzle has a checked logical grade. */
public class ExpandBank {
 static Random rng=new Random(17020260914L);static List<String>[] pools=new ArrayList[4];static Set<String> seen=new HashSet<>();
 static boolean fill(int[] b){int at=-1,mask=0,best=10;for(int i=0;i<81;i++)if(b[i]==0){int m=Game.mask(b,i),n=Integer.bitCount(m);if(n==0)return false;if(n<best){at=i;mask=m;best=n;}if(n==1)break;}if(at<0)return true;List<Integer>a=new ArrayList<>();for(int n=1;n<=9;n++)if((mask&(1<<(n-1)))!=0)a.add(n);Collections.shuffle(a,rng);for(int n:a){b[at]=n;if(fill(b))return true;}b[at]=0;return false;}
 public static void main(String[] args)throws Exception{int target=Integer.parseInt(args[1]);for(int i=0;i<4;i++)pools[i]=new ArrayList<>();int attempts=0;long began=System.currentTimeMillis();
 while(Arrays.stream(pools).anyMatch(p->p.size()<target)){int[] sol=new int[81];fill(sol);int[] b=sol.clone();List<Integer> order=new ArrayList<>();for(int i=0;i<81;i++)order.add(i);Collections.shuffle(order,rng);boolean[] captured=new boolean[4];attempts++;
 for(int i:order){int old=b[i];b[i]=0;if(Game.countSolutions(b.clone(),2)!=1){b[i]=old;continue;}int clues=0;for(int v:b)if(v>0)clues++;if(clues>42)continue;int grade=HintEngine.grade(b);if(grade<0||captured[grade]||pools[grade].size()>=target)continue;if(grade==0&&clues>42||grade==1&&clues>37||grade==2&&clues>32||grade==3&&clues>29)continue;String puzzle=Game.encode(b);if(!seen.add(puzzle))continue;captured[grade]=true;String technique=new String[]{"直接单数","隐性单数","区块排除 / 数对","三数组 / X-Wing"}[grade];pools[grade].add("{\"puzzle\":\""+puzzle+"\",\"solution\":\""+Game.encode(sol)+"\",\"clues\":"+clues+",\"technique\":\""+technique+"\",\"id\":\"v17-"+grade+"-"+pools[grade].size()+"\"}");}
 if(attempts%20==0)System.out.println("attempts="+attempts+" sizes="+Arrays.toString(Arrays.stream(pools).mapToInt(List::size).toArray())+" ms="+(System.currentTimeMillis()-began));}
 StringJoiner out=new StringJoiner(",","[","]");for(List<String> pool:pools)out.add("["+String.join(",",pool)+"]");Files.write(Paths.get(args[0]),out.toString().getBytes("UTF-8"));System.out.println("BANK_COMPLETE "+target*4+" unique logically solved puzzles");}
}
