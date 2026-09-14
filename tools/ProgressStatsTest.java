import com.tankecho.zensudoku.*;
import java.time.LocalDate;
import java.util.*;

public class ProgressStatsTest {
    static int checks;
    static void ok(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
    static ProgressStats.Entry entry(int id,int level,String status,boolean practice,int hints,int mistakes,String daily){return new ProgressStats.Entry("id"+id,"2026-09-14",status,daily,level,hints,mistakes,60000L+id,practice);}
    public static void main(String[] args){
        List<ProgressStats.Entry> entries=new ArrayList<>();Map<String,Long> days=new HashMap<>();LocalDate today=LocalDate.of(2026,9,14);
        ProgressStats empty=new ProgressStats(entries,days,today);ok(empty.best()==-1&&empty.challengeEnded==0,"empty records have no fake time/rate");
        Set<String> ids=new HashSet<>();for(Medal m:Medal.all(empty)){ok(!m.unlocked()&&m.fraction()==0,"all new-install medals locked");ok(ids.add(m.id),"stable unique ID");}ok(ids.size()==16,"sixteen medals");
        entries.add(entry(1,0,"won",false,0,0,"2026-09-01"));entries.add(entry(2,0,"won",false,1,1,"2026-09-01"));entries.add(entry(3,0,"lost",false,0,3,""));entries.add(entry(4,0,"abandoned",false,0,0,""));entries.add(entry(5,3,"playing",false,0,0,""));entries.add(entry(6,3,"won",true,0,12,""));entries.add(entry(7,0,"lost",true,0,0,""));
        days.put("2024-02-28",60000L);days.put("2024-02-29",1000L);days.put("2024-03-01",1000L);days.put("2026-09-13",999L);days.put("2027-01-01",1000L);
        ProgressStats s=new ProgressStats(entries,days,today);ok(s.challengeWins==2&&s.challengeEnded==4,"rate excludes active and all practice");ok(s.practiceWins==1&&s.practiceEnded==2,"practice separate");ok(s.dailyWins==1,"daily completion dates deduplicate");ok(s.noHintWins==1&&s.perfectWins==1,"practice cannot unlock challenge skill");ok(s.levelWins[3]==0&&s.completedDifficulties()==1,"playing/practice expert excluded");ok(s.longestStreak==3&&s.activeDays==3,"longest streak spans leap day and ignores future/subsecond");ok(s.totalTime==63999,"time is actual day totals not record sum");ok(s.best()==60001,"challenge fastest only");ok(Medal.all(s).get(8).unlocked(),"past streak remains earned even when current streak broken");
        days.clear();for(int i=0;i<30;i++)days.put(today.minusDays(i+10).toString(),1200000L);
        entries.clear();for(int i=0;i<100;i++)entries.add(entry(i,i%4,"won",false,0,0,"2026-09-"+String.format("%02d",1+i%7)));for(int i=100;i<110;i++)entries.add(entry(i,0,"won",true,1,9,""));
        ProgressStats full=new ProgressStats(entries,days,today);for(Medal m:Medal.all(full)){ok(m.unlocked(),"all achievable: "+m.id);ok(m.fraction()==1,"progress capped: "+m.id);}
        for(int n:new int[]{0,1,9,10,49,50,99,100,150}){entries.clear();for(int i=0;i<n;i++)entries.add(entry(i,0,"won",false,1,1,""));s=new ProgressStats(entries,Collections.emptyMap(),today);List<Medal> medals=Medal.all(s);for(int i=0;i<4;i++)ok(medals.get(i).unlocked()==(n>=new int[]{1,10,50,100}[i]),"journey exact boundary "+n+":"+i);}
        for(int n:new int[]{0,2,3,6,7,29,30,31}){days.clear();for(int i=0;i<n;i++)days.put(today.minusDays(i+5).toString(),1000L);s=new ProgressStats(Collections.emptyList(),days,today);for(int i=0;i<3;i++)ok(Medal.all(s).get(8+i).unlocked()==(n>=new int[]{3,7,30}[i]),"streak exact boundary "+n+":"+i);}
        entries.clear();for(int i=0;i<30000;i++)entries.add(entry(i,i%4,i%2==0?"won":"abandoned",i%3==0,0,0,""));s=new ProgressStats(entries,days,today);ok(s.records.size()==30000&&s.challengeEnded==20000,"backup-sized histories aggregate consistently");
        System.out.println("PASS "+checks+" statistics and medal assertions");
    }
}
