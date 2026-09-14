package com.tankecho.zensudoku;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

/** Real game rendering and input, with no connection to a player's Store. */
public final class TutorialActivity extends Activity {
    private final Handler demonstration=new Handler(Looper.getMainLooper());
    private Palette palette;
    private boolean motion=true,foreground,demonstrating,compact,candidateSeen,noteSeen;
    private int lesson=-1,completed,ruleKind,ruleSeen;
    private Game sandbox;
    private BoardView board;
    private FrameLayout stage;
    private LinearLayout root,body,footer;
    private ScrollView scroll;
    private TextView feedback,demoButton,candidateButton,noteButton,hintButton,applyButton;
    private final TextView[] keys=new TextView[9];
    private VictoryOverlay confetti;
    private String message="";

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        palette=Palette.of(getIntent().getIntExtra("theme",0));motion=getIntent().getBooleanExtra("motion",true);
        if(saved!=null){lesson=saved.getInt("lesson",-1);completed=saved.getInt("completed",0);ruleKind=saved.getInt("ruleKind",0);ruleSeen=saved.getInt("ruleSeen",0);candidateSeen=saved.getBoolean("candidateSeen");noteSeen=saved.getBoolean("noteSeen");message=saved.getString("message","");
            try{if(saved.containsKey("sandbox"))sandbox=GameCodec.decode(new JSONObject(saved.getString("sandbox")),false);}catch(Exception ignored){sandbox=null;}}
        lesson=Math.max(-1,Math.min(5,lesson));if(lesson>=0&&sandbox==null)resetLesson();
        getWindow().setStatusBarColor(palette.bg);getWindow().setNavigationBarColor(palette.bg);
        getWindow().getDecorView().setSystemUiVisibility(palette.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        render();
    }
    @Override protected void onSaveInstanceState(Bundle saved){
        super.onSaveInstanceState(saved);saved.putInt("lesson",lesson);saved.putInt("completed",completed);saved.putInt("ruleKind",ruleKind);saved.putInt("ruleSeen",ruleSeen);saved.putBoolean("candidateSeen",candidateSeen);saved.putBoolean("noteSeen",noteSeen);saved.putString("message",message);
        if(sandbox!=null)try{saved.putString("sandbox",GameCodec.encode(sandbox).toString());}catch(Exception e){throw new IllegalStateException(e);}
    }
    @Override protected void onResume(){super.onResume();foreground=true;echoMotion(root,motion);}
    @Override protected void onPause(){foreground=false;stopDemo();stopEffects();echoMotion(root,false);super.onPause();}
    @Override protected void onDestroy(){stopDemo();stopEffects();UiMotion.clearTree(root);super.onDestroy();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused){stopDemo();stopEffects();}echoMotion(root,focused&&foreground&&motion);}
    @Override public void onBackPressed(){if(lesson>=0)openLesson(-1);else finish();}
    private void openLesson(int index){stopDemo();stopEffects();lesson=index;if(index>=0)resetLesson();render();UiMotion.enter(body,motion);}
    private void resetLesson(){sandbox=TutorialLesson.create(lesson);ruleKind=0;ruleSeen=0;candidateSeen=false;noteSeen=false;message=initialMessage();}
    private String initialMessage(){switch(lesson){
        case 0:return "点「行 / 列 / 宫」，观察三个区域。";
        case 1:return "试一试：点第 1 行第 3 列的空格，再填 4。";
        case 2:return "先点数字 5，再依次填入三个空格。";
        case 3:return "选中标记空格，打开候选数，再试试笔记。";
        case 4:return "选中标记空格，点「查看提示」读一段推理。";
        default:return "只差最后一格。选中空格，填入 4。";
    }}
    private void render(){
        stopEffects();UiMotion.clearTree(root);compact=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density<740;
        stage=new FrameLayout(this);root=column();root.setBackgroundColor(palette.bg);stage.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(stage);
        LinearLayout top=row();top.setPadding(dp(18),dp(4),dp(18),dp(4));TextView back=button(lesson<0?"‹ 返回":"‹ 目录",palette.bg,palette.accent);back.setContentDescription(lesson<0?"关闭教学":"返回教学目录");top.addView(back,new LinearLayout.LayoutParams(dp(82),dp(46)));back.setOnClickListener(v->onBackPressed());
        TextView brand=text("ECHO · LEARN",11,palette.muted,true);brand.setGravity(Gravity.CENTER);top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView close=button("×",palette.bg,palette.accent);close.setTextSize(24);close.setContentDescription("退出教学");top.addView(close,new LinearLayout.LayoutParams(dp(46),dp(46)));close.setOnClickListener(v->finish());root.addView(top);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);body=column();body.setClipChildren(false);body.setPadding(dp(22),dp(12),dp(22),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        footer=column();footer.setPadding(dp(22),dp(10),dp(22),dp(14));footer.setBackgroundColor(palette.surface);root.addView(footer,new LinearLayout.LayoutParams(-1,-2));
        if(lesson<0)overview();else lessonPage();
    }
    private void overview(){
        LinearLayout hero=row();LinearLayout words=column();words.addView(text("和 Echo 一起，\n学会下一步。",28,palette.ink,true));add(words,text("六个小练习，把操作变成直觉。",12,palette.muted,false),10);hero.addView(words,new LinearLayout.LayoutParams(0,-2,1));EchoArtView echo=new EchoArtView(this,EchoArtView.THINK,motion);hero.addView(echo,new LinearLayout.LayoutParams(dp(90),dp(110)));body.addView(hero);
        TextView safe=text("真实棋盘 · 随时重播 · 不影响你的对局",11,palette.accent,true);add(body,safe,14);
        add(body,text("已探索 "+Integer.bitCount(completed)+" / 6 节",11,palette.muted,false),18);
        String[] summaries={"观察 1–9 在行、列和宫里的秩序","亲手填入第一个答案","一次选数，连续填入","自动候选与手写笔记的区别","先看理由，再决定是否填入","试试波浪、同数完成与整盘庆祝"};
        for(int i=0;i<6;i++){final int at=i;LinearLayout card=column();card.setPadding(dp(18),dp(16),dp(18),dp(16));card.setBackground(shape(palette.surface,20));LinearLayout line=row();TextView count=text(String.format("%02d",i+1),15,palette.accent,true);line.addView(count,new LinearLayout.LayoutParams(dp(36),-2));line.addView(text(TutorialLesson.LABELS[i],16,palette.ink,true),new LinearLayout.LayoutParams(0,-2,1));line.addView(text((completed&(1<<i))!=0?"✓":"↗",18,palette.accent,true));card.addView(line);add(card,text(summaries[i],12,palette.muted,false),8);card.setContentDescription("学习 "+TutorialLesson.LABELS[i]);card.setFocusable(true);card.setOnClickListener(v->openLesson(at));UiMotion.press(card,()->motion);add(body,card,10);}
        TextView start=button("从第一节开始    →",palette.accent,palette.onAccent);footer.addView(start,new LinearLayout.LayoutParams(-1,dp(50)));start.setOnClickListener(v->openLesson(0));
    }
    private void lessonPage(){
        LinearLayout progress=row();for(int i=0;i<6;i++){View segment=new View(this);segment.setBackground(shape(i<=lesson?palette.accent:palette.line,3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(3),1);lp.setMargins(0,0,dp(i==5?0:5),0);progress.addView(segment,lp);}body.addView(progress);
        LinearLayout heading=row();heading.addView(text(String.format("%02d / 06",lesson+1),11,palette.accent,true),new LinearLayout.LayoutParams(0,-2,1));TextView reset=button("重新练习 ↻",palette.bg,palette.accent);reset.setTextSize(11);reset.setContentDescription("重新练习本节");heading.addView(reset,new LinearLayout.LayoutParams(dp(102),dp(40)));reset.setOnClickListener(v->openLesson(lesson));add(body,heading,6);
        add(body,text(TutorialLesson.TITLES[lesson],compact?23:26,palette.ink,true),2);add(body,text(TutorialLesson.DESCRIPTIONS[lesson],13,palette.muted,false),9);
        feedback=text(message,13,palette.accent,true);feedback.setPadding(dp(14),dp(12),dp(14),dp(12));feedback.setBackground(shape(palette.pale,14));feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);feedback.setContentDescription("教学反馈："+message);add(body,feedback,14);
        if(lesson==0){LinearLayout regions=row();String[] labels={"行","列","宫"};for(int i=0;i<3;i++){final int k=i;TextView b=button(labels[i],palette.surface,palette.accent);b.setContentDescription("观察"+labels[i]);addWeighted(regions,b,42);b.setOnClickListener(v->{manual();inspect(k);});}add(body,regions,10);}
        LinearLayout frame=column();frame.setPadding(dp(8),dp(8),dp(8),dp(8));frame.setBackground(shape(palette.surface,18));frame.setClipChildren(false);board=new BoardView(this,sandbox,palette,i->{manual();selectCell(i);});
        int side=Math.min(dp(compact?258:322),getResources().getDisplayMetrics().widthPixels-dp(60));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(side,side);bp.gravity=Gravity.CENTER;frame.addView(board,bp);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-2,-2);fp.gravity=Gravity.CENTER;fp.topMargin=dp(14);body.addView(frame,fp);
        if(lesson==0)board.showHint(-1,TutorialLesson.region(ruleKind));else if(sandbox.board[2]==0&&lesson!=2)board.showHint(2,new int[0]);
        if(lesson!=0){LinearLayout pad=row();pad.setGravity(Gravity.CENTER);for(int n=1;n<=9;n++){final int digit=n;TextView key=button(""+n,palette.surface,palette.accent);key.setTextSize(20);key.setPadding(0,0,0,0);key.setContentDescription("教学数字 "+n);keys[n-1]=key;LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(0,dp(48),1);kp.setMargins(dp(1),0,dp(1),0);pad.addView(key,kp);key.setOnClickListener(v->{manual();number(digit);});}add(body,pad,12);}
        if(lesson==3){LinearLayout tools=row();candidateButton=button("候选数",palette.pale,palette.accent);candidateButton.setContentDescription("教学候选数");noteButton=button("笔记",palette.pale,palette.accent);noteButton.setContentDescription("教学笔记");addWeighted(tools,candidateButton,44);addWeighted(tools,noteButton,44);candidateButton.setOnClickListener(v->{manual();candidates();});noteButton.setOnClickListener(v->{manual();notes();});add(body,tools,9);}
        if(lesson==4){LinearLayout tools=row();hintButton=button("查看提示 · "+(3-sandbox.hints),palette.pale,palette.accent);hintButton.setContentDescription("教学查看提示");applyButton=button("填入这一格",palette.accent,palette.onAccent);applyButton.setContentDescription("教学填入提示");addWeighted(tools,hintButton,46);addWeighted(tools,applyButton,46);hintButton.setOnClickListener(v->{manual();hint();});applyButton.setOnClickListener(v->{manual();applyHint();});add(body,tools,10);}
        demoButton=button("▶  看一遍演示",palette.pale,palette.accent);demoButton.setContentDescription("观看本节演示");add(body,demoButton,14,46);demoButton.setOnClickListener(v->{if(demonstrating){stopDemo();message("轮到你了，接着试试。 ");}else showDemo();});
        LinearLayout note=column();note.setPadding(dp(16),dp(16),dp(16),dp(16));note.setBackground(shape(palette.surface,18));note.addView(text("ECHO 的小提醒",10,palette.accent,true));add(note,text(TutorialLesson.NOTES[lesson],12,palette.muted,false),9);add(body,note,16);
        LinearLayout navigation=row();TextView prev=button(lesson==0?"目录":"上一节",palette.pale,palette.accent);prev.setContentDescription("教学上一节");navigation.addView(prev,new LinearLayout.LayoutParams(dp(88),dp(48)));prev.setOnClickListener(v->openLesson(lesson-1));TextView next=button(lesson==5?"完成学习  ✓":"下一节   →",palette.accent,palette.onAccent);next.setContentDescription("教学下一节");LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(48),1);np.leftMargin=dp(10);navigation.addView(next,np);next.setOnClickListener(v->openLesson(lesson==5?-1:lesson+1));footer.addView(navigation);refreshControls();
    }
    private void inspect(int kind){ruleKind=kind;ruleSeen|=1<<kind;board.showHint(-1,TutorialLesson.region(kind));String[] words={"横着看：这一行包含 1–9，各出现一次。","竖着看：这一列也需要 1–9，不重复。","再看粗线围成的 3×3 宫：这里也有完整的 1–9。"};message(words[kind]);if(ruleSeen==7)complete("三个规则一起成立，就是一盘完整数独。✓");}
    private void selectCell(int index){
        if(lesson==0){sandbox.selected=index;board.refresh();return;}
        boolean[] before=sandbox.completedUnits();int result=sandbox.selectCell(index);
        if(result>0){afterEntry(result,before,sandbox.activeNumber);return;}
        if(sandbox.board[index]>0){message("已有数字可以点选查看。试试标记的空格。 ");board.refresh();return;}
        if(lesson==1||lesson==5)message(index==2?"这一格还缺 4。点下方数字 4。":"本节先完成第 1 行第 3 列的标记空格。 ");
        else if(lesson==2)message("先点下方数字 5，再点空格就能连续填入。 ");
        else if(lesson==3)message("点「候选数」查看可能性；也可以打开「笔记」亲手记一个数字。 ");
        else message("点击「查看提示」，先看看为什么。 ");board.refresh();refreshControls();
    }
    private void number(int digit){
        if(sandbox.quickMode){if(sandbox.selectNumber(digit))message("已选 "+digit+"，现在点空格。再点数字可取消选择。 ");board.refresh();refreshControls();return;}
        if(sandbox.selected<0){message("先点棋盘里的空格，再选择数字。 ");return;}
        boolean[] before=sandbox.completedUnits();int result=sandbox.enter(digit);afterEntry(result,before,digit);
    }
    private void afterEntry(int result,boolean[] before,int digit){
        if(result==2||result==5){board.wrong(digit,motionAllowed());message("还不对，看看这一格的行、列和宫。这里只是练习，可以再试。 ");}
        else if(result==3){noteSeen=true;message("小数字只是你的笔记，不算填错。再点同一个数可以擦掉笔记。 ");if(candidateSeen)complete("你已经试过自动候选和手动笔记。✓");}
        else if(result==1||result==4){board.clearHint();board.celebrate(before,motionAllowed());if(lesson==1&&sandbox.board[2]==4)complete("第一格，填对了。主题色的 4 就是你的答案。✓");else if(lesson==2&&sandbox.remaining(5)==0)complete("九个 5 都齐了！数字键已取消并禁用，棋盘上的 5 仍能点选。✓");else if(lesson==5){complete("整盘完成！格子像波浪一样起伏，庆祝这一份专注。✓");victory();}else message("填对了，继续探索。 ");}
        else message("先选择可填写的空格。 ");board.refresh();refreshControls();
    }
    private void candidates(){
        if(sandbox.toggleCandidates()){candidateSeen=true;message("小数字是这一格当前合法的候选；再点可隐藏。现在关闭候选，打开笔记，记下 4。 ");if(noteSeen)complete("你已经试过自动候选和手动笔记。✓");}else message("先选择一个空格，再查看它的候选数。 ");board.refresh();refreshControls();
    }
    private void notes(){
        sandbox.pencil=!sandbox.pencil;if(sandbox.pencil&&sandbox.selected>=0){sandbox.candidates[sandbox.selected]=false;sandbox.allCandidates=false;}
        message(sandbox.pencil?"笔记已开启。点数字 4，把推测写成小字。":"笔记已关闭，数字键恢复填写答案。 ");refreshControls();board.refresh();
    }
    private void hint(){
        if(sandbox.pendingHintCell>=0){message(sandbox.pendingHintReason+"\n这次已计入，不会重复扣次。 ");return;}
        if(sandbox.selected<0||sandbox.board[sandbox.selected]!=0){message("先点标记的空格，再查看提示。 ");return;}
        HintEngine.Plan plan=HintEngine.find(sandbox.board,sandbox.selected);
        if(sandbox.reserveHint(plan)){board.showHint(plan.cell,plan.related);message(plan.explanation+"\n已使用 "+sandbox.hints+" / 3 次提示。可以先自己想，也可以填入。 ");}else message("本节没有可用的新提示。点击重新练习可以再看一次。 ");refreshControls();
    }
    private void applyHint(){boolean[] before=sandbox.completedUnits();if(sandbox.applyPendingHint()){board.clearHint();board.celebrate(before,motionAllowed());complete("读懂理由，再填入答案。提示格会留下小标记，不能擦除。✓");board.refresh();}refreshControls();}
    private void victory(){if(!motionAllowed())return;long now=SystemClock.uptimeMillis();board.startVictory(now);if(confetti!=null)stage.removeView(confetti);confetti=new VictoryOverlay(this,palette,now);stage.addView(confetti,new FrameLayout.LayoutParams(-1,-1));}
    private void complete(String text){completed|=1<<lesson;message(text);}
    private void message(String text){message=text;if(feedback!=null){feedback.setText(text);feedback.setContentDescription("教学反馈："+text);}}
    private void refreshControls(){
        if(sandbox==null||lesson<0)return;
        if(lesson!=0)for(int n=1;n<=9;n++){TextView key=keys[n-1];if(key==null)continue;boolean available=!sandbox.quickMode||sandbox.numberAvailable(n);key.setEnabled(available&&sandbox.status.equals("playing"));key.setAlpha(available?1:.32f);key.setSelected(sandbox.activeNumber==n);key.setBackground(shape(sandbox.activeNumber==n?palette.accent:palette.surface,12));key.setTextColor(sandbox.activeNumber==n?palette.onAccent:palette.accent);}
        if(lesson==3){candidateButton.setSelected(sandbox.selected>=0&&sandbox.showCandidates(sandbox.selected));noteButton.setSelected(sandbox.pencil);noteButton.setText(sandbox.pencil?"笔记 · 开":"笔记");}
        if(lesson==4){hintButton.setText(sandbox.pendingHintCell>=0?"重看这次提示":"查看提示 · "+(3-sandbox.hints));applyButton.setEnabled(sandbox.pendingHintCell>=0);applyButton.setAlpha(applyButton.isEnabled()?1:.4f);}
    }
    private void manual(){if(demonstrating)stopDemo();}
    private void showDemo(){
        stopDemo();resetLesson();render();demonstrating=true;demoButton.setText("Ⅱ  接下来，自己试试");
        if(lesson==0){later(550,()->inspect(0));later(1750,()->inspect(1));later(2950,()->inspect(2));later(4200,()->stopDemo());}
        else if(lesson==1){later(550,()->selectCell(2));later(1700,()->number(4));later(3300,()->stopDemo());}
        else if(lesson==2){later(550,()->number(5));later(1550,()->selectCell(0));later(2550,()->selectCell(14));later(3550,()->selectCell(24));later(5400,()->stopDemo());}
        else if(lesson==3){later(450,()->selectCell(2));later(1300,()->candidates());later(3000,()->notes());later(4000,()->number(4));later(5300,()->stopDemo());}
        else if(lesson==4){later(450,()->selectCell(2));later(1300,()->hint());later(4700,()->applyHint());later(6300,()->stopDemo());}
        else{later(550,()->selectCell(2));later(1500,()->number(4));later(5600,()->stopDemo());}
        body.post(()->scroll.smoothScrollTo(0,Math.max(0,feedback.getTop()-dp(8))));
    }
    private void later(long delay,Runnable action){demonstration.postDelayed(()->{if(demonstrating&&foreground&&hasWindowFocus())action.run();},delay);}
    private void stopDemo(){demonstration.removeCallbacksAndMessages(null);demonstrating=false;if(demoButton!=null)demoButton.setText("▶  再看一遍演示");}
    private void stopEffects(){if(board!=null)board.stopEffects();if(confetti!=null){confetti.stop();if(confetti.getParent() instanceof FrameLayout)((FrameLayout)confetti.getParent()).removeView(confetti);confetti=null;}}
    private boolean motionAllowed(){return motion&&foreground&&ValueAnimator.areAnimatorsEnabled();}
    private void echoMotion(View v,boolean on){if(v instanceof EchoArtView)((EchoArtView)v).setMotionEnabled(on);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)echoMotion(((ViewGroup)v).getChildAt(i),on);}
    private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String value,float size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("tnum");v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));v.setLineSpacing(dp(3),1);return v;}
    private TextView button(String label,int color,int ink){TextView v=text(label,13,ink,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(8),dp(4),dp(8),dp(4));v.setBackground(new RippleDrawable(ColorStateList.valueOf(palette.match),shape(color,14),null));v.setFocusable(true);UiMotion.press(v,()->motion);return v;}
    private GradientDrawable shape(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void add(LinearLayout parent,View child,int top){add(parent,child,top,-2);}
    private void add(LinearLayout parent,View child,int top,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));p.topMargin=dp(top);parent.addView(child,p);}
    private void addWeighted(LinearLayout parent,View child,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(height),1);p.setMargins(dp(3),0,dp(3),0);parent.addView(child,p);}
}
