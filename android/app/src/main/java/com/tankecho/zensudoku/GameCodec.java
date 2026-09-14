package com.tankecho.zensudoku;
import org.json.*;import java.util.*;
/** Versioned game serialization, including undo. Import validation runs off the UI thread. */
public final class GameCodec {
 public static JSONObject encode(Game g)throws JSONException{
  JSONObject o=new JSONObject();o.put("givens",Game.encode(g.givens));o.put("solution",Game.encode(g.solution));o.put("board",Game.encode(g.board));o.put("level",g.level);o.put("selected",g.selected);o.put("mistakes",g.mistakes);o.put("hints",g.hints);o.put("ms",g.elapsedMs);o.put("id",g.id);o.put("daily",g.dailyDate);o.put("status",g.status);o.put("pencil",g.pencil);o.put("quickMode",g.quickMode);o.put("activeNumber",g.activeNumber);
  o.put("practice",g.practice);o.put("allCandidates",g.allCandidates);o.put("inspectNumber",g.inspectNumber);o.put("puzzleId",g.puzzleId);o.put("technique",g.technique);
  o.put("notes",new JSONArray(g.notes));o.put("candidates",new JSONArray(g.candidates));o.put("hinted",new JSONArray(g.hinted));
  JSONArray u=new JSONArray();for(int[][] entry:g.undoSnapshots()){JSONArray e=new JSONArray();for(int[] a:entry)e.put(new JSONArray(a));u.put(e);}o.put("undo",u);
  o.put("pendingHintCell",g.pendingHintCell);o.put("pendingHintDigit",g.pendingHintDigit);o.put("pendingHintReason",g.pendingHintReason);o.put("pendingHintRelated",new JSONArray(g.pendingHintRelated));return o;
 }
 private static void require(boolean value)throws JSONException{if(!value)throw new JSONException("存档内容不完整或不合法");}
 private static int bounded(JSONObject o,String key,int fallback,int min,int max)throws JSONException{int n=o.optInt(key,fallback);require(n>=min&&n<=max);return n;}
 private static int[] grid(String text,boolean complete)throws JSONException{require(text.matches(complete?"[1-9]{81}":"[0-9]{81}"));return Game.digits(text);}
 public static Game decode(JSONObject o,boolean fullValidation)throws JSONException{
  int[] givens=grid(o.getString("givens"),false),solution=grid(o.getString("solution"),true),board=grid(o.getString("board"),false);
  for(int[] unit:HintEngine.UNITS){int mask=0;for(int i:unit)mask|=1<<(solution[i]-1);require(mask==511);}
  int level=bounded(o,"level",0,0,3);Game g=new Game(givens,solution,level);g.board=board;g.practice=o.optBoolean("practice");
  for(int i=0;i<81;i++){require(givens[i]==0||givens[i]==solution[i]);require(board[i]==0||board[i]==solution[i]);require(givens[i]==0||board[i]==givens[i]);}
  if(fullValidation)require(Game.countSolutions(givens.clone(),2)==1);
  g.selected=bounded(o,"selected",-1,-1,80);g.mistakes=bounded(o,"mistakes",0,0,g.practice?1000000:3);g.hints=bounded(o,"hints",0,0,3);g.elapsedMs=o.getLong("ms");require(g.elapsedMs>=0&&g.elapsedMs<=315360000000L);
  g.id=o.getString("id");require(g.id.length()>0&&g.id.length()<=128);g.dailyDate=o.optString("daily");if(!g.dailyDate.isEmpty()){try{require(!java.time.LocalDate.parse(g.dailyDate).isAfter(java.time.LocalDate.now()));}catch(Exception e){throw new JSONException("每日挑战日期不合法");}require(!g.practice);}
  g.status=o.getString("status");require(Arrays.asList("playing","won","lost","abandoned").contains(g.status));require(!g.status.equals("won")||g.filled()==81);require(!g.status.equals("playing")||g.filled()<81&&(g.practice||g.mistakes<3));require(!g.status.equals("lost")||!g.practice&&g.mistakes==3);
  g.pencil=o.optBoolean("pencil");g.quickMode=o.optBoolean("quickMode");g.activeNumber=bounded(o,"activeNumber",0,0,9);g.allCandidates=o.optBoolean("allCandidates");g.inspectNumber=bounded(o,"inspectNumber",0,0,9);g.puzzleId=o.optString("puzzleId");g.technique=o.optString("technique");
  JSONArray n=o.getJSONArray("notes"),c=o.getJSONArray("candidates"),h=o.getJSONArray("hinted");require(n.length()==81&&c.length()==81&&h.length()==81);int hinted=0;
  for(int i=0;i<81;i++){g.notes[i]=n.getInt(i);require(g.notes[i]>=0&&g.notes[i]<=511);g.candidates[i]=c.getBoolean(i);g.hinted[i]=h.getBoolean(i);if(g.hinted[i]){hinted++;require(g.givens[i]==0&&g.board[i]==g.solution[i]);}}require(hinted<=g.hints);
  JSONArray u=o.optJSONArray("undo");List<int[][]> history=new ArrayList<>();if(u!=null){require(u.length()<=100);for(int k=0;k<u.length();k++){JSONArray entry=u.getJSONArray(k);require(entry.length()==2||entry.length()==3);int[][] data=new int[entry.length()][81];for(int a=0;a<entry.length();a++){JSONArray values=entry.getJSONArray(a);require(values.length()==81);for(int i=0;i<81;i++){int v=values.getInt(i);data[a][i]=v;if(a==0){require(v==0||v==g.solution[i]);require(g.givens[i]==0||v==g.givens[i]);require(!g.hinted[i]||v==g.solution[i]);}else require(v>=0&&v<=(a==1?511:1));}}history.add(data);}}g.restoreUndo(history);
  g.pendingHintCell=bounded(o,"pendingHintCell",-1,-1,80);g.pendingHintDigit=bounded(o,"pendingHintDigit",0,0,9);g.pendingHintReason=o.optString("pendingHintReason");JSONArray related=o.optJSONArray("pendingHintRelated");if(related!=null){require(related.length()<=81);g.pendingHintRelated=new int[related.length()];for(int i=0;i<related.length();i++){g.pendingHintRelated[i]=related.getInt(i);require(g.pendingHintRelated[i]>=0&&g.pendingHintRelated[i]<81);}}
  if(g.pendingHintCell>=0){require(g.hints>hinted&&g.board[g.pendingHintCell]==0&&g.pendingHintDigit==g.solution[g.pendingHintCell]&&g.pendingHintReason.length()>0&&g.pendingHintReason.length()<30000);}g.normalizeActiveNumber();return g;
 }
}
