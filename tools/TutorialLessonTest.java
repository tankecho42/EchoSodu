import com.tankecho.zensudoku.*;
public final class TutorialLessonTest {
    static int checks;
    static void check(boolean v,String name){checks++;if(!v)throw new AssertionError(name);}
    public static void main(String[] args){
        for(int lesson=0;lesson<6;lesson++){Game g=TutorialLesson.create(lesson);for(int[] region:HintEngine.UNITS){int mask=0;for(int i:region)mask|=1<<(g.solution[i]-1);check(mask==511,"valid solution");}check(Game.countSolutions(g.givens.clone(),2)==1,"unique teaching board");check(g.practice,"safe unlimited practice");}
        Game quick=TutorialLesson.create(2);check(quick.remaining(5)==3,"exactly three fives missing");quick.selectNumber(5);for(int i:TutorialLesson.QUICK_CELLS)check(quick.selectCell(i)==1,"each target accepts five");check(quick.activeNumber==0&&!quick.numberAvailable(5),"completed five becomes unavailable");check(quick.status.equals("playing"),"digit demo does not end game");
        Game notes=TutorialLesson.create(3);check(Integer.bitCount(Game.mask(notes.board,2))>1,"candidate example offers multiple possibilities");check((Game.mask(notes.board,2)&8)!=0,"four remains possible");
        Game hint=TutorialLesson.create(4);HintEngine.Plan plan=HintEngine.find(hint.board,2);check(plan.cell==2&&plan.digit==4,"preferred hint matches lesson");
        Game win=TutorialLesson.create(5);win.selectCell(2);check(win.enter(4)==4,"last cell triggers actual win");
        EffectTimeline timeline=new EffectTimeline();timeline.add(EffectTimeline.ROW,0,0,new int[]{0,1,2},1000);timeline.add(EffectTimeline.WRONG,2,3,new int[]{2},1000);timeline.clearWrong(2);check(timeline.active(1001).size()==1&&timeline.active(1001).get(0).kind==EffectTimeline.ROW,"correcting mistake preserves concurrent completion");
        System.out.println("PASS "+checks+" tutorial fixture and correction checks");
    }
}
