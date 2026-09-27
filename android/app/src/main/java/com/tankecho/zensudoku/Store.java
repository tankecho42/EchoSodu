package com.tankecho.zensudoku;

import android.content.*;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Store {
    private final android.content.SharedPreferences prefs;
    private static final ExecutorService WRITER=Executors.newSingleThreadExecutor();
    private static final Map<android.content.SharedPreferences,JSONObject> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    private final AtomicReference<JSONObject> pending=new AtomicReference<>();
    private boolean writing;public volatile String saveError="";
    private String current="",preImport="";public JSONObject seen=new JSONObject();
    public JSONObject records,days;
    public boolean motion=true,haptic=true,quickMode=false;
    public int themeId=0;
    public Store(Context c){
        prefs=c.getSharedPreferences("zen_sudoku_v1",Context.MODE_PRIVATE);
        JSONObject data=CACHE.get(prefs);
        if(data==null){data=new JSONObject();try{data.put("records",parse(prefs.getString("records","{}")));data.put("days",parse(prefs.getString("days","{}")));data.put("seen",parse(prefs.getString("seen","{}")));data.put("current",prefs.getString("current",""));data.put("themeId",prefs.getInt("appearanceDefaults",0)<1?0:prefs.getInt("themeId",0));data.put("motion",prefs.getBoolean("motion",true));data.put("haptic",prefs.getBoolean("haptic",true));data.put("quickMode",prefs.getBoolean("quickMode",false));data.put("preImport",prefs.getString("preImport",""));}catch(JSONException e){throw new IllegalStateException(e);}}
        restore(data);
    }
    private void restore(JSONObject data){records=parse(data.optJSONObject("records").toString());days=parse(data.optJSONObject("days").toString());seen=parse(data.optJSONObject("seen")==null?"{}":data.optJSONObject("seen").toString());current=data.optString("current");preImport=data.optString("preImport");themeId=Math.max(0,Math.min(Palette.NAMES.length-1,data.optInt("themeId")));motion=data.optBoolean("motion",true);haptic=data.optBoolean("haptic",true);quickMode=data.optBoolean("quickMode");}
    private static JSONObject parse(String s){try{return new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    public static String today(){return LocalDate.now().toString();}
    public void addTime(long ms){try{String d=today();JSONObject v=days.optJSONObject(d);if(v==null)v=new JSONObject();v.put("ms",v.optLong("ms")+ms);days.put(d,v);}catch(JSONException e){throw new IllegalStateException(e);}}
    public void save(Game g){
        try {
            if(g!=null){
                JSONObject data=toJson(g);current=data.toString();
                JSONObject r=records.optJSONObject(g.id);if(r==null){r=new JSONObject();r.put("date",today());}
                r.put("id",g.id);r.put("level",g.level);r.put("ms",g.elapsedMs);r.put("mistakes",g.mistakes);r.put("hints",g.hints);r.put("status",g.status);r.put("daily",g.dailyDate);r.put("practice",g.practice);r.put("puzzleId",g.puzzleId);
                if(!g.status.equals("playing")&&!r.has("endDate"))r.put("endDate",today());
                records.put(g.id,r);
            }
            JSONObject snapshot=snapshot();CACHE.put(prefs,snapshot);pending.set(snapshot);queueWrite();
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    private synchronized void queueWrite(){if(writing)return;writing=true;WRITER.execute(()->{try{JSONObject s;while((s=pending.getAndSet(null))!=null){if(CACHE.get(prefs)!=s)continue;boolean ok=prefs.edit().putString("current",s.optString("current")).putString("records",s.optJSONObject("records").toString()).putString("days",s.optJSONObject("days").toString()).putString("seen",s.optJSONObject("seen").toString()).putString("preImport",s.optString("preImport")).putBoolean("motion",s.optBoolean("motion")).putBoolean("haptic",s.optBoolean("haptic")).putBoolean("quickMode",s.optBoolean("quickMode")).putInt("themeId",s.optInt("themeId")).putInt("appearanceDefaults",1).commit();saveError=ok?"":"进度暂未写入，请检查设备剩余空间";}}finally{synchronized(Store.this){writing=false;if(pending.get()!=null)queueWrite();}}});}
    public static void awaitWrites()throws Exception{WRITER.submit(()->{}).get(10,TimeUnit.SECONDS);}
    private JSONObject snapshot()throws JSONException{JSONObject s=new JSONObject();s.put("records",new JSONObject(records.toString()));s.put("days",new JSONObject(days.toString()));s.put("seen",new JSONObject(seen.toString()));s.put("current",current);s.put("themeId",themeId);s.put("motion",motion);s.put("haptic",haptic);s.put("quickMode",quickMode);s.put("preImport",preImport);return s;}
    public Game load(){try{return current.isEmpty()?null:GameCodec.decode(new JSONObject(current),false);}catch(Exception e){saveError="当前对局无法读取，历史记录仍保留";return null;}}
    public static JSONObject toJson(Game g)throws JSONException{return GameCodec.encode(g);}
    // Historical backup identifier is stable across the EchoSudoku display-name change.
    public String exportBackup()throws Exception{JSONObject s=snapshot();s.remove("preImport");String payload=s.toString();JSONObject envelope=new JSONObject();envelope.put("app","EchoSodu");envelope.put("version",1);envelope.put("exportedAt",java.time.Instant.now().toString());envelope.put("payload",payload);envelope.put("sha256",digest(payload));return envelope.toString(2);}
    private static String digest(String s)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder h=new StringBuilder();for(byte b:bytes)h.append(String.format(Locale.ROOT,"%02x",b&255));return h.toString();}
    public static JSONObject validateBackup(String text)throws Exception{
        if(text.length()>8*1024*1024)throw new IllegalArgumentException("存档不能超过 8MB");JSONObject e=new JSONObject(text);if(!e.getString("app").equals("EchoSodu")||e.getInt("version")!=1)throw new IllegalArgumentException("不支持的存档版本");String raw=e.getString("payload");if(!digest(raw).equals(e.getString("sha256")))throw new IllegalArgumentException("存档校验失败，文件可能已损坏");JSONObject s=new JSONObject(raw);JSONObject records=s.getJSONObject("records"),days=s.getJSONObject("days");if(records.length()>30000||days.length()>36500)throw new IllegalArgumentException("存档记录过多");
        String game=s.getString("current");if(!game.isEmpty())GameCodec.decode(new JSONObject(game),true);
        for(Iterator<String> it=records.keys();it.hasNext();){String id=it.next();JSONObject r=records.getJSONObject(id);if(!id.equals(r.getString("id"))||r.getInt("level")<0||r.getInt("level")>3||r.getLong("ms")<0||r.getLong("ms")>315360000000L||r.getInt("hints")<0||r.getInt("hints")>3||r.getInt("mistakes")<0||!Arrays.asList("won","playing","lost","abandoned").contains(r.getString("status")))throw new IllegalArgumentException("对局记录不合法");LocalDate.parse(r.getString("date"));if(r.has("endDate"))LocalDate.parse(r.getString("endDate"));if(!r.optString("daily").isEmpty())LocalDate.parse(r.getString("daily"));}
        for(Iterator<String> it=days.keys();it.hasNext();){String d=it.next();LocalDate.parse(d);long ms=days.getJSONObject(d).getLong("ms");if(ms<0||ms>315360000000L)throw new IllegalArgumentException("每日时长不合法");}
        if(s.getInt("themeId")<0||s.getInt("themeId")>=Palette.NAMES.length)throw new IllegalArgumentException("主题不合法");s.getBoolean("motion");s.getBoolean("haptic");s.getBoolean("quickMode");JSONObject seen=s.optJSONObject("seen");if(seen==null)s.put("seen",new JSONObject());else for(Iterator<String> it=seen.keys();it.hasNext();){String k=it.next();if(!k.matches("[0-3]")||seen.getJSONArray(k).length()>1000)throw new IllegalArgumentException("题目记录不合法");}s.remove("preImport");return s;
    }
    public void importBackup(JSONObject validated)throws Exception{String recovery=exportBackup();restore(validated);preImport=recovery;save(null);}
    public boolean hasRecovery(){return !preImport.isEmpty();}
    public String recovery(){return preImport;}
    public int choosePuzzle(int level,JSONArray pool,Random rng)throws JSONException{String key=""+level;JSONArray used=seen.optJSONArray(key);if(used==null)used=new JSONArray();Set<String> set=new HashSet<>();for(int i=0;i<used.length();i++)set.add(used.getString(i));List<Integer> available=new ArrayList<>();for(int i=0;i<pool.length();i++)if(!set.contains(pool.getJSONObject(i).getString("id")))available.add(i);if(available.isEmpty()){used=new JSONArray();for(int i=0;i<pool.length();i++)available.add(i);}int index=available.get(rng.nextInt(available.size()));used.put(pool.getJSONObject(index).getString("id"));seen.put(key,used);return index;}
    public int cleanWins(boolean noHints,boolean noErrors){int n=0;for(JSONObject r:history())if(!r.optBoolean("practice")&&r.optString("status").equals("won")&&(!noHints||r.optInt("hints")==0)&&(!noErrors||r.optInt("mistakes")==0))n++;return n;}
    public int practiceWins(){int n=0;for(JSONObject r:history())if(r.optBoolean("practice")&&r.optString("status").equals("won"))n++;return n;}
    public long best(int level,String excludeId){long best=Long.MAX_VALUE;for(JSONObject r:history())if(!r.optBoolean("practice")&&r.optInt("level")==level&&!r.optString("id").equals(excludeId)&&r.optString("status").equals("won"))best=Math.min(best,r.optLong("ms"));return best;}
    public long[] trend(int level){long[] sums=new long[30],counts=new long[30];LocalDate first=LocalDate.now().minusDays(29);for(JSONObject r:history())if(!r.optBoolean("practice")&&r.optInt("level")==level&&r.optString("status").equals("won")){try{int d=(int)java.time.temporal.ChronoUnit.DAYS.between(first,LocalDate.parse(r.optString("endDate",r.optString("date"))));if(d>=0&&d<30){sums[d]+=r.optLong("ms");counts[d]++;}}catch(Exception ignored){}}for(int i=0;i<30;i++)sums[i]=counts[i]==0?-1:sums[i]/counts[i];return sums;}
    public List<JSONObject> history(){List<JSONObject>a=new ArrayList<>();Iterator<String> keys=records.keys();while(keys.hasNext())a.add(records.optJSONObject(keys.next()));a.sort((x,y)->y.optString("endDate",y.optString("date")).compareTo(x.optString("endDate",x.optString("date"))));return a;}
    public int wins(){int n=0;for(JSONObject r:history())if(!r.optBoolean("practice")&&r.optString("status").equals("won"))n++;return n;}
    public ProgressStats statistics(){
        List<ProgressStats.Entry> entries=new ArrayList<>();
        for(JSONObject r:history())entries.add(new ProgressStats.Entry(r.optString("id"),r.optString("endDate",r.optString("date")),r.optString("status"),r.optString("daily"),r.optInt("level"),r.optInt("hints"),r.optInt("mistakes"),r.optLong("ms"),r.optBoolean("practice")));
        Map<String,Long> times=new HashMap<>();for(Iterator<String> i=days.keys();i.hasNext();){String d=i.next();times.put(d,days.optJSONObject(d).optLong("ms"));}
        return new ProgressStats(entries,times,LocalDate.now());
    }
    public int ended(){int n=0;for(JSONObject r:history())if(!r.optBoolean("practice")&&!r.optString("status").equals("playing"))n++;return n;}
    public long totalTime(){long n=0;Iterator<String>i=days.keys();while(i.hasNext())n+=days.optJSONObject(i.next()).optLong("ms");return n;}
    public int dayWins(String day){int n=0;for(JSONObject r:history())if(r.optString("status").equals("won")&&r.optString("endDate",r.optString("date")).equals(day))n++;return n;}
    public boolean dailyWon(String day){for(JSONObject r:history())if(r.optString("daily").equals(day)&&r.optString("status").equals("won"))return true;return false;}
    public int streak(){int n=0;LocalDate d=LocalDate.now();if(!active(d.toString()))d=d.minusDays(1);while(active(d.toString())){n++;d=d.minusDays(1);}return n;}
    public boolean active(String date){JSONObject v=days.optJSONObject(date);return v!=null&&v.optLong("ms")>=1000;}
}
