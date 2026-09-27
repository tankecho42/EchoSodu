package com.tankecho.zensudoku;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
    private Palette palette=Palette.of(0);
    private int BG,INK,MUTED,GREEN,PALE,DARK,LIME,WHITE,LINE,ON_ACCENT,ON_HERO,HERO_MUTED;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Store store;private Game game;private JSONArray bank,legacyBank;
    private boolean relaxed=false,compact=false,ioBusy=false;private int trendLevel=0;
    private final java.util.concurrent.ExecutorService io=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final int EXPORT_SAVE=701,IMPORT_SAVE=702;
    private TextView allCandidateTool;
    private String page="home";private boolean paused=false,foreground=false;
    private long lastTick,lastSave;private String shownSaveError="";
    private LinearLayout root,content;private BoardView boardView;
    private TextView timerLabel,mistakesLabel,hintsLabel,progressLabel,noteTool,candidateTool,pauseButton,hintTool,classicModeButton,quickModeButton,modeLabel,inputGuide;
    private final TextView[] numberKeys=new TextView[9];private FrameLayout boardFrame;private View pauseCover;
    private int chosenLevel=0;
    private int setupStep=0,statsTab=0,historyLimit=20;
    private static final String AUTHOR="Tank",PROJECT_URL="https://github.com/tankecho42/EchoSodu";
    private AlertDialog medalDialog,startConfirmationDialog;
    private VictoryOverlay victoryOverlay;private Runnable pendingResult;private Game pendingResultGame;private AlertDialog settlementDialog,hintDialog,calendarDialog;
    private final Runnable ticker=new Runnable(){public void run(){tick();handler.postDelayed(this,250);}};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        store=new Store(this);game=store.load();if(game!=null)chosenLevel=game.level;
        try(InputStream in=getAssets().open("puzzles.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bank=new JSONArray(out.toString("UTF-8"));}catch(Exception e){throw new IllegalStateException("Puzzle bank unavailable",e);}
        if(state!=null){page=state.getString("page","home");paused=state.getBoolean("paused",false);setupStep=state.getInt("setupStep",0);statsTab=state.getInt("statsTab",0);chosenLevel=state.getInt("chosenLevel",chosenLevel);relaxed=state.getBoolean("relaxed",false);}
        if(page.equals("profile"))page="stats";
        try(InputStream in=getAssets().open("puzzles_legacy.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);legacyBank=new JSONArray(out.toString("UTF-8"));}catch(Exception e){throw new IllegalStateException(e);}
        render();handler.post(ticker);
    }
    @Override protected void onResume(){super.onResume();foreground=true;setEchoMotion(root,store.motion);if(medalDialog!=null&&medalDialog.getWindow()!=null)setEchoMotion(medalDialog.getWindow().getDecorView(),store.motion);lastTick=SystemClock.elapsedRealtime();if(pendingResultGame==game&&game!=null&&!game.status.equals("playing")&&page.equals("game"))scheduleResult(350);}
    @Override protected void onPause(){tick();foreground=false;setEchoMotion(root,false);UiMotion.clearTree(root);if(medalDialog!=null&&medalDialog.getWindow()!=null){setEchoMotion(medalDialog.getWindow().getDecorView(),false);UiMotion.clearTree(medalDialog.getWindow().getDecorView());}stopVictoryVisuals();if(pendingResult!=null){handler.removeCallbacks(pendingResult);pendingResult=null;}store.save(game);super.onPause();}
    @Override protected void onDestroy(){setEchoMotion(root,false);UiMotion.clearTree(root);cancelCompletion();handler.removeCallbacks(ticker);if(settlementDialog!=null)settlementDialog.dismiss();if(hintDialog!=null)hintDialog.dismiss();if(medalDialog!=null)medalDialog.dismiss();if(startConfirmationDialog!=null)startConfirmationDialog.dismiss();if(calendarDialog!=null)calendarDialog.dismiss();io.shutdown();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle o){o.putString("page",page);o.putBoolean("paused",paused);o.putInt("setupStep",setupStep);o.putInt("statsTab",statsTab);o.putInt("chosenLevel",chosenLevel);o.putBoolean("relaxed",relaxed);store.save(game);super.onSaveInstanceState(o);}
    @Override public void onBackPressed(){if(page.equals("setup")&&setupStep>0){setupStep--;render();}else if(!page.equals("home"))navigate("home");else super.onBackPressed();}
    private void tick(){
        if(store!=null&&!store.saveError.isEmpty()&&!store.saveError.equals(shownSaveError)){shownSaveError=store.saveError;tip(shownSaveError);}
        long now=SystemClock.elapsedRealtime(),delta=lastTick==0?0:Math.max(0,now-lastTick);lastTick=now;
        if(foreground&&page.equals("game")&&game!=null&&game.status.equals("playing")&&!paused){game.elapsedMs+=delta;store.addTime(delta);if(timerLabel!=null)timerLabel.setText(time(game.elapsedMs));if(now-lastSave>5000){store.save(game);lastSave=now;}}
    }
    private void navigate(String next){tick();store.save(game);page=next.equals("profile")?"stats":next;paused=false;render();if(!page.equals("game"))UiMotion.enter(content,store.motion);}
    private void render(){
        cancelCompletion();UiMotion.clearTree(root);applyPalette();compact=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density<740;timerLabel=null;boardView=null;
        root=column();root.setBackgroundColor(BG);setContentView(root);
        if(page.equals("game")&&game==null)page="home";
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setClipToPadding(false);
        content=column();content.setClipChildren(false);content.setPadding(dp(22),dp(20),dp(22),dp(18));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page.equals("home"))home();else if(page.equals("game"))play();else if(page.equals("setup"))setup();else if(page.equals("settings"))settings();else statistics();
        navigation();
    }
    private void home(){
        LinearLayout brand=row();brand.addView(text("EchoSudoku",25,INK,true),new LinearLayout.LayoutParams(0,-2,1));brand.addView(text(LocalDate.now().format(DateTimeFormatter.ofPattern("MM / dd")),12,MUTED,false));content.addView(brand);
        add(content,kicker("A LITTLE SPACE TO THINK"),compact?22:32,8);
        add(content,text("下一格，一起想。",compact?28:32,INK,true),0,6);
        add(content,text("让数字归位，也让思绪慢下来。",13,MUTED,false),0,0);
        boolean continuing=game!=null&&game.status.equals("playing");
        LinearLayout main=card(WHITE,24);main.addView(text(continuing?"还留着你的那一局":"今天，从一份专注开始",17,INK,true));
        add(main,text(continuing?Game.LEVELS[game.level]+(game.practice?" · 练习":" · 挑战")+"   /   "+time(game.elapsedMs):"选一种节奏，解一盘数独。",12,MUTED,false),8,18);
        TextView primary=button(continuing?"继续游戏    →":"开始新游戏    →",GREEN,ON_ACCENT,16);primary.setContentDescription(continuing?"继续游戏":"开始新游戏");add(main,primary,0,0,52);primary.setOnClickListener(v->{if(continuing)navigate("game");else openSetup();});
        if(continuing){TextView fresh=button("另开一局",WHITE,GREEN,13);fresh.setContentDescription("开始新游戏");add(main,fresh,8,0,44);fresh.setOnClickListener(v->openSetup());}
        add(content,new EchoPerchLayout(this,main,store.motion,compact?68:82),compact?2:10,0);
        LinearLayout daily=card(PALE,20);daily.setPadding(dp(18),dp(16),dp(18),dp(16));LinearLayout line=row();LinearLayout copy=column();copy.addView(kicker("DAILY RITUAL"));add(copy,text("每日一题",17,INK,true),6,5);copy.addView(text(store.dailyWon(Store.today())?"今日已完成 · 明天再见":"每天同一题，留下一点坚持。",11,MUTED,false));line.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        TextView go=button(store.dailyWon(Store.today())?"日历":"开始",WHITE,GREEN,13);go.setContentDescription("今日挑战");line.addView(go,new LinearLayout.LayoutParams(dp(64),dp(46)));go.setOnClickListener(v->{if(store.dailyWon(Store.today()))showDailyCalendar(YearMonth.now());else requestStart(1,true);});daily.addView(line);
        TextView calendar=text("查看挑战日历   ↗",11,GREEN,false);calendar.setMinHeight(dp(44));calendar.setContentDescription("每日挑战日历");calendar.setOnClickListener(v->showDailyCalendar(YearMonth.now()));daily.addView(calendar);add(content,daily,14,0);
        LinearLayout summary=row();metricInline(summary,"今日完成",store.dayWins(Store.today())+" 局");metricInline(summary,"今日专注",minutes(dayTime(Store.today()))+" 分");metricInline(summary,"连续游玩",store.streak()+" 天");summary.setContentDescription("查看详细统计");summary.setOnClickListener(v->{statsTab=0;navigate("stats");});UiMotion.press(summary,()->store.motion);add(content,summary,compact?20:28,0);
    }
    private void openSetup(){setupStep=0;navigate("setup");}
    private void setup(){
        LinearLayout top=row();TextView back=roundIcon("‹","返回上一步");top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(44)));back.setOnClickListener(v->onBackPressed());TextView step=kicker(setupStep==0?"01 / 02     YOUR PACE":"02 / 02     YOUR CHALLENGE");step.setPadding(dp(14),0,0,0);top.addView(step);content.addView(top);
        add(content,text(setupStep==0?"今天，想怎么解？":"选一个舒服的难度",28,INK,true),26,10);add(content,text(setupStep==0?"先选节奏，再挑难度。":relaxed?"不限错练习 · 每一步都可以慢慢想。":"三错挑战 · 留意每一个落下的数字。",13,MUTED,false),0,24);
        if(setupStep==0){
            View challenge=choiceCard("01","三错挑战","每局最多 3 次错误，挑战自己的推理。",!relaxed,"三错挑战模式",()->{relaxed=false;setupStep=1;render();UiMotion.enter(content,store.motion);});add(content,challenge,0,14);
            View practice=choiceCard("02","不限错练习","保留计时与提示，填错也能继续探索。",relaxed,"不限错练习模式",()->{relaxed=true;setupStep=1;render();UiMotion.enter(content,store.motion);});add(content,practice,0,20);
            add(content,text("两种模式都能积累专注时间与专属奖章。\n挑战成绩和练习记录分别统计。",12,MUTED,false),8,0);
        }else{
            String[] desc={"从单一候选开始，轻轻进入状态。","多看一行、一列，发现隐藏的答案。","组合线索，给推理多一点空间。","给耐心与洞察力一次完整的挑战。"};
            for(int i=0;i<4;i++){final int level=i;add(content,choiceCard("0"+(i+1),Game.LEVELS[i],desc[i],chosenLevel==i,"选择"+Game.LEVELS[i]+"难度",()->{chosenLevel=level;haptic();render();}),0,10);}
            TextView start=button("开始"+Game.LEVELS[chosenLevel]+(relaxed?"练习":"挑战")+"    →",GREEN,ON_ACCENT,15);start.setContentDescription("确认开始游戏");start.setOnClickListener(v->requestStart(chosenLevel,false));
            if(compact){LinearLayout footer=column();footer.setPadding(dp(22),dp(8),dp(22),dp(6));add(footer,start,0,0,52);TextView note=text("每局 3 次提示 · 进度自动保存",10,MUTED,false);note.setGravity(Gravity.CENTER);add(footer,note,8,0);root.addView(footer,new LinearLayout.LayoutParams(-1,-2));}else{add(content,start,14,0,54);add(content,text("每局 3 次提示 · 进度自动保存",11,MUTED,false),12,0);}
        }
    }
    private View choiceCard(String number,String title,String description,boolean selected,String label,Runnable action){
        LinearLayout tile=card(selected?PALE:WHITE,20);tile.setPadding(dp(17),dp(18),dp(17),dp(18));tile.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf((GREEN&0xffffff)|0x22000000),shape(selected?PALE:WHITE,20,selected?GREEN:LINE),null));
        LinearLayout line=row();TextView n=text(number,12,GREEN,true);line.addView(n,new LinearLayout.LayoutParams(dp(34),-2));LinearLayout words=column();words.addView(text(title,17,INK,true));add(words,text(description,11,MUTED,false),7,0);line.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView check=text(selected?"✓":"›",18,GREEN,false);check.setGravity(Gravity.RIGHT);line.addView(check,new LinearLayout.LayoutParams(dp(25),-2));tile.addView(line);tile.setSelected(selected);tile.setFocusable(true);tile.setContentDescription(label+(selected?"，已选择":""));tile.setOnClickListener(v->action.run());UiMotion.press(tile,()->store.motion);return tile;
    }

    private void requestStart(int level,boolean daily){requestGame(level,daily?Store.today():"",!daily&&relaxed);}
    private void requestGame(int level,String date,boolean practice){
        if(!date.isEmpty()&&store.dailyWon(date)){tip("这一天的挑战已完成");return;}
        if(!date.isEmpty()&&game!=null&&game.status.equals("playing")&&game.dailyDate.equals(date)){navigate("game");return;}
        if(game!=null&&game.status.equals("playing")){tick();boolean wasPaused=paused;paused=true;AlertDialog confirmation=dialogBuilder().setTitle("开始新的专注？").setMessage("当前对局将结束并记录为未完成。也可以返回继续这盘游戏。").setNegativeButton("继续当前对局",(d,w)->{paused=false;navigate("game");}).setPositiveButton("开始新游戏",(d,w)->{game.status="abandoned";store.save(game);startGame(level,date,practice);}).setOnCancelListener(d->{paused=wasPaused;}).create();startConfirmationDialog=confirmation;confirmation.setOnDismissListener(d->{if(startConfirmationDialog==confirmation)startConfirmationDialog=null;});confirmation.show();}else startGame(level,date,practice);
    }
    private void start(int level,boolean daily){startGame(level,daily?Store.today():"",!daily&&relaxed);}
    private void startGame(int level,String date,boolean practice){
        try{boolean daily=!date.isEmpty();long seed=daily?LocalDate.parse(date).toEpochDay()*7919L+215:System.nanoTime();Random rng=new Random(seed);
            JSONArray pool=(daily&&!LocalDate.parse(date).isAfter(LocalDate.of(2026,9,14))?legacyBank:bank).getJSONArray(level);
            JSONObject puzzle=pool.getJSONObject(daily?rng.nextInt(pool.length()):store.choosePuzzle(level,pool,rng));int[][] transformed=Game.transform(Game.digits(puzzle.getString("puzzle")),Game.digits(puzzle.getString("solution")),seed);
            game=new Game(transformed[0],transformed[1],level);game.quickMode=store.quickMode;game.practice=practice;game.dailyDate=date;game.puzzleId=puzzle.optString("id","legacy-"+level);game.technique=puzzle.optString("technique",Game.LEVELS[level]);chosenLevel=level;paused=false;page="game";lastTick=SystemClock.elapsedRealtime();store.save(game);render();
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    private void play(){
        content.setPadding(dp(20),dp(compact?8:18),dp(20),dp(compact?8:22));
        LinearLayout head=row();TextView back=roundIcon("‹","返回游戏中心");head.addView(back,new LinearLayout.LayoutParams(dp(42),dp(42)));back.setOnClickListener(v->navigate("home"));
        LinearLayout labels=column();LinearLayout brand=row();brand.setGravity(Gravity.CENTER);Icon sudokuIcon=new Icon(this,1,GREEN);sudokuIcon.setContentDescription("数独");brand.addView(sudokuIcon,new LinearLayout.LayoutParams(dp(23),dp(23)));TextView name=text("EchoSudoku",22,INK,true);name.setPadding(dp(7),0,0,0);brand.addView(name);labels.addView(brand);TextView difficulty=text(Game.LEVELS[game.level]+(game.practice?" · 不限错练习":game.dailyDate.isEmpty()?" · 三错挑战":" · "+game.dailyDate.substring(5)+" 每日挑战"),11,MUTED,false);difficulty.setGravity(Gravity.CENTER);add(labels,difficulty,4,0);head.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        pauseButton=roundIcon(paused?"▷":"Ⅱ","暂停游戏");head.addView(pauseButton,new LinearLayout.LayoutParams(dp(42),dp(42)));pauseButton.setOnClickListener(v->togglePause());content.addView(head);
        LinearLayout modeRow=row();LinearLayout toggle=row();toggle.setPadding(dp(3),dp(3),dp(3),dp(3));toggle.setBackground(shape(PALE,14,0));
        classicModeButton=button("常规",WHITE,GREEN,13);classicModeButton.setContentDescription("常规模式");classicModeButton.setOnClickListener(v->setQuickMode(false));toggle.addView(classicModeButton,new LinearLayout.LayoutParams(dp(68),dp(34)));
        quickModeButton=button("ϟ 快速",PALE,GREEN,13);quickModeButton.setContentDescription("快速模式");quickModeButton.setOnClickListener(v->setQuickMode(true));toggle.addView(quickModeButton,new LinearLayout.LayoutParams(dp(78),dp(34)));modeRow.addView(toggle);
        modeLabel=text("",11,MUTED,false);modeLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);modeRow.addView(modeLabel,new LinearLayout.LayoutParams(0,-2,1));add(content,modeRow,compact?6:22,compact?2:8);
        LinearLayout indicators=row();mistakesLabel=text("",12,MUTED,false);indicators.addView(mistakesLabel,new LinearLayout.LayoutParams(0,dp(compact?28:38),1));timerLabel=text(time(game.elapsedMs),17,INK,true);timerLabel.setGravity(Gravity.CENTER);indicators.addView(timerLabel,new LinearLayout.LayoutParams(0,dp(compact?28:38),1));hintsLabel=text("",12,MUTED,false);hintsLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);indicators.addView(hintsLabel,new LinearLayout.LayoutParams(0,dp(compact?28:38),1));add(content,indicators,0,compact?3:7);
        boardFrame=new FrameLayout(this);boardFrame.setClipChildren(false);boardFrame.setClipToPadding(false);content.setClipChildren(false);
        boardView=new BoardView(this,game,palette,this::selectCell);
        int width=Math.min(getResources().getDisplayMetrics().widthPixels-dp(48),dp(386));
        int heightCap=Math.max(dp(216),getResources().getDisplayMetrics().heightPixels-dp(compact?350:440));width=Math.min(width,heightCap);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(width,width,Gravity.CENTER);boardFrame.addView(boardView,bp);
        LinearLayout overlay=column();overlay.setGravity(Gravity.CENTER);overlay.setBackground(shape(PALE,8,LINE));int restSize=Math.min(dp(136),width-dp(160));overlay.addView(echo(EchoArtView.REST),new LinearLayout.LayoutParams(restSize,restSize));add(overlay,text("Echo 也在歇一会儿",21,INK,true),12,10);overlay.addView(text("计时已暂停，进度已保存",12,MUTED,false));TextView resume=button("继续专注",GREEN,ON_ACCENT,15);LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(dp(150),dp(46));op.topMargin=dp(24);overlay.addView(resume,op);resume.setOnClickListener(v->togglePause());centerTexts(overlay);pauseCover=overlay;boardFrame.addView(overlay,bp);content.addView(boardFrame,new LinearLayout.LayoutParams(-1,width));
        LinearLayout under=row();progressLabel=text("",11,MUTED,false);under.addView(progressLabel,new LinearLayout.LayoutParams(0,dp(compact?24:32),1));allCandidateTool=text("全盘候选",11,GREEN,false);allCandidateTool.setContentDescription("全盘候选数");allCandidateTool.setPadding(dp(8),0,dp(8),0);under.addView(allCandidateTool);allCandidateTool.setOnClickListener(v->{if(active()){game.toggleAllCandidates();updateGame();store.save(game);}});TextView settings=text("说明 ↗",11,GREEN,false);settings.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);under.addView(settings);settings.setOnClickListener(v->help());add(content,under,compact?0:5,compact?0:5);
        LinearLayout tools=row();TextView undo=tool(tools,"↶","撤销");undo.setOnClickListener(v->{if(active()&&game.undo()){updateGame();store.save(game);}else tip("没有可撤销的步骤");});
        TextView erase=tool(tools,"⌫","擦除");erase.setOnClickListener(v->{if(active()&&game.erase()){haptic();updateGame();store.save(game);}else tip("请选择可擦除的格子");});
        noteTool=tool(tools,"✎","笔记");noteTool.setOnClickListener(v->{if(active()){game.pencil=!game.pencil;updateGame();store.save(game);}});
        candidateTool=tool(tools,"⁙","候选数");candidateTool.setOnClickListener(v->{if(active()&&game.toggleCandidates()){updateGame();store.save(game);}else tip(game.quickMode&&game.activeNumber>0?"再次点选中数字取消快填，再选空格查看候选数":"先选一个空格，再查看候选数");});
        hintTool=tool(tools,"✧","提示");hintTool.setContentDescription("使用提示");hintTool.setOnClickListener(v->hint());add(content,tools,compact?4:8,0);
        LinearLayout keys=row();for(int i=1;i<=9;i++){final int n=i;TextView key=button(String.valueOf(i),PALE,GREEN,25);key.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));key.setContentDescription("输入数字 "+i);key.setPadding(0,0,0,0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(compact?48:56),1);if(i>1)lp.leftMargin=dp(4);keys.addView(key,lp);numberKeys[i-1]=key;key.setOnClickListener(v->enter(n));key.setOnLongClickListener(v->{if(active()){game.inspect(n);updateGame();tip("查看候选 "+n+"，再次长按取消");}return true;});}add(content,keys,compact?8:21,0);
        inputGuide=text("",11,MUTED,false);inputGuide.setGravity(Gravity.CENTER);inputGuide.setLineSpacing(dp(5),1);if(!compact)add(content,inputGuide,14,0);
        updateGame();
        if(!game.status.equals("playing"))scheduleResult(350);
    }
    private void setQuickMode(boolean enabled){
        if(!active()||game.quickMode==enabled)return;
        game.setQuickMode(enabled);game.inspectNumber=0;store.quickMode=enabled;haptic();updateGame();store.save(game);
    }
    private void selectCell(int at){
        if(!active())return;
        int number=game.activeNumber;boolean[] before=game.completedUnits();int result=game.selectCell(at);
        if(result==0){haptic();updateGame();store.save(game);}else handleMove(result,number,before);
    }
    private boolean active(){return !ioBusy&&game!=null&&!paused&&game.status.equals("playing");}
    private void updateGame(){
        if(boardView==null)return;game.normalizeActiveNumber();
        mistakesLabel.setText(game.practice?"错误 "+game.mistakes+" · 不限":"错误  "+game.mistakes+" / 3");mistakesLabel.setTextColor(game.mistakes>0?palette.error:MUTED);
        hintsLabel.setText("提示剩余  "+(3-game.hints));progressLabel.setText("已填 "+game.filled()+" / 81"+(game.pencil?"    · 笔记模式":""));
        noteTool.setTextColor(game.pencil?GREEN:MUTED);noteTool.setBackground(shape(game.pencil?PALE:BG,12,0));
        boolean c=game.selected>=0&&game.showCandidates(game.selected);candidateTool.setTextColor(c?GREEN:MUTED);candidateTool.setBackground(shape(c?PALE:BG,12,0));
        classicModeButton.setBackground(shape(game.quickMode?PALE:WHITE,11,0));classicModeButton.setTextColor(game.quickMode?MUTED:GREEN);classicModeButton.setSelected(!game.quickMode);
        quickModeButton.setBackground(shape(game.quickMode?GREEN:PALE,11,0));quickModeButton.setTextColor(game.quickMode?ON_ACCENT:MUTED);quickModeButton.setSelected(game.quickMode);
        modeLabel.setText(game.inspectNumber>0?"查看候选 "+game.inspectNumber:game.quickMode?"先选数字 · 再点空格":"先点空格 · 再选数字");
        allCandidateTool.setText(game.allCandidates?"隐藏全盘候选":"全盘候选");hintTool.setAlpha(game.hints>=3&&game.pendingHintCell<0?.35f:1);hintTool.setTextColor(GREEN);
        for(int i=1;i<=9;i++){boolean chosen=game.quickMode&&game.activeNumber==i;numberKeys[i-1].setBackground(shape(chosen?GREEN:PALE,14,0));numberKeys[i-1].setTextColor(chosen?ON_ACCENT:GREEN);numberKeys[i-1].setSelected(chosen);boolean disabled=game.quickMode&&!game.numberAvailable(i);numberKeys[i-1].setEnabled(!disabled);numberKeys[i-1].setAlpha(disabled?.28f:1);numberKeys[i-1].setContentDescription("输入数字 "+i+(disabled?"，已完成":""));}
        inputGuide.setText(game.quickMode?(game.activeNumber>0?"已选 "+game.activeNumber+" · 点空格"+(game.pencil?"记录笔记":"连续填入")+"，再次点数字可取消":"选择下方数字，高亮同数并连续填格"):(game.pencil?"笔记模式 · 在空格记下你的推测":"慢慢来，每一格都有它的位置。"));
        boardView.setVisibility(paused?View.INVISIBLE:View.VISIBLE);pauseCover.setVisibility(paused?View.VISIBLE:View.GONE);pauseButton.setText(paused?"▷":"Ⅱ");pauseButton.setContentDescription(paused?"继续游戏":"暂停游戏");boardView.refresh();
    }
    private void enter(int n){
        if(!active())return;game.inspectNumber=0;if(game.quickMode){if(game.selectNumber(n)){haptic();updateGame();store.save(game);}return;}if(game.selected<0){tip("先选择一个格子");return;}
        boolean[] before=game.completedUnits();int result=game.enter(n);
        handleMove(result,n,before);
    }
    private void handleMove(int result,int n,boolean[] before){
        if(result==0){tip("这是已确定的数字，请选择空格");return;}
        haptic();if(result==2||result==5){boardView.wrong(n,store.motion);if(result==2)tip(game.practice?"这格还需再想一想，练习不限错误次数":"再想一想，还能填错 "+(3-game.mistakes)+" 次");}
        else if(result==1||result==4)boardView.celebrate(before,store.motion);
        updateGame();store.save(game);if(result==4)completeVictory();else if(result==5)scheduleResult(750);
    }
    private void hint(){
        if(!active())return;
        if(game.pendingHintCell<0){if(game.hints>=3){tip("本局的 3 次提示已用完");return;}HintEngine.Plan plan=HintEngine.find(game.board,game.selected);if(plan==null){tip("这一步需要更复杂的推理，暂未找到可解释提示；本次不扣次数");return;}if(!game.reserveHint(plan))return;store.save(game);}
        showHintExplanation();
    }
    private void showHintExplanation(){
        final Game target=game;tick();paused=true;updateGame();
        LinearLayout panel=card(BG,18);LinearLayout heading=row();heading.addView(echo(EchoArtView.THINK),new LinearLayout.LayoutParams(dp(52),dp(52)));TextView title=text("Echo 陪你想一步",18,INK,true);heading.addView(title);panel.addView(heading);
        add(panel,text("第"+(target.pendingHintCell/9+1)+"行 · 第"+(target.pendingHintCell%9+1)+"列  /  本次已计 1 次提示",11,MUTED,false),6,8);
        BoardView preview=new BoardView(this,target,palette,i->{});preview.showHint(target.pendingHintCell,target.pendingHintRelated);int side=Math.min(dp(230),getResources().getDisplayMetrics().widthPixels-dp(100));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(side,side);bp.gravity=Gravity.CENTER;panel.addView(preview,bp);
        add(panel,text(target.pendingHintReason,13,INK,false),12,8);add(panel,text("因此，这一格可以填 "+target.pendingHintDigit+"。",14,GREEN,true),0,0);
        ScrollView scroll=new ScrollView(this);scroll.addView(panel);AlertDialog d=dialogBuilder().setView(scroll).setPositiveButton("填入这一格",(dialog,which)->acceptHint(target)).setNegativeButton("先自己想",null).create();hintDialog=d;d.setOnDismissListener(dialog->{hintDialog=null;if(game==target){paused=false;lastTick=SystemClock.elapsedRealtime();updateGame();}});d.show();if(d.getWindow()!=null){d.getWindow().setBackgroundDrawable(shape(BG,22,0));d.getWindow().setLayout(-1,-2);panel.measure(View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels-dp(64),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));ViewGroup.LayoutParams sp=scroll.getLayoutParams();sp.height=Math.min(panel.getMeasuredHeight(),getResources().getDisplayMetrics().heightPixels-dp(180));scroll.setLayoutParams(sp);d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(GREEN);d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(GREEN);}
    }
    private void acceptHint(Game target){if(game!=target)return;boolean[] before=game.completedUnits();if(target.applyPendingHint()){paused=false;haptic();boardView.celebrate(before,store.motion);updateGame();store.save(game);if(game.status.equals("won"))completeVictory();}}
    private void togglePause(){if(game==null||!game.status.equals("playing"))return;tick();paused=!paused;lastTick=SystemClock.elapsedRealtime();updateGame();store.save(game);}
    private void help(){
        tick();startActivity(new Intent(this,TutorialActivity.class).putExtra("theme",palette.id).putExtra("motion",store.motion));
    }
    private void resultDialog(){
        if(game==null||game.status.equals("playing")||!page.equals("game")||!foreground||isFinishing()||isDestroyed()||(settlementDialog!=null&&settlementDialog.isShowing()))return;
        pendingResultGame=null;stopVictoryVisuals();
        boolean win=game.status.equals("won");LinearLayout panel=card(BG,24);panel.setGravity(Gravity.CENTER);
        panel.addView(echo(win?EchoArtView.CELEBRATE:EchoArtView.RETRY),new LinearLayout.LayoutParams(dp(140),dp(140)));add(panel,text(win?"秩序，恰好完成。":"休息一下，再试一次。",22,INK,true),10,12);
        TextView summary=text((win?"你完成了这份专注。":"本局累计填错 3 次，挑战结束。")+"\n\n"+Game.LEVELS[game.level]+"   ·   "+time(game.elapsedMs)+"\n提示 "+game.hints+" / 3     错误 "+game.mistakes+" / 3",14,MUTED,false);summary.setGravity(Gravity.CENTER);summary.setLineSpacing(dp(6),1);panel.addView(summary);
        if(win){String note=game.practice?"练习完成 · 已计入独立练习统计":store.best(game.level,game.id)==Long.MAX_VALUE?"第一份 "+Game.LEVELS[game.level]+" 通关纪录":game.elapsedMs<store.best(game.level,game.id)?"刷新 "+Game.LEVELS[game.level]+" 个人纪录 ✧":"又完成了一次挑战";add(panel,text(note,13,GREEN,true),12,0);if(!game.practice&&game.hints==0)add(panel,text(game.mistakes==0?"零错误 · 无提示通关":"无提示通关",12,GREEN,false),6,0);}
        AlertDialog dialog=dialogBuilder().setView(panel).setCancelable(false).create();settlementDialog=dialog;dialog.setOnDismissListener(d->{if(settlementDialog==dialog)settlementDialog=null;});
        centerTexts(panel);TextView next=button(win?"开启下一局":"重试本题",GREEN,ON_ACCENT,15);add(panel,next,24,10,50);next.setOnClickListener(v->{dialog.dismiss();if(win)startGame(game.level,"",game.practice);else{Game old=game;game=new Game(old.givens,old.solution,old.level);game.quickMode=store.quickMode;game.dailyDate=old.dailyDate;game.practice=old.practice;game.puzzleId=old.puzzleId;game.technique=old.technique;paused=false;lastTick=SystemClock.elapsedRealtime();store.save(game);render();}});
        TextView back=button("回到游戏中心",PALE,GREEN,14);add(panel,back,0,0,46);back.setOnClickListener(v->{dialog.dismiss();navigate("home");});dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    private void statistics(){
        ProgressStats stats=store.statistics();
        content.addView(kicker("A RECORD OF YOUR PATIENCE"));add(content,text("每一份专注，都有回响。",26,INK,true),10,8);add(content,text("在这里，看见自己的节奏。",12,MUTED,false),0,20);
        LinearLayout tabs=row();tabs.setPadding(dp(4),dp(4),dp(4),dp(4));tabs.setBackground(shape(PALE,16,0));String[] names={"概览","奖章","记录"};
        for(int i=0;i<3;i++){final int tab=i;TextView button=button(names[i],statsTab==i?WHITE:PALE,statsTab==i?INK:MUTED,13);button.setContentDescription("统计"+names[i]);button.setSelected(statsTab==i);tabs.addView(button,new LinearLayout.LayoutParams(0,dp(42),1));button.setOnClickListener(v->{statsTab=tab;historyLimit=20;haptic();render();UiMotion.enter(content,store.motion);});}content.addView(tabs);
        if(statsTab==0)statsOverview(stats);else if(statsTab==1)medals(stats);else history(stats);
    }
    private void statsOverview(ProgressStats s){
        LinearLayout hero=card(WHITE,24);hero.addView(kicker("YOUR QUIET PROGRESS"));LinearLayout main=row();TextView count=text(String.valueOf(s.challengeWins),44,INK,true);main.addView(count);TextView caption=text("  局挑战完成",13,MUTED,false);main.addView(caption);add(hero,main,14,8);hero.addView(text("每一次落笔，都比昨天更从容。",12,MUTED,false));add(content,new EchoPerchLayout(this,hero,store.motion,80,EchoArtView.PROFILE),18,0);
        LinearLayout numbers=row();statCard(numbers,"挑战完成率",s.challengeEnded==0?"—":Math.round(s.challengeWins*100f/s.challengeEnded)+"%",s.challengeEnded+" 局已结束挑战");statCard(numbers,"累计专注",minutes(s.totalTime)+" 分",s.activeDays+" 个游玩日");add(content,numbers,12,0);
        LinearLayout second=row();statCard(second,"最长连续",s.longestStreak+" 天","当前连续 "+store.streak()+" 天");statCard(second,"练习完成",s.practiceWins+" 局","不限错练习单独记录");add(content,second,10,0);
        LinearLayout clean=card(WHITE,20);clean.addView(text("推理的足迹",17,INK,true));detailRow(clean,"无提示通关",s.noHintWins+" 局");detailRow(clean,"零错误且无提示",s.perfectWins+" 局");detailRow(clean,"每日挑战",s.dailyWins+" 个日期");detailRow(clean,"挑战失败 / 主动结束",s.challengeLost+" / "+s.challengeAbandoned);add(clean,text("完成率 = 完成挑战 ÷ 已结束挑战。\n练习和进行中对局不计入挑战完成率及难度纪录。",10,MUTED,false),10,0);add(content,clean,14,0);
        LinearLayout chart=card(WHITE,20);LinearLayout chartTitle=row();chartTitle.addView(text("最近 7 天",17,INK,true),new LinearLayout.LayoutParams(0,-2,1));chartTitle.addView(text("全部完成局数",10,MUTED,false));chart.addView(chartTitle);add(chart,new WeeklyChart(this),14,0,125);add(chart,text("今日完成 "+store.dayWins(Store.today())+" 局 · 专注 "+minutes(dayTime(Store.today()))+" 分钟",12,GREEN,false),12,0);add(content,chart,14,0);
        add(content,activityHeatmap(),14,0);
        LinearLayout levels=card(WHITE,20);levels.addView(text("四种难度，四份成长",17,INK,true));add(levels,text("挑战成绩 · 最快 / 平均用时",10,MUTED,false),7,12);
        for(int i=0;i<4;i++){LinearLayout line=row();TextView name=text(Game.LEVELS[i]+"  ·  "+s.levelWins[i]+" 局",13,INK,true);line.addView(name,new LinearLayout.LayoutParams(0,dp(47),1));line.addView(text(s.levelWins[i]==0?"—  /  —":time(s.levelBest[i])+"  /  "+time(s.levelTotal[i]/s.levelWins[i]),11,MUTED,false));levels.addView(line);}add(content,levels,14,0);
        progressDetails();
    }
    private void detailRow(LinearLayout parent,String title,String value){LinearLayout r=row();r.addView(text(title,12,MUTED,false),new LinearLayout.LayoutParams(0,dp(39),1));r.addView(text(value,13,INK,true));parent.addView(r);}
    private View progressBar(float fraction){
        View bar=new View(this){private final Paint paint=new Paint(3);@Override protected void onDraw(Canvas c){paint.setColor(PALE);c.drawRoundRect(0,0,getWidth(),getHeight(),getHeight()/2f,getHeight()/2f,paint);paint.setColor(GREEN);c.drawRoundRect(0,0,getWidth()*fraction,getHeight(),getHeight()/2f,getHeight()/2f,paint);}};bar.setContentDescription("完成进度 "+Math.round(fraction*100)+"%");return bar;
    }
    private void medals(ProgressStats s){
        List<Medal> all=Medal.all(s);int earned=0;Medal next=null;for(Medal m:all){if(m.unlocked())earned++;else if(next==null||m.fraction()>next.fraction())next=m;}
        LinearLayout hero=card(WHITE,24);hero.addView(kicker("ECHO'S LITTLE TREASURES"));add(hero,text("把努力，收藏起来。",22,INK,true),10,8);add(hero,text("已获得 "+earned+" / "+all.size()+" 枚 · 四个系列",12,MUTED,false),0,14);add(hero,progressBar(earned/(float)all.size()),0,0,5);add(content,hero,20,0);
        if(next!=null){final Medal upcoming=next;LinearLayout trail=row();trail.addView(new MedalView(this,next,palette,store.motion&&foreground,false),new LinearLayout.LayoutParams(dp(60),dp(68)));LinearLayout copy=column();copy.addView(text("下一份小期待 · "+next.name,13,INK,true));add(copy,text(next.progress()+"   ↗",11,GREEN,false),7,0);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.leftMargin=dp(12);trail.addView(copy,cp);trail.setContentDescription("下一枚奖章，"+next.name);trail.setOnClickListener(v->showMedal(upcoming));add(content,trail,18,2);}
        String[] descriptions={"每一步，都会抵达新的风景。","把线索，变成属于自己的答案。","日复一日，和 Echo 留下约定。","不用着急，时间会记得。"};
        for(int family=0;family<4;family++){
            LinearLayout heading=row();heading.addView(text("0"+(family+1)+"  /  "+Medal.FAMILIES[family],19,INK,true),new LinearLayout.LayoutParams(0,-2,1));heading.addView(text("4 枚",10,MUTED,false));add(content,heading,26,6);add(content,text(descriptions[family],11,MUTED,false),0,14);
            for(int pair=0;pair<2;pair++){LinearLayout line=row();line.setGravity(Gravity.TOP);for(int col=0;col<2;col++){
                Medal medal=all.get(family*4+pair*2+col);LinearLayout tile=card(WHITE,22);tile.setPadding(dp(12),dp(15),dp(12),dp(16));tile.setGravity(Gravity.CENTER);
                int artWidth=Math.min(128,(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density-104)/2);tile.addView(new MedalView(this,medal,palette,store.motion&&foreground,false),new LinearLayout.LayoutParams(dp(artWidth),dp(Math.round(artWidth*152f/136))));TextView name=text(medal.name,14,medal.unlocked()?INK:MUTED,true);name.setGravity(Gravity.CENTER);add(tile,name,5,4);MedalStyle finish=MedalStyle.of(medal);TextView finishLabel=text(finish.rankName()+" · "+finish.title.split(" · ")[0],9,MUTED,false);finishLabel.setGravity(Gravity.CENTER);add(tile,finishLabel,0,8);TextView status=text(medal.unlocked()?"已获得  ✓":medal.progress(),10,medal.unlocked()?GREEN:MUTED,false);status.setGravity(Gravity.CENTER);tile.addView(status);add(tile,progressBar(medal.fraction()),12,0,3);
                tile.setFocusable(true);tile.setContentDescription("奖章 "+medal.name+"，"+(medal.unlocked()?"已获得":"未获得，"+medal.progress()));tile.setOnClickListener(v->showMedal(medal));UiMotion.press(tile,()->store.motion);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(col>0)lp.leftMargin=dp(12);line.addView(tile,lp);
            }add(content,line,0,12);}
        }
        add(content,text("奖章由真实记录自动解锁，也会识别旧版本积累。",11,MUTED,false),10,0);
    }
    private void showMedal(Medal medal){
        LinearLayout panel=card(BG,26);panel.setGravity(Gravity.CENTER);if(compact)panel.setPadding(dp(18),dp(14),dp(18),dp(14));add(panel,kicker(Medal.FAMILIES[medal.family]+"  /  COLLECTION 0"+(medal.tier+1)),0,compact?6:14);
        MedalView art=new MedalView(this,medal,palette,store.motion&&foreground,true);panel.addView(art,new LinearLayout.LayoutParams(dp(compact?160:208),dp(compact?179:233)));add(panel,text(medal.name,25,INK,true),compact?6:10,6);MedalStyle finish=MedalStyle.of(medal);TextView finishLabel=text(finish.rankName()+"  /  "+finish.title,11,MUTED,false);finishLabel.setGravity(Gravity.CENTER);add(panel,finishLabel,0,compact?10:14);TextView condition=text(medal.condition,13,MUTED,false);condition.setLineSpacing(dp(5),1);condition.setGravity(Gravity.CENTER);panel.addView(condition);add(panel,progressBar(medal.fraction()),compact?14:22,12,6);add(panel,text(medal.unlocked()?"已获得 · 这份努力已被珍藏":medal.progress(),12,GREEN,true),0,8);
        TextView close=button("回到奖章册",PALE,GREEN,14);close.setContentDescription("关闭奖章详情");add(panel,close,compact?14:18,0,48);centerTexts(panel);
        ScrollView scroll=new ScrollView(this);scroll.setBackground(shape(BG,26,0));scroll.setClipToOutline(true);scroll.addView(panel);medalDialog=dialogBuilder().setView(scroll).create();AlertDialog d=medalDialog;close.setOnClickListener(v->d.dismiss());d.setOnDismissListener(v->{UiMotion.clearTree(panel);if(medalDialog==d)medalDialog=null;});d.show();if(d.getWindow()!=null){d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));int width=getResources().getDisplayMetrics().widthPixels-dp(24);d.getWindow().setLayout(width,-2);panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));ViewGroup.LayoutParams size=scroll.getLayoutParams();size.height=Math.min(panel.getMeasuredHeight(),getResources().getDisplayMetrics().heightPixels-dp(100));scroll.setLayoutParams(size);}UiMotion.enter(art,store.motion);haptic();
    }
    private void history(ProgressStats s){
        add(content,text("留下的每一局",21,INK,true),24,8);add(content,text("按结束日期排列 · 进行中对局显示开始日期",11,MUTED,false),0,16);
        if(s.records.isEmpty()){LinearLayout empty=card(WHITE,22);empty.addView(echo(EchoArtView.DAILY),new LinearLayout.LayoutParams(dp(82),dp(82)));add(empty,text("故事，还等着你落笔。",19,INK,true),14,8);empty.addView(text("完成第一局后，记录会出现在这里。",12,MUTED,false));TextView start=button("开始一局",GREEN,ON_ACCENT,14);add(empty,start,22,0,48);start.setOnClickListener(v->openSetup());content.addView(empty);return;}
        int count=Math.min(historyLimit,s.records.size());for(int i=0;i<count;i++){ProgressStats.Entry entry=s.records.get(i);String status=entry.status.equals("won")?"已完成":entry.status.equals("lost")?"三错结束":entry.status.equals("abandoned")?"主动结束":"进行中";LinearLayout tile=card(WHITE,18);tile.setPadding(dp(16),dp(16),dp(16),dp(16));LinearLayout row=row();row.addView(text(Game.LEVELS[Math.max(0,Math.min(3,entry.level))]+" · "+(entry.practice?"练习":entry.daily.isEmpty()?"挑战":"每日挑战"),14,INK,true),new LinearLayout.LayoutParams(0,-2,1));row.addView(text(status,11,entry.status.equals("won")?GREEN:MUTED,false));tile.addView(row);add(tile,text(entry.date+"    /    "+time(entry.ms),12,MUTED,false),9,6);tile.addView(text("提示 "+entry.hints+" 次 · 错误 "+entry.mistakes+" 次"+(entry.daily.isEmpty()?"":" · 题目日期 "+entry.daily),10,MUTED,false));tile.setContentDescription("对局记录 "+entry.id);add(content,tile,0,10);}
        if(count<s.records.size()){TextView more=button("再看 20 条   ·   已显示 "+count+" / "+s.records.size(),PALE,GREEN,13);add(content,more,10,0,48);more.setContentDescription("加载更多记录");more.setOnClickListener(v->{int y=((ScrollView)content.getParent()).getScrollY();historyLimit+=20;render();content.post(()->((ScrollView)content.getParent()).scrollTo(0,y));});}else add(content,text("共 "+s.records.size()+" 局 · 每一局都算数",11,MUTED,false),10,0);
    }
    private void openProject(String path){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(PROJECT_URL+path)));}catch(ActivityNotFoundException e){android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("EchoSudoku GitHub",PROJECT_URL+path));tip("没有可用浏览器，已复制项目链接");}}
    private void settings(){
        content.addView(kicker("MAKE YOURSELF AT HOME"));add(content,text("你的节奏，你来定。",27,INK,true),10,8);add(content,text("一些小偏好，让专注更舒服。",12,MUTED,false),0,24);
        LinearLayout appearance=card(WHITE,22);appearance.addView(text("外观与感受",18,INK,true));add(appearance,themeEntry(),18,0,48);setting(appearance,"完成动效","页面轻动效、奖章流光、Echo 与棋盘庆祝",store.motion,value->{store.motion=value;store.save(game);setEchoMotion(root,value);if(!value)UiMotion.clearTree(root);});setting(appearance,"触感反馈","点按时轻轻回应",store.haptic,value->{store.haptic=value;store.save(game);});add(content,appearance,0,14);
        LinearLayout play=card(WHITE,22);play.addView(text("解题习惯",18,INK,true));setting(play,"新局默认快速模式","先选数字，再连续点空格填入；当前局不变",store.quickMode,value->{store.quickMode=value;store.save(game);});TextView rules=button("玩法与操作说明   ↗",PALE,GREEN,13);add(play,rules,20,0,46);rules.setOnClickListener(v->help());add(content,play,0,14);
        LinearLayout data=card(WHITE,22);data.addView(text("数据与存档",18,INK,true));saveTools(data);add(content,data,0,14);
        LinearLayout about=card(WHITE,22);about.addView(text("关于 EchoSudoku",18,INK,true));detailRow(about,"版本","1.10.1");detailRow(about,"作者",AUTHOR);detailRow(about,"体验","离线可用 · 无广告");
        add(about,text("EchoSudoku · by tankecho42",12,MUTED,false),12,0);
        TextView github=button("项目 GitHub   ↗",PALE,GREEN,13);github.setContentDescription("项目 GitHub");add(about,github,12,0,46);github.setOnClickListener(v->openProject(""));
        LinearLayout links=row();TextView releases=button("版本下载 ↗",PALE,GREEN,12),issues=button("反馈问题 ↗",PALE,GREEN,12);releases.setContentDescription("GitHub 版本下载");issues.setContentDescription("GitHub 反馈问题");links.addView(releases,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(0,dp(44),1);ilp.leftMargin=dp(8);links.addView(issues,ilp);releases.setOnClickListener(v->openProject("/releases"));issues.setOnClickListener(v->openProject("/issues"));add(about,links,8,0);
        add(about,text("记录保存在本机。卸载会删除本机数据，\n更换设备前可以先导出一份存档。",11,MUTED,false),16,0);add(content,about,0,0);
        TextView footer=text("MADE WITH PATIENCE.\n和 Echo 一起，把下一格想明白。",10,MUTED,false);footer.setGravity(Gravity.CENTER);footer.setLineSpacing(dp(7),1);add(content,footer,28,0);
    }

    private void showDailyCalendar(YearMonth month){
        LinearLayout panel=card(BG,18);LinearLayout heading=row();TextView previous=button("‹",PALE,GREEN,23),next=button("›",PALE,GREEN,23);previous.setContentDescription("上个月");next.setContentDescription("下个月");heading.addView(previous,new LinearLayout.LayoutParams(dp(42),dp(42)));TextView label=text(month.getYear()+" 年 "+month.getMonthValue()+" 月",18,INK,true);label.setGravity(Gravity.CENTER);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));heading.addView(next,new LinearLayout.LayoutParams(dp(42),dp(42)));panel.addView(heading);
        LinearLayout dailyGuide=row();dailyGuide.addView(echo(EchoArtView.DAILY),new LinearLayout.LayoutParams(dp(58),dp(58)));TextView guide=text("点选日期开始或补做 · ✓ 已完成\n补做计入实际游玩日的专注时长。",11,MUTED,false);LinearLayout.LayoutParams guideLp=new LinearLayout.LayoutParams(0,-2,1);guideLp.leftMargin=dp(10);dailyGuide.addView(guide,guideLp);add(panel,dailyGuide,10,12);
        LinearLayout weekdays=row();for(String name:new String[]{"一","二","三","四","五","六","日"}){TextView t=text(name,11,MUTED,false);t.setGravity(Gravity.CENTER);weekdays.addView(t,new LinearLayout.LayoutParams(0,dp(28),1));}panel.addView(weekdays);
        AlertDialog dialog=dialogBuilder().setView(panel).setNegativeButton("返回",null).create();calendarDialog=dialog;dialog.setOnDismissListener(d->{if(calendarDialog==dialog)calendarDialog=null;});int offset=month.atDay(1).getDayOfWeek().getValue()-1;
        for(int w=0;w<(offset+month.lengthOfMonth()+6)/7;w++){LinearLayout row=row();for(int c=0;c<7;c++){int day=w*7+c-offset+1;TextView t=text("",12,INK,false);t.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);lp.setMargins(dp(1),dp(1),dp(1),dp(1));row.addView(t,lp);if(day<1||day>month.lengthOfMonth())continue;LocalDate date=month.atDay(day);boolean future=date.isAfter(LocalDate.now()),done=store.dailyWon(date.toString());t.setText(day+(done?" ✓":""));t.setTextColor(future?MUTED:done?ON_ACCENT:INK);t.setAlpha(future?.3f:1);t.setBackground(shape(done?GREEN:date.equals(LocalDate.now())?PALE:WHITE,8,0));t.setContentDescription("每日挑战 "+date+(done?"，已完成":future?"，尚未开放":"，可挑战"));t.setEnabled(!future&&!done);t.setOnClickListener(v->{dialog.dismiss();requestGame(1,date.toString(),false);});}panel.addView(row);}
        previous.setEnabled(month.isAfter(YearMonth.now().minusMonths(11)));previous.setAlpha(previous.isEnabled()?1:.3f);next.setEnabled(month.isBefore(YearMonth.now()));next.setAlpha(next.isEnabled()?1:.3f);previous.setOnClickListener(v->{dialog.dismiss();showDailyCalendar(month.minusMonths(1));});next.setOnClickListener(v->{dialog.dismiss();showDailyCalendar(month.plusMonths(1));});dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(shape(BG,22,0));dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(GREEN);
    }
    private void progressDetails(){
        LinearLayout trend=card(WHITE,18);trend.addView(text("最近 30 天 · 同难度用时",17,INK,true));LinearLayout choices=row();for(int i=0;i<4;i++){final int level=i;TextView t=button(Game.LEVELS[i],trendLevel==i?GREEN:PALE,trendLevel==i?ON_ACCENT:GREEN,11);t.setContentDescription("趋势难度 "+Game.LEVELS[i]);choices.addView(t,new LinearLayout.LayoutParams(0,dp(36),1));t.setOnClickListener(v->{trendLevel=level;float before=0;ScrollView sc=(ScrollView)content.getParent();int y=sc.getScrollY();render();content.post(()->((ScrollView)content.getParent()).scrollTo(0,y));});}add(trend,choices,12,0);long[] values=store.trend(trendLevel);add(trend,new TrendView(this,values),12,0,118);int days=0;long sum=0;for(long v:values)if(v>=0){days++;sum+=v;}add(trend,text(days==0?"这个难度还没有通关记录。":"有通关记录的 "+days+" 天 · 按每日平均用时绘制\n越低表示用时越短；空白日期不按零计算。",11,MUTED,false),8,0);add(content,trend,14,0);
    }
    private final class TrendView extends View{
        private final long[] values;private final Paint paint=new Paint(3);TrendView(Context c,long[] values){super(c);this.values=values;setContentDescription("最近30天 "+Game.LEVELS[trendLevel]+" 挑战每日平均用时趋势");}
        @Override protected void onDraw(Canvas c){float left=dp(34),top=dp(12),bottom=getHeight()-dp(23),w=getWidth()-left-dp(8);long max=60000;for(long v:values)max=Math.max(max,v);max=((max+59999)/60000)*60000;paint.setStrokeWidth(dp(1));paint.setColor(LINE);for(int i=0;i<3;i++){float y=top+(bottom-top)*i/2;c.drawLine(left,y,getWidth(),y,paint);}paint.setTextSize(dp(9));paint.setColor(MUTED);c.drawText(String.valueOf(Math.round(max/60000f))+"分",0,top+dp(4),paint);c.drawText("0",dp(16),bottom,paint);paint.setStrokeWidth(dp(1.8f));paint.setColor(GREEN);int previous=-1;for(int i=0;i<30;i++)if(values[i]>=0){float x=left+w*i/29,y=bottom-(bottom-top)*values[i]/max;if(previous>=0&&previous==i-1)c.drawLine(left+w*previous/29,bottom-(bottom-top)*values[previous]/max,x,y,paint);c.drawCircle(x,y,dp(2.5f),paint);previous=i;}paint.setColor(MUTED);LocalDate first=LocalDate.now().minusDays(29);c.drawText(first.getMonthValue()+"/"+first.getDayOfMonth(),left,getHeight()-dp(3),paint);c.drawText("今天",getWidth()-dp(26),getHeight()-dp(3),paint);}
    }
    private void saveTools(LinearLayout options){
        TextView export=button("导出存档  ↗",PALE,GREEN,13),importButton=button("导入存档  ↙",PALE,GREEN,13);export.setContentDescription("导出存档");importButton.setContentDescription("导入存档");add(options,export,14,0,44);add(options,importButton,8,0,44);export.setOnClickListener(v->pickBackup(true));importButton.setOnClickListener(v->pickBackup(false));add(options,text("包含当前进度、撤销历史、统计和主题。\n导入会替换本机记录，并保留一次导入前恢复点。",10,MUTED,false),8,0);
        if(store.hasRecovery()){TextView recovery=button("恢复上次导入前的存档",PALE,GREEN,12);add(options,recovery,10,0,44);recovery.setOnClickListener(v->readBackupText(store.recovery()));}
    }
    private void pickBackup(boolean export){if(ioBusy)return;tick();store.save(game);Intent intent=new Intent(export?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");if(export)intent.putExtra(Intent.EXTRA_TITLE,"EchoSudoku-"+Store.today()+".json");try{startActivityForResult(intent,export?EXPORT_SAVE:IMPORT_SAVE);}catch(ActivityNotFoundException e){tip("设备没有可用的文件选择器");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;if(request!=EXPORT_SAVE&&request!=IMPORT_SAVE)return;android.net.Uri uri=data.getData();ioBusy=true;
        if(request==EXPORT_SAVE){final String text;try{tick();store.save(game);text=store.exportBackup();}catch(Exception e){ioBusy=false;tip("无法准备存档");return;}io.execute(()->{try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException();out.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.flush();runOnUiThread(()->{ioBusy=false;tip("存档已导出");});}catch(Exception e){runOnUiThread(()->{ioBusy=false;tip("导出失败，请换一个保存位置重试");});}});
        }else io.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>8*1024*1024)throw new IOException("文件超过8MB");out.write(b,0,n);}JSONObject valid=Store.validateBackup(out.toString("UTF-8"));runOnUiThread(()->{ioBusy=false;confirmImport(valid);});}catch(Exception e){runOnUiThread(()->{ioBusy=false;tip("导入失败：文件损坏或版本不支持，原存档未修改");});}});
    }
    private void readBackupText(String text){if(ioBusy)return;ioBusy=true;io.execute(()->{try{JSONObject valid=Store.validateBackup(text);runOnUiThread(()->{ioBusy=false;confirmImport(valid);});}catch(Exception e){runOnUiThread(()->{ioBusy=false;tip("恢复点校验失败，原存档未修改");});}});}
    private void confirmImport(JSONObject valid){if(isFinishing()||isDestroyed())return;dialogBuilder().setTitle("替换本机存档？").setMessage("文件包含 "+valid.optJSONObject("records").length()+" 局记录。导入将替换当前进度与统计；本机原存档会保留为一次恢复点。").setNegativeButton("取消",null).setPositiveButton("确认导入",(d,w)->{try{tick();store.importBackup(valid);game=store.load();paused=false;page="settings";render();ioBusy=true;io.execute(()->{try{Store.awaitWrites();}catch(Exception ignored){}runOnUiThread(()->{ioBusy=false;tip(store.saveError.isEmpty()?"存档已导入，原存档已保存为恢复点":store.saveError);});});}catch(Exception e){tip("导入失败，未完成替换");}}).show();}

    private long dayTime(String day){JSONObject o=store.days.optJSONObject(day);return o==null?0:o.optLong("ms");}
    private int heatColor(int level){
        if(level<=0)return PALE;float t=new float[]{0,.28f,.52f,.76f,1}[level];
        return Color.rgb(Math.round(Color.red(PALE)*(1-t)+Color.red(GREEN)*t),Math.round(Color.green(PALE)*(1-t)+Color.green(GREEN)*t),Math.round(Color.blue(PALE)*(1-t)+Color.blue(GREEN)*t));
    }
    private View activityHeatmap(){
        LinearLayout panel=card(WHITE,20);panel.setPadding(dp(16),dp(20),dp(16),dp(18));panel.setContentDescription("最近13周游玩热力图");
        LinearLayout heading=row();heading.addView(text("每一天，都算数",17,INK,true),new LinearLayout.LayoutParams(0,-2,1));heading.addView(text("最近 13 周",10,MUTED,false));panel.addView(heading);
        add(panel,text("按每日游玩时长 · 越专注，颜色越深",10,MUTED,false),6,16);
        LocalDate today=LocalDate.now(),start=ActivityHeatmap.start(today);
        TextView detail=text("点选一天，看看留下的专注。",11,GREEN,false);detail.setContentDescription("热力图日期详情");detail.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout months=row();months.addView(new View(this),new LinearLayout.LayoutParams(dp(20),dp(18)));int lastMonth=-1;
        for(int week=0;week<ActivityHeatmap.WEEKS;week++){LocalDate date=start.plusWeeks(week);String label=date.getMonthValue()!=lastMonth?date.getMonthValue()+"月":"";lastMonth=date.getMonthValue();TextView month=text(label,8,MUTED,false);month.setGravity(Gravity.CENTER);months.addView(month,new LinearLayout.LayoutParams(0,dp(18),1));}panel.addView(months);
        String[] weekdays={"一","二","三","四","五","六","日"};
        for(int day=0;day<7;day++){
            LinearLayout line=row();TextView weekday=text(day%2==0?weekdays[day]:"",8,MUTED,false);line.addView(weekday,new LinearLayout.LayoutParams(dp(20),dp(21)));
            for(int week=0;week<ActivityHeatmap.WEEKS;week++){
                final LocalDate date=start.plusDays(week*7+day);final long ms=dayTime(date.toString());boolean future=date.isAfter(today);
                FrameLayout hit=new FrameLayout(this);View square=new View(this);square.setBackground(shape(future?WHITE:heatColor(ActivityHeatmap.level(ms)),3,0));FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,dp(16),Gravity.CENTER);sp.leftMargin=dp(2);sp.rightMargin=dp(2);hit.addView(square,sp);line.addView(hit,new LinearLayout.LayoutParams(0,dp(21),1));
                if(!future){String description=date+"，游玩 "+(ms==0?"0 分钟":minutes(ms)+" 分钟");hit.setContentDescription(description);hit.setTag(date.toString());hit.setFocusable(true);hit.setOnClickListener(v->{detail.setText(date.getMonthValue()+"月"+date.getDayOfMonth()+"日  ·  "+(ms==0?"还没有游玩":("专注 "+minutes(ms)+" 分钟"))+"  ·  完成 "+store.dayWins(date.toString())+" 局");});}
                else hit.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }panel.addView(line);
        }
        LinearLayout legend=row();legend.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);legend.addView(text("少  ",9,MUTED,false));
        for(int i=0;i<5;i++){View swatch=new View(this);swatch.setBackground(shape(heatColor(i),2,0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(11),dp(11));lp.leftMargin=dp(3);legend.addView(swatch,lp);}legend.addView(text("  多",9,MUTED,false));add(panel,legend,10,10);panel.addView(detail);return panel;
    }
    private interface Toggle{void set(boolean value);}
    private void setting(LinearLayout parent,String label,String detail,boolean checked,Toggle action){LinearLayout r=row();LinearLayout left=column();left.addView(text(label,14,INK,false));add(left,text(detail,10,MUTED,false),4,0);r.addView(left,new LinearLayout.LayoutParams(0,-2,1));Switch toggle=new Switch(this);toggle.setContentDescription(label);toggle.setChecked(checked);toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(GREEN));toggle.setTrackTintList(android.content.res.ColorStateList.valueOf(palette.match));toggle.setOnCheckedChangeListener((b,c)->action.set(c));r.addView(toggle);add(parent,r,20,0);}
    private void navigation(){
        LinearLayout nav=row();nav.setBackgroundColor(BG);nav.setPadding(dp(10),dp(7),dp(10),dp(7));String[] names={"游戏中心","专注对局","统计","设置"},pages={"home","game","stats","settings"};
        for(int i=0;i<4;i++){final String target=pages[i];LinearLayout item=column();item.setGravity(Gravity.CENTER);boolean selected=page.equals(target)||(target.equals("game")&&page.equals("setup"));Icon icon=new Icon(this,i,selected?GREEN:MUTED);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);item.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));if(selected)item.setBackground(shape(PALE,18,0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(50),1);lp.setMargins(dp(5),0,dp(5),0);nav.addView(item,lp);item.setContentDescription(names[i]);item.setSelected(selected);item.setFocusable(true);UiMotion.press(item,()->store.motion);item.setOnClickListener(v->{if(page.equals(target))return;haptic();if(target.equals("game")&&game==null)openSetup();else navigate(target);});}root.addView(nav,new LinearLayout.LayoutParams(-1,dp(64)));
    }
    private void applyPalette(){
        palette=Palette.of(store.themeId);BG=palette.bg;INK=palette.ink;MUTED=palette.muted;GREEN=palette.accent;PALE=palette.pale;DARK=palette.hero;LIME=palette.lime;WHITE=palette.surface;LINE=palette.line;ON_ACCENT=palette.onAccent;ON_HERO=palette.onHero;HERO_MUTED=palette.heroMuted;
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().setBackgroundDrawable(new ColorDrawable(BG));
        getWindow().getDecorView().setSystemUiVisibility(palette.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    private AlertDialog.Builder dialogBuilder(){return new AlertDialog.Builder(new ContextThemeWrapper(this,palette.dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert));}
    private View themeEntry(){TextView choice=button("外观主题   ·   "+Palette.NAMES[store.themeId]+"     ◇",PALE,GREEN,12);choice.setContentDescription("切换主题");choice.setOnClickListener(v->showThemes());return choice;}
    private void changeTheme(int id){tick();store.themeId=Math.max(0,Math.min(4,id));store.save(game);render();}
    private void showThemes(){
        tick();boolean wasPaused=paused;paused=true;if(boardView!=null)updateGame();
        AlertDialog d=dialogBuilder().setTitle("挑一个今天的颜色").setSingleChoiceItems(Palette.NAMES,store.themeId,(dialog,which)->{store.themeId=which;store.save(game);dialog.dismiss();}).setNegativeButton("返回",null).create();
        d.setOnDismissListener(dialog->{paused=wasPaused;lastTick=SystemClock.elapsedRealtime();render();});d.show();
    }
    private void completeVictory(){
        if(game==null||!game.status.equals("won")||victoryOverlay!=null)return;
        if(store.motion&&android.animation.ValueAnimator.areAnimatorsEnabled()){
            long now=SystemClock.uptimeMillis();boardView.startVictory(now);
            FrameLayout host=findViewById(android.R.id.content);victoryOverlay=new VictoryOverlay(this,palette,now);
            host.addView(victoryOverlay,new FrameLayout.LayoutParams(-1,-1));
            scheduleResult(VictoryMotion.RESULT_DELAY_MS);
        }else{boardView.stopEffects();scheduleResult(350);}
    }
    private void stopVictoryVisuals(){
        if(victoryOverlay!=null){victoryOverlay.stop();ViewParent parent=victoryOverlay.getParent();if(parent instanceof ViewGroup)((ViewGroup)parent).removeView(victoryOverlay);victoryOverlay=null;}
        if(boardView!=null)boardView.stopEffects();
    }
    private void cancelCompletion(){
        if(pendingResult!=null)handler.removeCallbacks(pendingResult);pendingResult=null;pendingResultGame=null;stopVictoryVisuals();
    }
    private void scheduleResult(long delay){
        if(pendingResult!=null)handler.removeCallbacks(pendingResult);
        final Game completedGame=game;pendingResultGame=completedGame;
        pendingResult=()->{pendingResult=null;if(game!=completedGame||!page.equals("game")){pendingResultGame=null;return;}if(foreground)resultDialog();};
        handler.postDelayed(pendingResult,delay);
    }
    private void centerTexts(LinearLayout layout){for(int i=0;i<layout.getChildCount();i++)if(layout.getChildAt(i) instanceof TextView)((TextView)layout.getChildAt(i)).setGravity(Gravity.CENTER);}
    private void metricInline(LinearLayout parent,String label,String value){LinearLayout c=column();c.setGravity(Gravity.CENTER);c.addView(text(value,17,INK,true));add(c,text(label,10,MUTED,false),7,0);centerTexts(c);parent.addView(c,new LinearLayout.LayoutParams(0,-2,1));}
    private void statCard(LinearLayout parent,String label,String value,String detail){LinearLayout c=card(WHITE,18);c.setPadding(dp(16),dp(16),dp(12),dp(16));c.addView(text(label,11,MUTED,false));add(c,text(value,25,INK,false),9,8);c.addView(text(detail,10,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(parent.getChildCount()>0)p.leftMargin=dp(10);parent.addView(c,p);}
    private TextView tool(LinearLayout p,String symbol,String label){TextView t=text(symbol+"\n"+label,14,MUTED,false);t.setGravity(Gravity.CENTER);t.setLineSpacing(dp(3),1);t.setContentDescription(label);t.setBackground(shape(BG,12,0));p.addView(t,new LinearLayout.LayoutParams(0,dp(compact?48:52),1));return t;}
    private TextView roundIcon(String s,String label){TextView t=button(s,PALE,GREEN,26);t.setContentDescription(label);return t;}
    private TextView kicker(String s){TextView t=text(s,10,GREEN,true);t.setLetterSpacing(.18f);return t;}
    private void setEchoMotion(View view,boolean enabled){if(view instanceof MedalView)((MedalView)view).setMotionEnabled(enabled);if(view instanceof EchoArtView)((EchoArtView)view).setMotionEnabled(enabled);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)setEchoMotion(group.getChildAt(i),enabled);}}
    private ImageView echo(int pose){return new EchoArtView(this,pose,store.motion);}
    private LinearLayout card(int color,int radius){LinearLayout c=column();c.setPadding(dp(22),dp(22),dp(22),dp(22));c.setBackground(shape(color,radius,color==WHITE?LINE:0));return c;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER_VERTICAL);t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return t;}
    private TextView button(String s,int fill,int color,int size){TextView t=text(s,size,color,true);t.setGravity(Gravity.CENTER);t.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(((GREEN&0xffffff)|0x28000000)),shape(fill,15,0),null));t.setClickable(true);t.setFocusable(true);UiMotion.press(t,()->store.motion);return t;}
    private GradientDrawable shape(int fill,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private void add(LinearLayout l,View v,int top,int bottom){add(l,v,top,bottom,-2);}
    private void add(LinearLayout l,View v,int top,int bottom,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));p.topMargin=dp(top);p.bottomMargin=dp(bottom);l.addView(v,p);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void tip(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void haptic(){if(store.haptic)root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}
    public static String time(long ms){long s=ms/1000;return s>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",s/3600,s/60%60,s%60):String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private static String minutes(long ms){if(ms>0&&ms<60000)return "<1";return String.valueOf(ms/60000);}
    private final class WeeklyChart extends View{
        private final Paint p=new Paint(3);private final int[] counts=new int[7];WeeklyChart(Context c){super(c);LocalDate first=LocalDate.now().minusDays(6);for(JSONObject r:store.history())if(r.optString("status").equals("won")){try{int i=(int)java.time.temporal.ChronoUnit.DAYS.between(first,LocalDate.parse(r.optString("endDate",r.optString("date"))));if(i>=0&&i<7)counts[i]++;}catch(RuntimeException ignored){}}setContentDescription("最近七天完成局数柱状图");}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),step=w/7;int max=1;for(int i=0;i<7;i++)max=Math.max(max,counts[i]);p.setTypeface(Typeface.create("sans-serif",0));p.setTextAlign(Paint.Align.CENTER);for(int i=0;i<7;i++){LocalDate d=LocalDate.now().minusDays(6-i);int value=counts[i];float x=(i+.5f)*step,bottom=h-dp(24),bar=Math.max(dp(4),(h-dp(50))*value/max);p.setColor(i==6?GREEN:palette.match);c.drawRoundRect(x-dp(11),bottom-bar,x+dp(11),bottom,dp(5),dp(5),p);p.setTextSize(dp(11));p.setColor(value>0?INK:MUTED);c.drawText(String.valueOf(value),x,bottom-bar-dp(7),p);p.setTextSize(dp(9));p.setColor(MUTED);c.drawText(i==6?"今天":d.getMonthValue()+"/"+d.getDayOfMonth(),x,h-dp(3),p);}}
    }
    private static final class Icon extends View{
        private final Paint p=new Paint(3);private final int kind,color;Icon(Context c,int k,int color){super(c);kind=k;this.color=color;}
        @Override protected void onDraw(Canvas canvas){canvas.save();canvas.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);if(kind==0){for(int r=0;r<2;r++)for(int c=0;c<2;c++)canvas.drawRoundRect(3+c*11,3+r*11,10+c*11,10+r*11,2,2,p);}else if(kind==1){canvas.drawRoundRect(3,3,21,21,4,4,p);canvas.drawLine(9,3,9,21,p);canvas.drawLine(15,3,15,21,p);canvas.drawLine(3,9,21,9,p);canvas.drawLine(3,15,21,15,p);}else if(kind==2){canvas.drawLine(3,21,21,21,p);canvas.drawRoundRect(4,12,7,18,1,1,p);canvas.drawRoundRect(10.5f,5,13.5f,18,1,1,p);canvas.drawRoundRect(17,9,20,18,1,1,p);}else{canvas.drawCircle(12,12,7,p);canvas.drawCircle(12,12,2.8f,p);for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45,12,12);canvas.drawLine(12,2,12,5,p);canvas.restore();}}canvas.restore();}
    }
}
